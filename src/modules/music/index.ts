// Stone (music and video player on the phone) as a hub module: every Stone tool appears as
// music_<tool>. Stone keeps checking its own token, permissions and on-screen confirmations.
import { type HubContext, type HubModule, type LinkedToken, type Tool, ToolError, type ToolResult, text } from "../types.ts";
import { type StoneLink, stoneRpc } from "./stone.ts";

export const MUSIC = "music_";
const LIST_TTL_MS = 10 * 60_000;

export async function savedLink(ctx: HubContext): Promise<StoneLink | null> {
  const raw = await ctx.store.getConfig("music.link");
  return raw ? JSON.parse(raw) as StoneLink : null;
}

/** Stone's tools, cached so tools/list stays fast and still works while the phone is offline. */
async function stoneTools(ctx: HubContext, link: StoneLink): Promise<Tool[]> {
  const cached = await ctx.store.getConfig("music.tools");
  const saved = cached ? JSON.parse(cached) as { at: number; tools: Tool[] } : null;
  if (saved && Date.now() - saved.at < LIST_TTL_MS) return saved.tools;
  try {
    const result = await stoneRpc(ctx, link, "tools/list", {}, 6_000) as { tools?: Tool[] };
    const tools = (result.tools ?? []).map((t) => ({ ...t, name: MUSIC + t.name, description: `[Stone] ${t.description}` }));
    await ctx.store.setConfig("music.tools", JSON.stringify({ at: Date.now(), tools }));
    return tools;
  } catch (error) {
    if (saved) return saved.tools;
    throw error;
  }
}

/** Stone writes its own key; Luma and later apps the shared one. */
const TOKEN_META = ["com.zirakusa.stone/token", "com.zirakusa/token"];
const clip = (value: unknown, max: number) => typeof value === "string" ? value.slice(0, max) : "";

/** The token details Stone gives in initialize (name, type, use, mode); unknown or missing fields stay empty. */
export function tokenInfo(result: unknown): LinkedToken | undefined {
  const all = (result as { _meta?: Record<string, unknown> } | null)?._meta;
  const meta = TOKEN_META.map((key) => all?.[key]).find((m) => m && typeof m === "object") as Record<string, unknown> | undefined;
  if (!meta || typeof meta !== "object") return undefined;
  return { name: clip(meta.name, 60), kind: clip(meta.kind, 20), usage: clip(meta.usage, 300), mode: clip(meta.mode, 20) };
}

export const managedBy = (token: LinkedToken): "app" | "hub" => token.kind === "redirect" ? "hub" : "app";

const MODES: Record<string, string> = { read: "solo lectura", write: "lectura y escritura", full: "acceso completo" };
export { MODES };

/** One line for the panel and the app: who manages Stone, with which token and what for. */
export function describe(token: LinkedToken | undefined): string {
  if (!token) return "Vinculado y en línea.";
  if (token.kind === "redirect") return "En línea · Administrado por el hub (Modo MCP de Arka): los permisos los pones aquí.";
  const use = token.usage ? ` · Uso: ${token.usage}` : "";
  return `En línea · Puente: los permisos los pone la app (token «${token.name}», ${MODES[token.mode] ?? token.mode})${use}`;
}

const statusTool: Tool = { name: "music_status", description: "Whether Stone is linked to the hub and reachable, and how many Stone tools there are.",
  inputSchema: { type: "object", properties: {} }, annotations: { title: "Estado de Stone", readOnlyHint: true } };

export const musicModule: HubModule = {
  prefix: MUSIC,
  title: "Música (Stone)",
  summary: "Stone on the phone: play, search the library, queue, lyrics, tags, playlists, equalizer, video.",
  validatesOwnArgs: true, // Stone validates every argument itself
  androidPackage: "com.zirakusa.stone.kotlin.preview",
  async status(ctx) {
    const link = await savedLink(ctx);
    if (!link) return { linked: false, online: false, tools: 0, detail: "Sin vincular: en Arka › Escáner › «Conectar todas las apps»." };
    try {
      const token = tokenInfo(await stoneRpc(ctx, link, "initialize", { protocolVersion: "2025-06-18" }, 6_000));
      const tools = (await stoneRpc(ctx, link, "tools/list", {}, 6_000) as { tools?: unknown[] }).tools?.length ?? 0;
      return { linked: true, online: true, tools, detail: describe(token), ...(token ? { token, managedBy: managedBy(token) } : {}) };
    } catch (error) {
      return { linked: true, online: false, tools: 0, detail: (error as Error).message };
    }
  },
  async tools(ctx) {
    const link = await savedLink(ctx);
    if (!link) return [statusTool];
    try { return [statusTool, ...await stoneTools(ctx, link)]; } catch { return [statusTool]; }
  },
  async call(name, args, ctx): Promise<ToolResult> {
    const link = await savedLink(ctx);
    if (name === "music_status") {
      if (!link) return text("Stone is not linked. The owner opens <hub>/admin and pastes the link copied from Stone.", { linked: false });
      try {
        const tools = (await stoneRpc(ctx, link, "tools/list", {}, 6_000) as { tools?: unknown[] }).tools?.length ?? 0;
        return text(`Stone is linked and online (${tools} tools).`, { linked: true, online: true, tools });
      } catch (error) {
        return text(`Stone is linked but not reachable: ${(error as Error).message}`, { linked: true, online: false });
      }
    }
    if (!link) throw new ToolError("NOT_LINKED", "Stone is not linked to the hub yet (owner: open /admin).");
    // Up to 75 s: Stone may ask the user on the phone to approve a critical change.
    const result = await stoneRpc(ctx, link, "tools/call", { name: name.slice(MUSIC.length), arguments: args }, 75_000);
    return result as ToolResult;
  },
};
