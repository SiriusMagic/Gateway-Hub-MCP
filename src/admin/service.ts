// What the owner can do, shared by the HTML panel (actions.ts) and the JSON API (api.ts) used by
// the Android app. Validation lives here, once.
import { type Agent, type HubAdminStore, type HubContext, type HubModule, type HubStoreApi, ToolError } from "../modules/types.ts";
import { MAX_AGENTS, agentStatus, newAgentToken, newId, sha256Hex } from "../agents.ts";
import { parseStoneLink, stoneRpc } from "../modules/music/stone.ts";
import { describe, savedLink, tokenInfo } from "../modules/music/index.ts";

export type AdminStore = HubStoreApi & HubAdminStore;
export const DAY = 86_400_000;

/** An agent as the owner sees it: never its token hash. */
export const publicAgent = (a: Agent) => ({ id: a.id, name: a.name, modules: a.modules, readOnly: a.readOnly,
  paused: a.paused, status: agentStatus(a), expiresAt: a.expiresAt, created: a.created, lastUsed: a.lastUsed });

export async function overview(store: AdminStore, ctx: HubContext, modules: HubModule[], version: string) {
  const now = Date.now();
  return {
    version,
    modules: modules.map((m) => ({ prefix: m.prefix, title: m.title, summary: m.summary, androidPackage: m.androidPackage ?? null })),
    stoneLinked: !!await savedLink(ctx),
    agents: (await store.agentsList()).map(publicAgent),
    metrics: { day: await store.metricsSummary(now - DAY), week: await store.metricsSummary(now - 7 * DAY) },
  };
}

/**
 * Read-only scan of every app: built-in ones are always ready; remote ones (Stone) get one harmless
 * requests (initialize and tools/list) in parallel. Nothing is turned on or changed.
 */
export async function scan(ctx: HubContext, modules: HubModule[]) {
  const results = await Promise.all(modules.map(async (m) => {
    const status = m.status ? await m.status(ctx).catch((e) => ({ linked: false, online: false, tools: 0, detail: String(e) }))
      : { linked: true, online: true, tools: (await m.tools(ctx)).length, detail: "Integrada en el hub." };
    return { prefix: m.prefix, title: m.title, androidPackage: m.androidPackage ?? null, remote: !!m.status, ...status };
  }));
  return { scannedAt: Date.now(), apps: results };
}

/** Saves Stone's link after checking it; a token Stone rejects is never saved. Returns a note for the owner. */
export async function linkStone(store: AdminStore, ctx: HubContext, raw: string): Promise<string> {
  const link = parseStoneLink(raw, ctx.env.STONE_RELAY_HOST);
  let note = "Stone vinculado y en línea.";
  try {
    const token = tokenInfo(await stoneRpc(ctx, link, "initialize", { protocolVersion: "2025-06-18" }, 8_000));
    if (token) note = "Stone vinculado. " + describe(token);
  } catch (error) {
    if ((error as ToolError).code === "STONE_TOKEN") throw error;
    note = "Stone vinculado. Ahora no responde: abre Stone con el Modo MCP activado.";
  }
  await store.setConfig("music.link", JSON.stringify(link)); await store.setConfig("music.tools", null);
  return note;
}

export async function unlinkStone(store: AdminStore) {
  await store.setConfig("music.link", null); await store.setConfig("music.tools", null);
}

export interface NewAgent { name: string; apps: string[]; readOnly: boolean; days: number }

/** Creates an agent; its secret link is returned once and only its SHA-256 is stored. */
export async function createAgent(store: AdminStore, modules: HubModule[], origin: string, input: NewAgent) {
  const name = String(input.name ?? "").trim().slice(0, 40);
  if (!name) throw new ToolError("INVALID_ARGUMENT", "Ponle un nombre al agente.");
  if ((await store.agentsList()).length >= MAX_AGENTS) throw new ToolError("LIMIT", `Máximo ${MAX_AGENTS} agentes.`);
  const known = new Set(modules.map((m) => m.prefix));
  const apps = [...new Set((Array.isArray(input.apps) ? input.apps : []).map(String))].filter((p) => known.has(p));
  if (!apps.length) throw new ToolError("INVALID_ARGUMENT", "Elige al menos una app.");
  const days = Math.min(365, Math.max(0, Math.floor(Number(input.days) || 0)));
  const secret = newAgentToken(), now = Date.now();
  const agent: Agent = { id: newId(), name, tokenHash: await sha256Hex(secret), modules: apps.length === known.size ? ["*"] : apps,
    readOnly: input.readOnly === true, paused: false, expiresAt: days ? now + days * DAY : 0, created: now, lastUsed: 0 };
  await store.agentsCreate(agent);
  return { agent: publicAgent(agent), token: secret, url: `${origin}/mcp?token=${secret}` };
}

export async function setPaused(store: AdminStore, id: string, paused: boolean): Promise<boolean> {
  if (!(await store.agentsList()).some((a) => a.id === id)) return false;
  await store.agentsSet(id, { paused });
  return true;
}
