// The shared hub's front desk (one Durable Object, "directory"): new spaces for new Gateway apps, and the
// public link's sign-in (OAuth 2.1 with PKCE and dynamic client registration, as MCP clients expect).
// The person proves which phone is theirs with the code Gateway shows; the AI app then gets an agent
// secret of that space and sends it on every call, so each call only ever reaches that person's phone.
import type { HubStoreApi } from "../modules/types.ts";
import { type Limiter } from "../auth.ts";
import { newSpaceId } from "../spaces.ts";
import { redeemConnectCode } from "./connect.ts";
import { PAGE_HEADERS, authorizePage, messagePage } from "./page.ts";
import { pkceMatches, seal, unseal } from "./seal.ts";
import { SECURITY, json } from "../serve.ts";

export interface DirectoryDeps {
  setupSpace(space: string, phone: string): Promise<{ owner: string; device: { id: string; name: string; created: number }; secret: string }>;
  issueAgent(space: string, name: string): Promise<string>;
}
export type DirectoryStore = HubStoreApi & { requests: Limiter; failures: Limiter; signups: Limiter };

interface Client { u: string[]; n: string }
interface Code { s: string; c: string; r: string; ch: string; n: string; e: number; id: string }
const CODE_MS = 5 * 60_000;
export const OAUTH_PATHS = ["/register", "/oauth/register", "/oauth/authorize", "/oauth/token",
  "/.well-known/oauth-authorization-server", "/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/mcp"];

const html = (status: number, body: string, extra: Record<string, string> = {}) =>
  new Response(body, { status, headers: { ...SECURITY, ...PAGE_HEADERS, ...extra } });
const oauthError = (status: number, error: string, description: string) => json(status, { error, error_description: description });

/** https, or http only to this device (apps that sign in through a local port). No fragment. */
const goodRedirect = (raw: unknown) => {
  if (typeof raw !== "string" || raw.length > 512) return false;
  try { const u = new URL(raw); return !u.hash && (u.protocol === "https:" || (u.protocol === "http:" && ["localhost", "127.0.0.1", "[::1]"].includes(u.hostname))); }
  catch { return false; }
};

async function form(request: Request): Promise<Record<string, string>> {
  const type = request.headers.get("content-type") ?? "";
  try {
    if (type.includes("application/json")) return Object.fromEntries(Object.entries(await request.json() as object).map(([k, v]) => [k, String(v)]));
    return Object.fromEntries(new URLSearchParams(await request.text()));
  } catch { return {}; }
}

