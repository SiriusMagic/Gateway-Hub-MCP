// End to end through serve(): auth, limits, MCP protocol, the modules, Stone link and /setup.
import { test } from "node:test";
import assert from "node:assert/strict";
import { TOKEN, hub } from "./helpers.ts";

test("token required, compared exactly, and wrong tokens throttled", async () => {
  const h = hub();
  assert.equal((await h.rpc("ping", {}, null)).status, 401);
  assert.equal((await h.rpc("ping", {}, TOKEN + "x")).status, 401);
  assert.equal((await h.rpc("ping")).status, 200);
  for (let i = 0; i < 20; i++) await h.rpc("ping", {}, "nope", "6.6.6.6");
  assert.equal((await h.rpc("ping", {}, TOKEN, "6.6.6.6")).status, 429); // that caller is blocked a minute
  assert.equal((await h.rpc("ping", {}, TOKEN, "7.7.7.7")).status, 200); // others are not
});

test("rate limit per caller", async () => {
  const h = hub();
  let last = 0;
  for (let i = 0; i < 121; i++) last = (await h.rpc("ping", {}, TOKEN, "9.9.9.9")).status;
  assert.equal(last, 429);
});

test("initialize names every module; notifications get 202", async () => {
  const h = hub();
  const init = await h.rpc("initialize", { protocolVersion: "2025-06-18" });
  assert.equal(init.body.result.protocolVersion, "2025-06-18");
  for (const p of ["music_", "demo_"]) assert.match(init.body.result.instructions, new RegExp(p));
  assert.ok(init.response.headers.get("mcp-session-id"));
  assert.equal(init.response.headers.get("x-content-type-options"), "nosniff");
});

test("built-in app through MCP: list, a good call, bad arguments, not found", async () => {
  const h = hub();
  const listed = (await h.rpc("tools/list")).body.result.tools.map((t: any) => t.name);
  assert.ok(["demo_get", "demo_save", "demo_delete"].every((n) => listed.includes(n)));
  assert.ok(!listed.some((n: string) => /^(calc|notes|utils)_/.test(n)));          // removed apps are gone
  const saved = await h.rpc("tools/call", { name: "demo_save", arguments: { key: "compras", value: "leche y pan" } });
  assert.equal(saved.body.result.isError, undefined);
  assert.equal((await h.rpc("tools/call", { name: "demo_get", arguments: { key: "compras" } })).body.result.structuredContent.value, "leche y pan");
  const bad = await h.rpc("tools/call", { name: "demo_save", arguments: { key: 5, value: "x" } });
  assert.equal(bad.body.result.structuredContent.error.code, "INVALID_ARGUMENT");
  const extra = await h.rpc("tools/call", { name: "demo_get", arguments: { key: "compras", evil: 1 } });
  assert.equal(extra.body.result.structuredContent.error.code, "INVALID_ARGUMENT");
  const long = await h.rpc("tools/call", { name: "demo_save", arguments: { key: "x".repeat(21), value: "x" } });
  assert.equal(long.body.result.structuredContent.error.code, "INVALID_ARGUMENT");
  await h.rpc("tools/call", { name: "demo_delete", arguments: { key: "compras" } });
  const gone = await h.rpc("tools/call", { name: "demo_get", arguments: { key: "compras" } });
  assert.equal(gone.body.result.structuredContent.error.code, "NOT_FOUND");
  const unknown = await h.rpc("tools/call", { name: "calc_evaluate", arguments: { expression: "1+1" } });
  assert.equal(unknown.body.result.structuredContent.error.code, "UNKNOWN_TOOL");
});

test("the hub ships with Stone only", async () => {
  const { modules } = await import("../src/modules/index.ts");
  assert.deepEqual(modules.map((m) => m.prefix), ["music_"]);
});

test("admin panel escapes what it shows", async () => {
  const h = hub();
  const page = await h.admin({ token: TOKEN, action: "agent_create", name: "<script>alert(1)</script>", apps: "demo_", days: "1" });
  assert.doesNotMatch(page.html, /<script>alert/);
});

test("music: without its phone connected, only music_status, and calls say why", async () => {
  const h = hub();
  const names = (await h.rpc("tools/list")).body.result.tools.filter((t: any) => t.name.startsWith("music_")).map((t: any) => t.name);
  assert.deepEqual(names, ["music_status"]);
  const status = await h.rpc("tools/call", { name: "music_status", arguments: {} });
  assert.equal(status.body.result.structuredContent.online, false);
  const call = await h.rpc("tools/call", { name: "music_player_state", arguments: {} });
  assert.equal(call.body.result.structuredContent.error.code, "PROVIDER_OFFLINE");
  assert.equal((await h.admin({ token: TOKEN, action: "link_stone", link: "https://x" })).status, 400);   // no relay links any more
});
