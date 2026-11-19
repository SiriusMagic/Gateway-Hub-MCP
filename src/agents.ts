// Who is calling: the owner (HUB_TOKEN, full access) or an agent with its own link. Agent tokens
// are random 256-bit values shown once; only their SHA-256 is stored.
import type { Agent, HubAdminStore, Tool } from "./modules/types.ts";

export interface Principal { id: string; name: string; modules: string[]; readOnly: boolean }
export const OWNER: Principal = { id: "owner", name: "Propietario", modules: ["*"], readOnly: false };
export const MAX_AGENTS = 50;

export async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** A random 256-bit secret; in a shared hub it also names its space ("gw_s<space>_…", see spaces.ts). */
export function newSecret(prefix: "hub" | "gw" | "dev", space: string | null = null): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  const random = btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `${prefix}_${space ? `s${space}_` : ""}${random}`;
}

export function newId(): string {
  return [...crypto.getRandomValues(new Uint8Array(6))].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** The principal behind [token], or null when it is unknown, paused or expired. */
export async function principalFor(token: string, isOwner: (token: string) => Promise<boolean>, store: HubAdminStore, now = Date.now()): Promise<Principal | null> {
  if (await isOwner(token)) return OWNER;
  if (!token.startsWith("gw_")) return null;
  const agent = await store.agentByHash(await sha256Hex(token));
  if (!agent || agent.paused || (agent.expiresAt && agent.expiresAt <= now)) return null;
  if (now - agent.lastUsed > 60_000) await store.agentsSet(agent.id, { lastUsed: now });
  return { id: agent.id, name: agent.name, modules: agent.modules, readOnly: agent.readOnly };
}

export const allowsModule = (p: Principal, tool: string) => p.modules.includes("*") || p.modules.some((m) => tool.startsWith(m));

/** Whether [p] may see and call [tool]: its app is allowed and, for read-only agents, the tool only reads. */
export const allows = (p: Principal, tool: Tool) => allowsModule(p, tool.name) && (!p.readOnly || tool.annotations?.readOnlyHint === true);

export function agentStatus(agent: Agent, now = Date.now()): "active" | "paused" | "expired" {
  return agent.paused ? "paused" : agent.expiresAt && agent.expiresAt <= now ? "expired" : "active";
}
