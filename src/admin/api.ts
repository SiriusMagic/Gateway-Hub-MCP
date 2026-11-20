// /api/admin/*: the same owner actions as JSON, for the Gateway Hub Android app. The owner token
// travels only in the Authorization header (never in the URL), and these answers carry no CORS
// headers, so no web page can call them from a browser.
import { type HubContext, type HubModule, ToolError } from "../modules/types.ts";
import { REMOTE_APPS, linkRemote, unlinkRemote } from "../modules/remote/index.ts";
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
