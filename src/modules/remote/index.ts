// Any app on the phone with a Modo MCP like Stone's (Luma first): the hub reaches it through the same relay with the
// link and token the app gave, and lists its tools under the module's prefix. The app keeps checking its own token.
import { type HubContext, type HubModule, type Tool, ToolError, type ToolResult, text } from "../types.ts";
import { type StoneLink, parseStoneLink, stoneRpc } from "../music/stone.ts";
import { describe, managedBy, tokenInfo } from "../music/index.ts";

const LIST_TTL_MS = 10 * 60_000;

export interface RemoteApp { prefix: string; title: string; summary: string; androidPackage: string; label: string }

export async function remoteLink(ctx: HubContext, prefix: string): Promise<StoneLink | null> {
  const raw = await ctx.store.getConfig(`${prefix}link`);
  return raw ? JSON.parse(raw) as StoneLink : null;
}

/** Checks and saves an app's link (a token the app rejects is never saved). */
export async function linkRemote(ctx: HubContext, app: RemoteApp, raw: string): Promise<string> {
  const link = parseStoneLink(raw, ctx.env.STONE_RELAY_HOST);
  let note = `${app.label} vinculada y en línea.`;
  try {
    const token = tokenInfo(await stoneRpc(ctx, link, "initialize", { protocolVersion: "2025-06-18" }, 8_000));
    if (token) note = `${app.label} vinculada. ` + describe(token);
  } catch (error) {
    if ((error as ToolError).code === "STONE_TOKEN") throw error;
    note = `${app.label} vinculada. Ahora no responde: abre ${app.label} con el Modo MCP activado.`;
  }
  await ctx.store.setConfig(`${app.prefix}link`, JSON.stringify(link)); await ctx.store.setConfig(`${app.prefix}tools`, null);
  return note;
}

export async function unlinkRemote(ctx: HubContext, prefix: string) {
  await ctx.store.setConfig(`${prefix}link`, null); await ctx.store.setConfig(`${prefix}tools`, null);
}

/** The app's tools under [prefix]; names that already carry it (gallery_search) are kept as they are. */
async function remoteTools(ctx: HubContext, app: RemoteApp, link: StoneLink): Promise<(Tool & { remote: string })[]> {
  const cached = await ctx.store.getConfig(`${app.prefix}tools`);
  const saved = cached ? JSON.parse(cached) as { at: number; tools: (Tool & { remote: string })[] } : null;
  if (saved && Date.now() - saved.at < LIST_TTL_MS) return saved.tools;
  try {
    const result = await stoneRpc(ctx, link, "tools/list", {}, 6_000) as { tools?: Tool[] };
    const tools = (result.tools ?? []).filter((t) => /^[a-z][a-z0-9_]{0,60}$/.test(t.name)).map((t) => ({
      ...t, remote: t.name, name: t.name.startsWith(app.prefix) ? t.name : app.prefix + t.name, description: `[${app.label}] ${t.description}`,
    }));
    await ctx.store.setConfig(`${app.prefix}tools`, JSON.stringify({ at: Date.now(), tools }));
    return tools;
  } catch (error) {
    if (saved) return saved.tools;
    throw error;
  }
}

export function remoteModule(app: RemoteApp): HubModule {
  const status: Tool = { name: `${app.prefix}status`, description: `Whether ${app.label} is linked to the hub and reachable.`,
    inputSchema: { type: "object", properties: {} }, annotations: { title: `Estado de ${app.label}`, readOnlyHint: true } };
  return {
    prefix: app.prefix, title: app.title, summary: app.summary, androidPackage: app.androidPackage, validatesOwnArgs: true,
    async status(ctx) {
      const link = await remoteLink(ctx, app.prefix);
      if (!link) return { linked: false, online: false, tools: 0, detail: `Sin vincular: en ${app.label}, Ajustes › Modo MCP.` };
      try {
        const token = tokenInfo(await stoneRpc(ctx, link, "initialize", { protocolVersion: "2025-06-18" }, 6_000));
        const tools = (await stoneRpc(ctx, link, "tools/list", {}, 6_000) as { tools?: unknown[] }).tools?.length ?? 0;
        return { linked: true, online: true, tools, detail: describe(token), ...(token ? { token, managedBy: managedBy(token) } : {}) };
      } catch (error) {
        return { linked: true, online: false, tools: 0, detail: (error as Error).message };
      }
    },
    async tools(ctx) {
      const link = await remoteLink(ctx, app.prefix);
      if (!link) return [status];
      try { return [status, ...(await remoteTools(ctx, app, link)).map(({ remote: _remote, ...tool }) => tool)]; } catch { return [status]; }
    },
    async call(name, args, ctx): Promise<ToolResult> {
      const link = await remoteLink(ctx, app.prefix);
      if (name === status.name) return text(link ? `${app.label} is linked.` : `${app.label} is not linked.`, { linked: !!link });
      if (!link) throw new ToolError("NOT_LINKED", `${app.label} is not linked to the hub yet (owner: open /admin).`);
      const tool = (await remoteTools(ctx, app, link)).find((t) => t.name === name);
      if (!tool) throw new ToolError("UNKNOWN_TOOL", `${app.label} has no tool ${name}.`);
      return await stoneRpc(ctx, link, "tools/call", { name: tool.remote, arguments: args }, 75_000) as ToolResult;
    },
  };
}

export const LUMA: RemoteApp = { prefix: "gallery_", title: "Galería (Luma)", label: "Luma", androidPackage: "app.luma.gallery",
  summary: "Luma on the phone: search the user's photos by what they show or the text in them (never the images), albums, favorites." };
export const galleryModule = remoteModule(LUMA);
export const CALCULADORA: RemoteApp = { prefix: "calc_", title: "Calculadora", label: "Calculadora", androidPackage: "com.zirakusa.calculadora",
  summary: "The user's calculator on the phone: exact arithmetic and scientific expressions, history, currency and unit conversion." };
export const calculatorAppModule = remoteModule(CALCULADORA);
export const REMOTE_APPS = [LUMA, CALCULADORA];
