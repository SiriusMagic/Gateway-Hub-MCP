// JSON admin API used by the Android app: owner-only, header-only token, no CORS, same rules as the panel.
import { test } from "node:test";
import assert from "node:assert/strict";
import { STONE_LINK, TOKEN, hub } from "./helpers.ts";

test("only the owner token, and only in the header", async () => {
  const h = hub();
  assert.equal((await h.api("GET", "/state", undefined, null)).status, 401);
  assert.equal((await h.api("GET", "/state", undefined, "nope")).status, 401);
  const { body } = await h.api("POST", "/agents", { name: "A", apps: ["utils_"], days: 1 });
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
  await h.api("POST", "/agents", { name: "Claude", apps: ["utils_", "notes_"], readOnly: true, days: 30 });
  const { body } = await h.api("GET", "/state");
  assert.equal(body.version, "0.0.2");
  assert.equal(body.modules[0].androidPackage, "com.zirakusa.stone.kotlin.preview");
  assert.deepEqual(body.modules.map((m: any) => m.prefix), ["music_", "gallery_", "calc_", "notes_", "utils_"]);
  assert.equal(body.agents[0].name, "Claude");
  assert.equal(body.agents[0].readOnly, true);
  assert.equal(body.agents[0].tokenHash, undefined);
  assert.equal(body.metrics.week.calls, 0);
});

test("create returns the link once; it works on /mcp; pause, resume and revoke", async () => {
  const h = hub();
  const made = await h.api("POST", "/agents", { name: "ChatGPT", apps: ["utils_"], days: 0 });
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
  assert.equal((await h.api("POST", "/agents", { name: "", apps: ["utils_"] })).body.error.code, "INVALID_ARGUMENT");
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
  assert.equal(apps.find((a: any) => a.prefix === "utils_").online, true);
  assert.deepEqual(apps.find((a: any) => a.prefix === "music_").linked, false);
  await h.api("POST", "/stone", { link: STONE_LINK });
  apps = (await h.api("GET", "/scan")).body.apps;
  const music = apps.find((a: any) => a.prefix === "music_");
  assert.ok(music.linked && music.online && music.tools === 2 && music.remote);
  assert.ok(stone.calls.every((c) => ["initialize", "tools/list"].includes(c.body.method))); // the scan only reads
  const offline = hub(h.store, fakeStone({ status: 503 }).fetcher);
  const down = (await offline.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "music_");
  assert.ok(down.linked && !down.online);
  assert.match(down.detail, /not open/);
});

test("scan tells who manages Stone: a bridge for LLM/Hub tokens, the hub when Stone redirects", async () => {
  const { fakeStone, MemoryStore } = await import("./helpers.ts");
  const bridge = hub(new MemoryStore(), fakeStone({ token: { name: "Casa", kind: "hub", usage: "Hub de casa", mode: "read" } }).fetcher);
  assert.match((await bridge.api("POST", "/stone", { link: STONE_LINK })).body.message, /Puente/);
  let music = (await bridge.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "music_");
  assert.equal(music.managedBy, "app");
  assert.deepEqual(music.token, { name: "Casa", kind: "hub", usage: "Hub de casa", mode: "read" });
  assert.match(music.detail, /Puente.*Casa.*solo lectura.*Hub de casa/);
  const managed = hub(bridge.store, fakeStone({ token: { name: "Gateway Hub", kind: "redirect", usage: "", mode: "full" } }).fetcher);
  music = (await managed.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "music_");
  assert.equal(music.managedBy, "hub");
  assert.match(music.detail, /Administrado por el hub/);
  // Odd values from the app are cut to size, never trusted as markup.
  const odd = hub(bridge.store, fakeStone({ token: { name: "x".repeat(500), kind: 3, usage: "<b>hola</b>", mode: "write" } }).fetcher);
  music = (await odd.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "music_");
  assert.equal(music.token.name.length, 60);
  assert.equal(music.token.kind, "");
});

test("Luma (gallery_): linked by its own link, tools keep their names, calls reach the app with its token", async () => {
  const { fakeStone, MemoryStore } = await import("./helpers.ts");
  const luma = fakeStone({ token: { name: "Hub", kind: "hub", usage: "Fotos", mode: "read" } });
  const h = hub(new MemoryStore(), luma.fetcher);
  const { RELAY } = await import("./helpers.ts");
  const link = `https://${RELAY}/d/abcdefabcdefabcdefabcdef/mcp?token=lum_` + "x".repeat(43);
  assert.equal((await h.api("POST", "/apps/notes_", { link })).body.error.code, "NOT_FOUND");
  assert.equal((await h.api("POST", "/apps/gallery_", { link: "https://evil.example/d/x/mcp" })).body.error.code, "INVALID_LINK");
  assert.match((await h.api("POST", "/apps/gallery_", { link })).body.message, /Luma vinculada.*Puente/);
  const names = (await h.rpc("tools/list")).body.result.tools.map((t: any) => t.name);
  assert.ok(names.includes("gallery_player_state"));                 // prefixed once, not twice
  assert.ok(names.includes("gallery_status"));
  await h.rpc("tools/call", { name: "gallery_player_state", arguments: {} });
  const last = luma.calls.at(-1)!;
  assert.equal(last.body.params.name, "player_state");               // the app's own name
  assert.equal(last.auth, "Bearer lum_" + "x".repeat(43));          // its token, in the header
  const scan = (await h.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "gallery_");
  assert.ok(scan.linked && scan.online && scan.managedBy === "app");
  await h.api("DELETE", "/apps/gallery_");
  assert.equal((await h.api("GET", "/scan")).body.apps.find((a: any) => a.prefix === "gallery_").linked, false);
});

test("the Calculadora app is calc_: its own tool names, its token, read through the relay", async () => {
  const { fakeStone, MemoryStore, RELAY } = await import("./helpers.ts");
  const calc = fakeStone({ token: { name: "Hub", kind: "redirect", usage: "", mode: "write" } });
  const h = hub(new MemoryStore(), calc.fetcher);
  const link = `https://${RELAY}/d/abcdefabcdefabcdefabcdef/mcp?token=cal_` + "y".repeat(43);
  assert.match((await h.api("POST", "/apps/calc_", { link })).body.message, /Calculadora vinculada.*Administrado por el hub/);
  await h.rpc("tools/call", { name: "calc_player_state", arguments: {} });
  assert.equal(calc.calls.at(-1)!.body.params.name, "player_state");
  assert.equal(calc.calls.at(-1)!.auth, "Bearer cal_" + "y".repeat(43));
});
