// /api/admin/*: the same owner actions as JSON, for the Gateway Hub Android app. The owner token
// travels only in the Authorization header (never in the URL), and these answers carry no CORS
// headers, so no web page can call them from a browser.
import { type HubContext, type HubModule, ToolError } from "../modules/types.ts";
import { environmentState } from "../environment.ts";
import { createDevice, deleteDevice } from "../channel/devices.ts";
import { createPairing } from "../channel/pairing.ts";
import { devicesView } from "./service.ts";
import { type AdminStore, DAY, changeEnvironment, createAgent, overview, scan, setPaused } from "./service.ts";

export interface ApiDeps {
  store: AdminStore; ctx: HubContext; modules: HubModule[]; security: Record<string, string>; version: string;
  /** Closes a phone's channel at once (the Durable Object); absent in tests. */
  dropDevice?(id: string): void;
  /** A space of the shared hub: the app shows the public link and its sign-in code. */
  shared?: boolean;
  connectCode?(): Promise<{ code: string; expiresAt: number }>;
}

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
    if (path === "/state" && method === "GET") return ok({ ...await overview(store, ctx, modules, deps.version),
      shared: deps.shared === true, publicUrl: `${url.origin}/mcp` });
    if (path === "/connect-code" && method === "POST") {
      if (!deps.connectCode) return error(404, "NOT_FOUND", "Este hub no tiene enlace público.");
      return ok(await deps.connectCode());
    }
    if (path === "/activity" && method === "GET") {
      // For the app's activity notifications: what ran since [since] (at most 7 days back).
      const since = Math.max(Date.now() - 7 * DAY, Math.floor(Number(url.searchParams.get("since")) || 0));
      return ok({ environment: await environmentState(store), ...await store.metricsSummary(since) });
    }
    if (path === "/scan" && method === "GET") return ok(await scan(ctx, modules));
    if (path === "/environment" && method === "POST") return ok(await changeEnvironment(store, String(body.state ?? "")));
    if (path === "/pairing" && method === "POST") return ok(await createPairing(store));
    if (path === "/devices" && method === "GET") return ok({ devices: await devicesView(store, ctx) });
    if (path === "/devices" && method === "POST") return ok(await createDevice(store, String(body.name ?? "")));
    const device = /^\/devices\/([0-9a-f]{12})$/.exec(path);
    if (device && method === "DELETE") {
      if (!await deleteDevice(store, device[1])) return error(404, "NOT_FOUND", "Ese teléfono no existe.");
      deps.dropDevice?.(device[1]);
      return ok({ id: device[1] });
    }
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
