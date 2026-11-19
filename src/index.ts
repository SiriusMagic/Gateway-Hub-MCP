// Gateway Hub MCP (Cloudflare Worker): one MCP link that gives an AI several apps. HTTPS only. Where each
// request goes:
// - sign-in and new spaces (OAuth, /register) → the "directory" object;
// - a secret that names a space ("gw_s<space>_…") → that person's own object, u_<space>;
// - anything else → "main", the single-owner hub (HUB_TOKEN), as before.
import { type HubEnv } from "./modules/types.ts";
import { CORS, SECURITY, json } from "./serve.ts";
import { givenToken } from "./auth.ts";
import { spaceOf } from "./spaces.ts";
import { OAUTH_PATHS } from "./oauth/directory.ts";
import type { HubStore } from "./store.ts";

export { HubStore } from "./store.ts";

interface Env extends HubEnv { HUB: DurableObjectNamespace<HubStore> }

const local = (url: URL) => url.hostname === "127.0.0.1" || url.hostname === "localhost";

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    // Over plain HTTP the token would already have crossed the network in clear: refuse, don't redirect.
    if (url.protocol !== "https:" && !local(url)) return json(400, { error: "Gateway Hub: use https://" });
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: { ...CORS, ...SECURITY } });
    if (url.pathname === "/") return new Response("Gateway Hub OK\n", { headers: { "Content-Type": "text/plain", ...SECURITY } });
    const hub = (name: string) => env.HUB.get(env.HUB.idFromName(name));
    if (OAUTH_PATHS.includes(url.pathname)) return hub("directory").directory(request);
    const token = givenToken(request, url);
    const space = spaceOf(token);
    if (space) return hub(`u_${space}`).tenant(request);
    // The public link with no sign-in yet: tell the AI app where to sign in (OAuth discovery).
    if (url.pathname === "/mcp" && !token && request.method === "POST") return json(401,
      { jsonrpc: "2.0", id: null, error: { code: -32001, message: "Gateway Hub: sign in first." } },
      { "WWW-Authenticate": `Bearer resource_metadata="${url.origin}/.well-known/oauth-protected-resource"` });
    return hub("main").fetch(request);
  },
};
