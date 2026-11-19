// One hub for many people: each person is a "space" with its own Durable Object (u_<space>), its own
// agents, phones and metrics. Every secret of a space names it ("gw_s<space>_…"), so the Worker knows where
// a request goes without any shared table; the space's own object then checks the secret's hash.
import type { Agent, HubAdminStore, HubStoreApi } from "./modules/types.ts";
import { MAX_AGENTS, newId, newSecret, sha256Hex } from "./agents.ts";
import { createDevice } from "./channel/devices.ts";

const SPACE_IN_TOKEN = /^(?:hub|gw|dev)_s([0-9a-f]{16})_[A-Za-z0-9_-]{20,}$/;
export const SPACE_KEY = "space.id";
const OWNER_KEY = "space.owner";

/** The space a secret belongs to, or null (a single-owner hub's token, or not a secret at all). */
export const spaceOf = (token: string | null): string | null => token ? SPACE_IN_TOKEN.exec(token)?.[1] ?? null : null;

export const newSpaceId = () => [...crypto.getRandomValues(new Uint8Array(8))].map((b) => b.toString(16).padStart(2, "0")).join("");

/** This object's space, or null for a single-owner hub (the "main" object, owner = HUB_TOKEN). */
export const spaceId = (store: HubStoreApi) => store.getConfig(SPACE_KEY);

/** First run of a space: its owner secret (for the Gateway app) and its phone's channel credential, shown once. */
export async function setupSpace(store: HubStoreApi, space: string, phone: string) {
  if (!/^[0-9a-f]{16}$/.test(space)) throw new Error("bad space id");
  if (await store.getConfig(SPACE_KEY)) throw new Error("space already set up");
  await store.setConfig(SPACE_KEY, space);
  const owner = newSecret("hub", space);
  await store.setConfig(OWNER_KEY, await sha256Hex(owner));
  const device = await createDevice(store, phone);
  return { owner, device: device.device, secret: device.secret };
}

/** Whether [token] is this space's owner secret (constant-time through the hashes). */
export async function isSpaceOwner(store: HubStoreApi, token: string): Promise<boolean> {
  const saved = await store.getConfig(OWNER_KEY);
  if (!saved || spaceOf(token) !== await store.getConfig(SPACE_KEY)) return false;
  const given = await sha256Hex(token);
  let diff = 0;
  for (let i = 0; i < saved.length; i++) diff |= saved.charCodeAt(i) ^ given.charCodeAt(i);
  return diff === 0 && saved.length === given.length;
}

/**
 * An AI connected through the public link (OAuth): every app, reading and writing, no expiry, named after
 * the AI. When the space is full, the link unused for longest makes room.
 */
export async function issueAgent(store: HubStoreApi & HubAdminStore, rawName: string): Promise<string> {
  const space = await store.getConfig(SPACE_KEY);
  if (!space) throw new Error("space not set up");
  const agents = await store.agentsList();
  if (agents.length >= MAX_AGENTS) {
    const oldest = [...agents].sort((a, b) => Math.max(a.lastUsed, a.created) - Math.max(b.lastUsed, b.created))[0];
    await store.agentsDelete(oldest.id);
  }
  const secret = newSecret("gw", space), now = Date.now();
  const agent: Agent = { id: newId(), name: String(rawName ?? "").trim().slice(0, 40) || "IA", tokenHash: await sha256Hex(secret),
    modules: ["*"], readOnly: false, paused: false, expiresAt: 0, created: now, lastUsed: 0 };
  await store.agentsCreate(agent);
  return secret;
}