export async function handleDirectory(request: Request, url: URL, store: DirectoryStore, deps: DirectoryDeps): Promise<Response> {
  const caller = request.headers.get("CF-Connecting-IP") || "local";
  const origin = url.origin, path = url.pathname, method = request.method;
  if (store.requests.hit(caller)) return json(429, { error: "slow_down", error_description: "Too many requests. Wait a minute." }, { "Retry-After": "60" });

  if (path.startsWith("/.well-known/oauth-protected-resource") && method === "GET")
    return json(200, { resource: `${origin}/mcp`, authorization_servers: [origin], bearer_methods_supported: ["header"] });
  if (path === "/.well-known/oauth-authorization-server" && method === "GET") return json(200, {
    issuer: origin, authorization_endpoint: `${origin}/oauth/authorize`, token_endpoint: `${origin}/oauth/token`,
    registration_endpoint: `${origin}/oauth/register`, response_types_supported: ["code"], grant_types_supported: ["authorization_code"],
    code_challenge_methods_supported: ["S256"], token_endpoint_auth_methods_supported: ["none"], scopes_supported: ["mcp"] });

  if (path === "/register" && method === "POST") {
    // A new Gateway app gets its own space. Few per address and hour, so nobody can fill the hub.
    if (store.signups.hit(caller)) return json(429, { error: { code: "SLOW_DOWN", message: "Demasiados registros desde esta red. Prueba en una hora." } }, { "Retry-After": "3600" });
    const body = await form(request);
    const made = await deps.setupSpace(newSpaceId(), String(body.name ?? "").slice(0, 40));
    return json(200, { hub: origin, ...made });
  }

  if (path === "/oauth/register" && method === "POST") {
    const body = await (async () => { try { return await request.json() as Record<string, unknown>; } catch { return {}; } })();
    const uris = Array.isArray(body.redirect_uris) ? body.redirect_uris.slice(0, 5) : [];
    if (!uris.length || !uris.every(goodRedirect)) return oauthError(400, "invalid_redirect_uri", "redirect_uris must be https (or http to localhost).");
    const name = String(body.client_name ?? "").trim().slice(0, 60) || "IA";
    const client_id = await seal(store, "cl", { u: uris, n: name } satisfies Client);
    return json(201, { client_id, client_name: name, redirect_uris: uris, token_endpoint_auth_method: "none",
      grant_types: ["authorization_code"], response_types: ["code"], client_id_issued_at: Math.floor(Date.now() / 1000) });
  }

  if (path === "/oauth/authorize" && (method === "GET" || method === "POST")) {
    const p = method === "GET" ? Object.fromEntries(url.searchParams) : await form(request);
    const client = await unseal<Client>(store, "cl", p.client_id);
    if (!client) return html(400, messagePage("Enlace no válido", "Esta app no se registró bien. Vuelve a añadir el conector en tu IA."));
    const redirect = p.redirect_uri || (client.u.length === 1 ? client.u[0] : "");
    if (!client.u.includes(redirect)) return html(400, messagePage("Enlace no válido", "La dirección de vuelta no coincide. Vuelve a añadir el conector."));
    const back = (params: Record<string, string>) => {
      const target = new URL(redirect);
      for (const [k, v] of Object.entries({ ...params, ...(p.state ? { state: p.state } : {}) })) target.searchParams.set(k, v);
      return new Response(null, { status: 302, headers: { ...SECURITY, Location: target.toString() } });
    };
    if ((p.response_type ?? "code") !== "code") return back({ error: "unsupported_response_type" });
    if (!/^[A-Za-z0-9_-]{43}$/.test(p.code_challenge ?? "") || (p.code_challenge_method ?? "S256") !== "S256")
      return back({ error: "invalid_request", error_description: "PKCE S256 is required." });
    const fields = { client_id: p.client_id, redirect_uri: redirect, code_challenge: p.code_challenge, ...(p.state ? { state: p.state } : {}) };
    const view = { client: client.n, host: new URL(redirect).host, fields };
    if (method === "GET") return html(200, authorizePage(view));
    if (store.failures.blocked(caller)) return html(429, authorizePage({ ...view, error: "Demasiados intentos. Espera un minuto." }));
    const space = await redeemConnectCode(store, p.code);
    if (!space) {
      store.failures.hit(caller);
      return html(401, authorizePage({ ...view, error: "Ese código no sirve. Mira el código de Gateway Hub (cambia cada 10 minutos)." }));
    }
    const code = await seal(store, "co", { s: space, c: p.client_id, r: redirect, ch: p.code_challenge, n: client.n,
      e: Date.now() + CODE_MS, id: newSpaceId() } satisfies Code);
    return back({ code });
  }

  if (path === "/oauth/token" && method === "POST") {
    const p = await form(request);
    if (p.grant_type !== "authorization_code") return oauthError(400, "unsupported_grant_type", "Only authorization_code.");
    const code = await unseal<Code>(store, "co", p.code);
    if (!code || code.e < Date.now() || code.c !== p.client_id || (p.redirect_uri && p.redirect_uri !== code.r)) {
      store.failures.hit(caller);
      return oauthError(400, "invalid_grant", "The code is wrong, expired or for another app.");
    }
    if (!await pkceMatches(p.code_verifier, code.ch)) return oauthError(400, "invalid_grant", "PKCE check failed.");
    if (!await useOnce(store, code.id, code.e)) return oauthError(400, "invalid_grant", "The code was already used.");
    const token = await deps.issueAgent(code.s, code.n);
    return json(200, { access_token: token, token_type: "Bearer", scope: "mcp" });
  }
  return json(404, { error: "not_found" });
}

/** Authorization codes work once: their ids are remembered until they expire. */
async function useOnce(store: HubStoreApi, id: string, exp: number, now = Date.now()): Promise<boolean> {
  let used: Record<string, number> = {};
  try { used = JSON.parse(await store.getConfig("oauth.used") ?? "{}"); } catch { /* start over */ }
  for (const [k, e] of Object.entries(used)) if (e < now) delete used[k];
  if (used[id]) return false;
  used[id] = exp;
  await store.setConfig("oauth.used", JSON.stringify(used));
  return true;
}
