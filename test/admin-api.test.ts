// JSON admin API used by the Android app: owner-only, header-only token, no CORS, same rules as the panel.
import { test } from "node:test";
import assert from "node:assert/strict";
import { TOKEN, hub, MemoryStore } from "./helpers.ts";
import { register } from "../src/channel/registry.ts";

test("only the owner token, and only in the header", async () => {
  const h = hub();
  assert.equal((await h.api("GET", "/state", undefined, null)).status, 401);
  assert.equal((await h.api("GET", "/state", undefined, "nope")).status, 401);
  const { body } = await h.api("POST", "/agents", { name: "A", apps: ["demo_"], days: 1 });
  assert.equal((await h.api("GET", "/state", undefined, body.token)).status, 401);   // agent links never administer
  const viaQuery = await h.api("GET", `/state?token=${TOKEN}`, undefined, null);
  assert.equal(viaQuery.status, 401);
  const ok = await h.api("GET", "/state");
  assert.equal(ok.status, 200);
  assert.equal(ok.response.headers.get("access-control-allow-origin"), null);         // no browser access
  assert.equal(ok.response.headers.get("cache-control"), "no-store");
});

test("state lists modules, agents without hashes and metrics", async () => {
  const h = hub();
  await h.api("POST", "/agents", { name: "Claude", apps: ["demo_"], readOnly: true, days: 30 });
  const { body } = await h.api("GET", "/state");
  assert.equal(body.version, "0.0.5");
  assert.equal(body.environment, "active");
  assert.equal(body.modules[0].androidPackage, "com.zirakusa.stone.kotlin.preview");
  assert.deepEqual(body.modules.map((m: any) => m.prefix), ["music_", "demo_"]);
  assert.equal(body.agents[0].name, "Claude");
  assert.equal(body.agents[0].readOnly, true);
  assert.equal(body.agents[0].tokenHash, undefined);
  assert.equal(body.metrics.week.calls, 0);
});

test("create returns the link once; it works on /mcp; pause, resume and revoke", async () => {
  const h = hub();
  const made = await h.api("POST", "/agents", { name: "ChatGPT", apps: ["demo_"], days: 0 });
  assert.match(made.body.url, /^https:\/\/hub\.example\/mcp\?token=gw_/);
  assert.equal(made.body.agent.expiresAt, 0);
  const id = made.body.agent.id;
  assert.equal((await h.rpc("ping", {}, made.body.token)).status, 200);
  assert.equal((await h.api("POST", `/agents/${id}/pause`, {})).status, 200);
  assert.equal((await h.rpc("ping", {}, made.body.token)).status, 401);
  assert.equal((await h.api("POST", `/agents/${id}/resume`, {})).status, 200);
  assert.equal((await h.rpc("ping", {}, made.body.token)).status, 200);
  assert.equal((await h.api("DELETE", `/agents/${id}`)).status, 200);
  assert.equal((await h.rpc("ping", {}, made.body.token)).status, 401);
  assert.equal((await h.api("DELETE", `/agents/${id}`)).status, 404);
});

test("validation: no name, no apps, unknown apps, bad JSON, unknown route", async () => {
  const h = hub();
  assert.equal((await h.api("POST", "/agents", { name: "", apps: ["demo_"] })).body.error.code, "INVALID_ARGUMENT");
  assert.equal((await h.api("POST", "/agents", { name: "X", apps: ["evil_"] })).body.error.code, "INVALID_ARGUMENT");
  assert.equal((await h.api("POST", "/agents", ["x"])).status, 400);
  assert.equal((await h.api("GET", "/nothing")).status, 404);
  assert.equal(h.store.agents.size, 0);
});

test("scan: built-in apps ready, Stone unlinked / registered offline / online through its channel, read-only", async () => {
  const store = new MemoryStore();
  const h = hub(store);
  let apps = (await h.api("GET", "/scan")).body.apps;
  assert.equal(apps.find((a: any) => a.prefix === "demo_").online, true);
  assert.equal(apps.find((a: any) => a.prefix === "music_").linked, false);
  await register(store, "dev1", { providers: [{ app: "com.zirakusa.stone", prefix: "music_", title: "Stone", packages: [] }] });
  let online = false; const sent: string[] = [];
  store.channel = { online: () => online, async request(_d, _a, m: any) { sent.push(m.method); return { result: { tools: [] } }; } };
  let music = (await h.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "music_");
  assert.ok(music.linked && !music.online);
  online = true;
  music = (await h.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "music_");
  assert.ok(music.linked && music.online && music.remote);
  assert.deepEqual(sent, []);                                                             // the scan only reads the registry
  assert.equal((await h.api("GET", "/state")).body.stoneLinked, true);
});
test("environment: pause and disconnect stop every tool call; connect brings them back", async () => {
  const store = new MemoryStore();
  const h = hub(store);
  await register(store, "dev1", { providers: [{ app: "com.zirakusa.stone", prefix: "music_", title: "Stone", packages: [] }] });
  const save = () => h.rpc("tools/call", { name: "demo_save", arguments: { key: "k", value: "v" } });
  assert.equal((await save()).body.result.isError, undefined);
  const paused = await h.api("POST", "/environment", { state: "paused" });
  assert.equal(paused.body.state, "paused");
  assert.equal((await h.api("GET", "/state")).body.environment, "paused");
  assert.equal((await save()).body.result.structuredContent.error.code, "PAUSED");
  assert.equal((await h.api("GET", "/state")).body.stoneLinked, true);               // pause keeps the links
  assert.equal((await h.rpc("ping")).status, 200);                                    // the link itself still answers
  await h.api("POST", "/environment", { state: "disconnected" });
  assert.equal((await save()).body.result.structuredContent.error.code, "DISCONNECTED");
  assert.equal((await h.api("GET", "/state")).body.stoneLinked, false);              // disconnect forgets the phones' apps
  await h.api("POST", "/environment", { state: "active" });
  assert.equal((await save()).body.result.isError, undefined);
  assert.equal((await h.api("POST", "/environment", { state: "off" })).body.error.code, "INVALID_ARGUMENT");
  assert.equal((await h.api("GET", "/state")).body.environment, "active");
});

test("activity since a moment, for the app's notifications", async () => {
  const h = hub();
  const start = Date.now();
  await h.rpc("tools/call", { name: "demo_save", arguments: { key: "k", value: "v" } });
  await h.rpc("tools/call", { name: "demo_get", arguments: { key: "missing" } });
  const { body } = await h.api("GET", `/activity?since=${start - 1}`);
  assert.equal(body.environment, "active");
  assert.equal(body.calls, 2); assert.equal(body.errors, 1);
  assert.equal(body.byTool.length, 2);
  assert.equal((await h.api("GET", `/activity?since=${Date.now() + 60_000}`)).body.calls, 0);
  assert.equal((await h.api("GET", "/activity?since=x")).status, 200);               // bad input = last 7 days
  assert.equal((await h.api("GET", "/activity", undefined, null)).status, 401);
});
