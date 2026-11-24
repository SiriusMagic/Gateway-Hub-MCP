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
export const FILEM: RemoteApp = { prefix: "files_", title: "Archivos (Filem)", label: "Filem", androidPackage: "com.zirakusa.filem",
  summary: "Filem on the phone: list, search and read the user's files and folders; with write access also create folders, rename, copy, move and send to its trash." };
export const filesAppModule = remoteModule(FILEM);
// The rest of the family (each with the same Modo MCP and relay). Their tools keep their own names; the prefix is added
// only when a name does not carry it (Notas' tasks_create → notas_tasks_create). Notas is notas_ because notes_ is the
// hub's own notes; Contactos lives inside Teléfono (its tools come as phone_contacts_…).
const app = (prefix: string, title: string, label: string, androidPackage: string, summary: string): RemoteApp =>
  ({ prefix, title, label, androidPackage, summary });
export const FAMILY: RemoteApp[] = [
  app("video_", "Stone Video", "Stone Video", "com.zirakusa.stone.video", "Stone Video on the phone: the user's videos and music videos, playback and library."),
  app("phone_", "Teléfono y Contactos", "Teléfono", "com.zirakusa.telefono", "The user's calls (recent, missed) and contacts (search, birthdays, favourites; create with write access). The AI never dials and never deletes a contact; it can only ask the user to call."),
  app("sms_", "SMS", "SMS", "com.zirakusa.sms", "The user's SMS conversations (verification codes always hidden). The AI never sends: it can only ask the user to send."),
  app("clock_", "Reloj", "Reloj", "com.zirakusa.reloj", "The user's alarms (list, next, create, turn on/off, delete) and world time."),
  app("calendar_", "Calendario", "Calendario", "com.zirakusa.calendario", "The user's calendar: events of a day, create and delete events, holidays of Ecuador."),
  app("notas_", "Notas", "Notas", "com.zirakusa.notas", "The Notas app on the phone: notes, folders and tasks (locked notes and the trash never come out)."),
  app("weather_", "Clima", "Clima", "com.zirakusa.clima", "The weather of the user's places (Open-Meteo): now, forecast, add and remove places."),
  app("camera_", "Cámara", "Cámara", "com.zirakusa.camara", "The camera's settings and the names and times of photos taken; the AI never takes photos."),
  app("qr_", "QR", "QR", "com.zirakusa.qr", "QR codes: read text into a code, make codes, history (Wi-Fi passwords never come out)."),
  app("network_", "Redes", "Redes", "com.zirakusa.redes", "The phone's network: status, nearby Wi-Fi (without BSSID), speed tests and their history."),
  app("compass_", "Brújula", "Brújula", "com.zirakusa.brujula", "Heading of the phone and, if allowed, its approximate location. Read only."),
  app("level_", "Nivel", "Nivel", "com.zirakusa.nivel", "The bubble level: reading, calibration and saved measurements. Read only."),
  app("rec_", "Grabadora", "Grabadora", "com.zirakusa.grabadora", "The user's voice recordings: names, dates, lengths and flags; rename with write access. Never the audio; the AI never records and never deletes."),
  app("launcher_", "Inicio", "Inicio", "com.zirakusa.launcher", "The home screen: apps list, dock order, «Hoy» cards. It never opens or installs apps."),
];
export const familyModules = FAMILY.map(remoteModule);
export const REMOTE_APPS = [LUMA, CALCULADORA, FILEM, ...FAMILY];
