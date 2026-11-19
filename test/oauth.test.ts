// The public link: an AI app signs in with the code Gateway shows and only reaches that person's space.
import { test } from "node:test";
import assert from "node:assert/strict";
import { Limiter } from "../src/auth.ts";
import { type DirectoryDeps, handleDirectory } from "../src/oauth/directory.ts";
import { createConnectCode, redeemConnectCode } from "../src/oauth/connect.ts";
import { issueAgent, setupSpace } from "../src/spaces.ts";
import { MemoryStore, hub } from "./helpers.ts";

const REDIRECT = "https://claude.ai/api/mcp/auth_callback";
const VERIFIER = "v".repeat(20) + "-verifier-" + "x".repeat(20);
const CHALLENGE = Buffer.from(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(VERIFIER))).toString("base64url");

function directory() {
  const store = Object.assign(new MemoryStore(), { signups: new Limiter(5, 3_600_000) });
  const spaces = new Map<string, MemoryStore>();
  const deps: DirectoryDeps = {
    async setupSpace(space, phone) { const s = new MemoryStore(); spaces.set(space, s); return setupSpace(s, space, phone); },
    async issueAgent(space, name) { return issueAgent(spaces.get(space)!, name); },
  };
  const call = async (method: string, path: string, body?: unknown, form = false, ip = "1.1.1.1") => {
    const headers: Record<string, string> = { "CF-Connecting-IP": ip };
    let payload: string | undefined;
    if (body !== undefined) {
      headers["Content-Type"] = form ? "application/x-www-form-urlencoded" : "application/json";
      payload = form ? new URLSearchParams(body as Record<string, string>).toString() : JSON.stringify(body);
    }
    const url = new URL(`https://hub.example${path}`);
    return handleDirectory(new Request(url, { method, headers, body: payload, redirect: "manual" }), url, store, deps);
  };
  return { store, spaces, call };
}

test("discovery tells AI apps where to sign in", async () => {
  const d = directory();
  const resource = await (await d.call("GET", "/.well-known/oauth-protected-resource")).json() as any;
  assert.deepEqual(resource.authorization_servers, ["https://hub.example"]); assert.equal(resource.resource, "https://hub.example/mcp");
  const meta = await (await d.call("GET", "/.well-known/oauth-authorization-server")).json() as any;
  assert.equal(meta.token_endpoint, "https://hub.example/oauth/token"); assert.deepEqual(meta.code_challenge_methods_supported, ["S256"]);
  const unsigned = await hub().rpc("tools/list", {}, null);
  assert.equal(unsigned.status, 401);
  assert.match(unsigned.response.headers.get("WWW-Authenticate")!, /resource_metadata="https:\/\/hub\.example\/\.well-known\/oauth-protected-resource"/);
  assert.equal((await d.call("GET", "/oauth/nothing")).status, 404);
});

