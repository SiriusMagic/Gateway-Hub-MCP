// JSON admin API used by the Android app: owner-only, header-only token, no CORS, same rules as the panel.
import { test } from "node:test";
import assert from "node:assert/strict";
import { STONE_LINK, TOKEN, hub } from "./helpers.ts";

test("only the owner token, and only in the header", async () => {
  const h = hub();
  assert.equal((await h.api("GET", "/state", undefined, null)).status, 401);
  assert.equal((await h.api("GET", "/state", undefined, "nope")).status, 401);
  const { body } = await h.api("POST", "/agents", { name: "A", apps: ["calc_"], days: 1 });
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
  await h.api("POST", "/agents", { name: "Claude", apps: ["calc_", "notes_"], readOnly: true, days: 30 });
  const { body } = await h.api("GET", "/state");
  assert.equal(body.version, "0.0.2");
  assert.equal(body.modules[0].androidPackage, "com.zirakusa.stone.kotlin.preview");
  assert.deepEqual(body.modules.map((m: any) => m.prefix), ["music_", "notes_", "calc_", "utils_"]);
  assert.equal(body.agents[0].name, "Claude");
  assert.equal(body.agents[0].readOnly, true);
  assert.equal(body.agents[0].tokenHash, undefined);
  assert.equal(body.metrics.week.calls, 0);
});

test("create returns the link once; it works on /mcp; pause, resume and revoke", async () => {
  const h = hub();
  const made = await h.api("POST", "/agents", { name: "ChatGPT", apps: ["calc_"], days: 0 });
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
  assert.equal((await h.api("POST", "/agents", { name: "", apps: ["calc_"] })).body.error.code, "INVALID_ARGUMENT");
  assert.equal((await h.api("POST", "/agents", { name: "X", apps: ["evil_"] })).body.error.code, "INVALID_ARGUMENT");
  assert.equal((await h.api("POST", "/agents", ["x"])).status, 400);
  assert.equal((await h.api("GET", "/nothing")).status, 404);
  assert.equal(h.store.agents.size, 0);
});

test("link and unlink Stone through the API", async () => {
  const h = hub();
  assert.equal((await h.api("POST", "/stone", { link: "https://evil.example/x" })).body.error.code, "INVALID_LINK");
  assert.match((await h.api("POST", "/stone", { link: STONE_LINK })).body.message, /vinculado/);
  assert.equal((await h.api("GET", "/state")).body.stoneLinked, true);
  await h.api("DELETE", "/stone");
  assert.equal((await h.api("GET", "/state")).body.stoneLinked, false);
});

test("scan: built-in apps ready, Stone unlinked / online / offline, read-only", async () => {
  const { fakeStone, MemoryStore } = await import("./helpers.ts");
  const stone = fakeStone();
  const h = hub(new MemoryStore(), stone.fetcher);
  let apps = (await h.api("GET", "/scan")).body.apps;
  assert.equal(apps.find((a: any) => a.prefix === "calc_").online, true);
  assert.deepEqual(apps.find((a: any) => a.prefix === "music_").linked, false);
  await h.api("POST", "/stone", { link: STONE_LINK });
  apps = (await h.api("GET", "/scan")).body.apps;
  const music = apps.find((a: any) => a.prefix === "music_");
  assert.ok(music.linked && music.online && music.tools === 2 && music.remote);
  assert.ok(stone.calls.every((c) => c.body.method === "tools/list"));          // the scan only reads
  const offline = hub(h.store, fakeStone({ status: 503 }).fetcher);
  const down = (await offline.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "music_");
  assert.ok(down.linked && !down.online);
  assert.match(down.detail, /not open/);
});
