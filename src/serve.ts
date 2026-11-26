import { feedback } from "./feedback/api.ts";
import type { FeedbackQueue } from "./feedback/queue.ts";
// Every request to the hub, after the Worker checked HTTPS: limits, size, who is calling, then
// /mcp (AI clients) or /admin (owner).
import type { HubAdminStore, HubContext, HubEnv, HubStoreApi } from "./modules/types.ts";
import { weakOwnerToken, type Limiter, givenToken, sameSecret } from "./auth.ts";
import { type Principal, principalFor } from "./agents.ts";
import { handleRpc } from "./mcp.ts";
import { createRouter } from "./router.ts";
import { modules } from "./modules/index.ts";
import { handleAdmin } from "./admin/actions.ts";
import { handleAdminApi } from "./admin/api.ts";

export const VERSION = "0.0.3";
const MAX_BODY = 1 << 20;
export const SECURITY = {
  "Strict-Transport-Security": "max-age=31536000; includeSubDomains", "X-Content-Type-Options": "nosniff",
  "Cache-Control": "no-store", "Referrer-Policy": "no-referrer", "X-Frame-Options": "DENY",
};
export const CORS = {
  "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type, Accept, Mcp-Session-Id, Mcp-Protocol-Version, X-Api-Key",
  "Access-Control-Expose-Headers": "Mcp-Session-Id",
};
const router = createRouter(modules);

export function json(status: number, body: unknown, extra: Record<string, string> = {}): Response {
  return new Response(body === null ? null : JSON.stringify(body),
    { status, headers: { "Content-Type": "application/json", ...SECURITY, ...CORS, ...extra } });
}
const rpcError = (status: number, message: string, extra: Record<string, string> = {}) =>
  json(status, { jsonrpc: "2.0", id: null, error: { code: -32001, message } }, extra);

export interface Limits { requests: Limiter; failures: Limiter }
export type ServeStore = HubStoreApi & HubAdminStore & Limits & { feedback?: FeedbackQueue };

function rejected(store: ServeStore, caller: string, missing: boolean) {
  store.failures.hit(caller);
  console.warn(JSON.stringify({ event: "auth_rejected", caller, missing })); // never the token itself
}

export async function serve(request: Request, env: HubEnv, store: ServeStore, fetcher: typeof fetch = fetch): Promise<Response> {
  const url = new URL(request.url);
  const caller = request.headers.get("CF-Connecting-IP") || "local";
  if (store.requests.hit(caller)) {
    console.warn(JSON.stringify({ event: "rate_limited", caller }));
    return rpcError(429, "Gateway Hub: too many requests. Wait a minute.", { "Retry-After": "60" });
  }
  const owner = env.HUB_TOKEN;
  if (!owner) return rpcError(503, "Gateway Hub has no owner token yet (npx wrangler secret put HUB_TOKEN).");
  // A guessable owner token would open the hub and every phone it reaches: refuse to serve until it is replaced.
  const weak = weakOwnerToken(owner);
  if (weak) {
    console.warn(JSON.stringify({ event: "weak_owner_token", reason: weak })); // never the token itself
    return rpcError(503, `Gateway Hub: the owner token is too weak (${weak}). Make a strong one: ` +
      "openssl rand -base64 32 | npx wrangler secret put HUB_TOKEN");
  }
  if (store.failures.blocked(caller)) return rpcError(429, "Gateway Hub: too many wrong tokens. Wait a minute.", { "Retry-After": "60" });
  if (Number(request.headers.get("content-length") ?? 0) > MAX_BODY) return rpcError(413, "Gateway Hub: body larger than 1 MiB.");
  if (url.pathname.startsWith("/api/feedback/") && store.feedback) {
    return feedback(request, url.pathname.slice("/api/feedback/".length), store.feedback, owner,
      env.FEEDBACK_REVIEW_TOKEN, () => rejected(store, caller, !request.headers.has("Authorization")));
  }
  const ctx: HubContext = { store, env, fetch: (input, init) => fetcher(input, init) };

  if (url.pathname === "/mcp") {
    if (request.method === "DELETE") return json(200, null);
    if (request.method !== "POST") return rpcError(405, "Gateway Hub: send MCP requests with POST.", { Allow: "POST, DELETE" });
    const token = givenToken(request, url);
    const principal: Principal | null = token ? await principalFor(token, owner, store) : null;
    if (!principal) {
      rejected(store, caller, !token);
      return rpcError(401, "Gateway Hub: missing, wrong, paused or expired link.", { "WWW-Authenticate": "Bearer" });
    }
    const body = await request.text();
    if (body.length > MAX_BODY) return rpcError(413, "Gateway Hub: body larger than 1 MiB.");
    const reply = await handleRpc(body, router, ctx, VERSION,
      { principal, record: (tool, ok, ms) => store.metricsRecord(principal.id, tool, ok, ms) });
    const session = body.includes('"initialize"') ? { "Mcp-Session-Id": crypto.randomUUID() } : {};
    return reply === null ? json(202, null, session) : json(200, reply, session);
  }

  if (url.pathname === "/admin" || url.pathname === "/setup") {
    return handleAdmin(request, url, { store, ctx, modules, security: SECURITY, async isOwner(token) {
      // Only the owner token opens the panel: agent links never do.
      if (token && await sameSecret(token, owner)) return true;
      rejected(store, caller, !token);
      return false;
    } });
  }
  if (url.pathname.startsWith("/api/admin/")) {
    // The owner token only in the Authorization header here: an API URL never carries a secret.
    const bearer = /^bearer\s+(.+)$/i.exec(request.headers.get("authorization") ?? "")?.[1]?.trim() ?? "";
    if (!bearer || !await sameSecret(bearer, owner)) {
      rejected(store, caller, !bearer);
      return new Response(JSON.stringify({ error: { code: "UNAUTHORIZED", message: "Token del propietario incorrecto." } }),
        { status: 401, headers: { "Content-Type": "application/json", "WWW-Authenticate": "Bearer", ...SECURITY } });
    }
    return handleAdminApi(request, url, { store, ctx, modules, security: SECURITY, version: VERSION });
  }
  return rpcError(404, "Gateway Hub: unknown path. Use POST /mcp, or open /admin.");
}