test("the whole sign-in: new phone, code, AI app, token, and the token reaches only that space", async () => {
  const d = directory();
  const joined = await (await d.call("POST", "/register", { name: "Pixel" })).json() as any;
  assert.equal(joined.hub, "https://hub.example"); assert.match(joined.owner, /^hub_s[0-9a-f]{16}_/);
  const space = [...d.spaces.keys()][0];
  const { code } = await createConnectCode(d.store, space);
  assert.match(code, /^\d{8}$/);

  const client = await (await d.call("POST", "/oauth/register", { client_name: "Claude <b>", redirect_uris: [REDIRECT] })).json() as any;
  const query = new URLSearchParams({ response_type: "code", client_id: client.client_id, redirect_uri: REDIRECT,
    code_challenge: CHALLENGE, code_challenge_method: "S256", state: "st8" });
  const page = await (await d.call("GET", `/oauth/authorize?${query}`)).text();
  assert.match(page, /Claude &lt;b&gt;/); assert.match(page, /claude\.ai/);                 // escaped, shows where it returns
  const fields = { client_id: client.client_id, redirect_uri: REDIRECT, code_challenge: CHALLENGE, state: "st8" };
  assert.equal((await d.call("POST", "/oauth/authorize", { ...fields, code: "00000000" }, true)).status, 401);
  const ok = await d.call("POST", "/oauth/authorize", { ...fields, code: code.slice(0, 4) + " " + code.slice(4) }, true);
  assert.equal(ok.status, 302);
  const back = new URL(ok.headers.get("Location")!);
  assert.equal(back.origin + back.pathname, REDIRECT); assert.equal(back.searchParams.get("state"), "st8");
  assert.equal(await redeemConnectCode(d.store, code), null);                              // the code was used up

  const exchange = (verifier: string) => d.call("POST", "/oauth/token", { grant_type: "authorization_code",
    code: back.searchParams.get("code")!, client_id: client.client_id, redirect_uri: REDIRECT, code_verifier: verifier }, true);
  assert.equal((await exchange("w".repeat(50))).status, 400);                              // PKCE
  const token = await (await exchange(VERIFIER)).json() as any;
  assert.equal(token.token_type, "Bearer"); assert.match(token.access_token, new RegExp(`^gw_s${space}_`));
  assert.equal((await exchange(VERIFIER)).status, 400);                                    // once
  const [agent] = await d.spaces.get(space)!.agentsList();
  assert.equal(agent.name, "Claude <b>"); assert.deepEqual(agent.modules, ["*"]);
  const mcp = hub(d.spaces.get(space)!, fetch, { tenant: true });
  assert.equal((await mcp.rpc("tools/list", {}, token.access_token)).status, 200);
});

test("bad clients, redirects, codes and too many sign-ups are refused", async () => {
  const d = directory();
  for (const uris of [[], ["http://evil.example/cb"], ["https://x.example/cb#frag"], ["javascript:alert(1)"]])
    assert.equal((await d.call("POST", "/oauth/register", { redirect_uris: uris })).status, 400);
  assert.equal((await d.call("POST", "/oauth/register", { redirect_uris: ["http://localhost:3334/cb"] })).status, 201);
  const forged = await d.call("GET", `/oauth/authorize?client_id=cl.e30.AAAA&redirect_uri=${encodeURIComponent(REDIRECT)}`);
  assert.equal(forged.status, 400);
  const client = await (await d.call("POST", "/oauth/register", { redirect_uris: [REDIRECT] })).json() as any;
  const other = await d.call("GET", `/oauth/authorize?client_id=${client.client_id}&redirect_uri=${encodeURIComponent("https://evil.example/cb")}`);
  assert.equal(other.status, 400);                                                        // never redirects elsewhere
  const noPkce = await d.call("GET", `/oauth/authorize?client_id=${client.client_id}&state=s`);
  assert.equal(noPkce.status, 302); assert.equal(new URL(noPkce.headers.get("Location")!).searchParams.get("error"), "invalid_request");
  assert.equal((await d.call("POST", "/oauth/token", { grant_type: "password" }, true)).status, 400);
  assert.equal((await d.call("POST", "/oauth/token", { grant_type: "authorization_code", code: "co.x.y" }, true)).status, 400);
  for (let i = 0; i < 5; i++) assert.equal((await d.call("POST", "/register", {}, false, "9.9.9.9")).status, 200);
  assert.equal((await d.call("POST", "/register", {}, false, "9.9.9.9")).status, 429);
});

test("connect codes expire, and a new one replaces the old", async () => {
  const store = new MemoryStore();
  const first = await createConnectCode(store, "aaaaaaaaaaaaaaaa", 1_000);
  const second = await createConnectCode(store, "aaaaaaaaaaaaaaaa", 2_000);
  assert.equal(await redeemConnectCode(store, first.code, 3_000), null);
  assert.equal(await redeemConnectCode(store, second.code, second.expiresAt + 1), null);
  const third = await createConnectCode(store, "aaaaaaaaaaaaaaaa");
  assert.equal(await redeemConnectCode(store, "abc"), null);
  assert.equal(await redeemConnectCode(store, third.code), "aaaaaaaaaaaaaaaa");
});
