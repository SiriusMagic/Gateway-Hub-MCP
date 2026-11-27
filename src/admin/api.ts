// /api/admin/*: the same owner actions as JSON, for the Gateway Hub Android app. The owner token
// travels only in the Authorization header (never in the URL), and these answers carry no CORS
// headers, so no web page can call them from a browser.
import { type HubContext, type HubModule, ToolError } from "../modules/types.ts";
import { mcpReport, TRACE_ID } from "./mcp-diagnostics.ts";
import { savedLink } from "../modules/music/index.ts";
import { stoneRpc } from "../modules/music/stone.ts";
import { REMOTE_APPS, linkRemote, remoteLink, unlinkRemote } from "../modules/remote/index.ts";
import { type AdminStore, createAgent, linkStone, overview, scan, setPaused, unlinkStone } from "./service.ts";

export interface ApiDeps { store: AdminStore; ctx: HubContext; modules: HubModule[]; security: Record<string, string>; version: string }

const send = (status: number, body: unknown, security: Record<string, string>) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", ...security } });

export async function handleAdminApi(request: Request, url: URL, deps: ApiDeps): Promise<Response> {
  const { store, ctx, modules, security } = deps;
  const ok = (body: unknown) => send(200, body, security);
  const error = (status: number, code: string, message: string) => send(status, { error: { code, message } }, security);
  const path = url.pathname.slice("/api/admin".length) || "/";
  const method = request.method;
  let body: Record<string, unknown> = {};
  if (method === "POST") {
    try { body = await request.json() as Record<string, unknown>; } catch { return error(400, "INVALID_JSON", "El cuerpo debe ser JSON."); }
    if (typeof body !== "object" || body === null || Array.isArray(body)) return error(400, "INVALID_JSON", "El cuerpo debe ser un objeto JSON.");
  }
  try {
    if (path === "/mcp-diagnostics" && method === "GET") return ok({ failures: await store.mcpFailures() });
    if (path === "/mcp-diagnostics" && method === "POST") {
      const report = mcpReport(body);
      if (!report) return error(400, "INVALID_REPORT", "El informe contiene campos inválidos.");
      await store.mcpReport(report);
      return ok({ id: report.id });
    }
    if (path === "/mcp-diagnostics/stone" && method === "POST") {
      const id = body.id;
      if (typeof id !== "string" || !TRACE_ID.test(id)) return error(400, "INVALID_TRACE", "ID de seguimiento inválido.");
      const link = await savedLink(ctx);
      if (!link) return error(409, "STONE_NOT_LINKED", "Stone aún no está vinculado.");
      const result = await stoneRpc({ ...ctx, traceId: id }, link, "tools/call", { name: "player_state", arguments: {} }, 5_000);
      const reply = result as { isError?: boolean; structuredContent?: { error?: { code?: string } } } | undefined;
      if (!reply || reply.isError) return error(502, reply?.structuredContent?.error?.code ?? "STONE_ERROR", "Stone rechazó la prueba.");
      return ok({ id, ok: true });
    }
    const check = /^\/mcp-diagnostics\/check\/([a-z][a-z0-9]{0,15}_)$/.exec(path);
    if (check && method === "POST") {
      const id = body.id;
      if (typeof id !== "string" || !TRACE_ID.test(id)) return error(400, "INVALID_TRACE", "ID de seguimiento inválido.");
      const app = REMOTE_APPS.find(({ prefix }) => prefix === check[1]);
      if (!app) return error(404, "NOT_FOUND", "Esa app no está en este Hub.");
      const link = await remoteLink(ctx, app.prefix);
      if (!link) return error(409, "APP_NOT_LINKED", "Vincula la app en el escáner.");
      await stoneRpc({ ...ctx, traceId: id }, link, "ping", {}, 5_000);
      return ok({ id, ok: true });
    }
    if (path === "/state" && method === "GET") return ok(await overview(store, ctx, modules, deps.version));
    if (path === "/scan" && method === "GET") return ok(await scan(ctx, modules));
    if (path === "/stone" && method === "POST") return ok({ message: await linkStone(store, ctx, String(body.link ?? "")) });
    if (path === "/stone" && method === "DELETE") { await unlinkStone(store); return ok({ message: "Stone desvinculado." }); }
    // Other apps with Modo MCP (Luma): /apps/gallery_ — the same as /stone for Stone.
    const app = /^\/apps\/([a-z][a-z0-9]{0,15}_)$/.exec(path);
    const remote = app ? REMOTE_APPS.find((a) => a.prefix === app[1]) : undefined;
    if (app && !remote) return error(404, "NOT_FOUND", "Esa app no está en este hub.");
    if (remote && method === "POST") return ok({ message: await linkRemote(ctx, remote, String(body.link ?? "")) });
    if (remote && method === "DELETE") { await unlinkRemote(ctx, remote.prefix); return ok({ message: `${remote.label} desvinculada.` }); }
    if (path === "/agents" && method === "POST") return ok(await createAgent(store, modules, url.origin,
      { name: String(body.name ?? ""), apps: body.apps as string[], readOnly: body.readOnly === true, days: Number(body.days) }));
    const agent = /^\/agents\/([0-9a-f]{12})(\/pause|\/resume)?$/.exec(path);
    if (agent && method === "POST" && agent[2]) {
      return (await setPaused(store, agent[1], agent[2] === "/pause")) ? ok({ id: agent[1] }) : error(404, "NOT_FOUND", "Ese agente no existe.");
    }
    if (agent && method === "DELETE" && !agent[2]) {
      return (await store.agentsDelete(agent[1])) ? ok({ id: agent[1] }) : error(404, "NOT_FOUND", "Ese agente no existe.");
    }
  } catch (failure) {
    if (failure instanceof ToolError) return error(400, failure.code, failure.message);
    console.error(JSON.stringify({ event: "admin_api_failed", path, error: String(failure) }));
    return error(500, "INTERNAL", "Algo falló en el hub.");
  }
  return error(404, "NOT_FOUND", "Ruta desconocida de la API de administración.");
}
