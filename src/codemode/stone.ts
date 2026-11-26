import { savedLink } from "../modules/music/index.ts";
import { parseStoneLink, stoneRpc } from "../modules/music/stone.ts";
import { type HubContext, type ToolResult, ToolError } from "../modules/types.ts";

type Fn = (...args: unknown[]) => Promise<unknown>;
export type RunStoneCode = (methods: Record<string, Fn>) => Promise<unknown>;
const KEYS = new Set(["titulo", "idPista", "volumen", "ecualizador", "segundoPlano"]);
const code = (value: unknown) => typeof value === "string" && /^[A-Z][A-Z0-9_]{1,63}$/.test(value) ? value : "STONE_ERROR";

async function readStoneOrder(request: Request) {
  const reader = request.body?.getReader();
  if (!reader) return null;
  const decoder = new TextDecoder();
  let length = 0, body = "";
  try {
    for (;;) {
      const chunk = await reader.read();
      if (chunk.done) return JSON.parse(body + decoder.decode());
      length += chunk.value.byteLength;
      if (length > 2048) { await reader.cancel(); return null; }
      body += decoder.decode(chunk.value, { stream: true });
    }
  } catch { return null; }
  finally { reader.releaseLock(); }
}

export async function codeModeStone(request: Request, ctx: HubContext, run: RunStoneCode) {
  const args = await readStoneOrder(request);
  if (!args || typeof args !== "object" || Array.isArray(args) || Object.keys(args).some((key) => !KEYS.has(key)) ||
      typeof args.titulo !== "string" || !args.titulo.trim() || args.titulo.length > 200 ||
      (args.idPista !== undefined && (typeof args.idPista !== "string" || !args.idPista || args.idPista.length > 128)) ||
      (args.volumen !== undefined && (!Number.isInteger(args.volumen) || args.volumen < 0 || args.volumen > 100)) ||
      ["ecualizador", "segundoPlano"].some((key) => args[key] !== undefined && typeof args[key] !== "boolean")) {
    throw new ToolError("INVALID_PARAM", "Indica un título y ajustes válidos de Stone.");
  }
  const saved = await savedLink(ctx);
  if (!saved) throw new ToolError("NOT_LINKED", "Vincula Stone con su enlace desde el Hub de prueba.");
  const url = new URL(saved.endpoint);
  url.searchParams.set("token", saved.token);
  const link = parseStoneLink(url.href, ctx.env.STONE_RELAY_HOST);
  const relay: HubContext = { ...ctx, fetch: (input, init) => ctx.fetch(input, { ...init, redirect: "manual" }) };
  async function call(name: string, values = {}, timeout = 4_000) {
    try {
      const response = await stoneRpc(relay, link, "tools/call", { name, arguments: values }, timeout) as ToolResult | undefined;
      const result = response?.structuredContent as Record<string, unknown> | undefined;
      if (response?.isError) return { error: code((result?.error as { code?: string } | undefined)?.code) };
      return result ?? { error: "STONE_BAD_RESPONSE" };
    } catch (error) {
      return { error: code(error instanceof ToolError ? error.code : null) };
    }
  }
  // The isolate sees the request and these five capabilities. It cannot fetch or read either token.
  const titulo = args.titulo.trim();
  return run({
    pedido: async () => ({ ...args, titulo, segundoPlano: args.segundoPlano ?? false }),
    buscar: () => call("library_search", { query: titulo, kind: "audio", limit: 20 }),
    volumen: () => call("player_set_volume", { percent: args.volumen }),
    ecualizador: () => call("equalizer_set_enabled", { enabled: args.ecualizador }),
    reproducir: (songId) => call("player_play_song", { songId }, 5_000),
    estado: () => call("player_state", {}, 2_000),
  });
}
