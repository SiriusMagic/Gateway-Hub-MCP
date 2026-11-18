// The phones' channel: devices with their own credential, the registry of provider apps and packages,
// and Stone reached through Gateway Hub instead of the relay (which stays as a fallback).
import { test } from "node:test";
import assert from "node:assert/strict";
import { ChannelCore } from "../src/channel/core.ts";
import { candidates, register, providerFor } from "../src/channel/registry.ts";
import { MemoryStore, STONE_LINK, TOKEN, fakeStone, hub } from "./helpers.ts";

test("channel core: answers by id, from the right phone; timeouts cancel; a closed phone fails at once", async () => {
  const sent: any[] = [];
  const sockets: Record<string, { send(d: string): void }> = { dev1: { send: (d) => sent.push(JSON.parse(d)) } };
  const core = new ChannelCore((d) => sockets[d], async () => {});
  const answer = core.request("dev1", "com.app", { method: "tools/list" }, 1_000);
  assert.equal(sent[0].t, "req"); assert.equal(sent[0].app, "com.app");
  await core.receive("dev2", JSON.stringify({ t: "res", id: sent[0].id, reply: { evil: true } }));   // another phone cannot answer
  await core.receive("dev1", JSON.stringify({ t: "res", id: sent[0].id, reply: { result: { ok: 1 } } }));
  assert.deepEqual(await answer, { result: { ok: 1 } });
  const slow = core.request("dev1", "com.app", {}, 20);
  await assert.rejects(slow, (e: any) => e.code === "PROVIDER_TIMEOUT");
  assert.equal(sent.at(-1).t, "cancel");
  const cut = core.request("dev1", "com.app", {}, 1_000);
  core.closed("dev1");
  await assert.rejects(cut, (e: any) => e.code === "PROVIDER_OFFLINE");
  await assert.rejects(core.request("nope", "com.app", {}, 10), (e: any) => e.code === "PROVIDER_OFFLINE");
  await core.receive("dev1", "not json"); await core.receive("dev1", "x".repeat((1 << 20) + 1));   // ignored
});

test("devices: credential shown once, only its hash kept, the owner token never opens the channel", async () => {
  const store = new MemoryStore();
  store.acceptChannel = (device) => { store.accepted.push(device); return new Response("ok"); };
  const h = hub(store);
  const made = (await h.api("POST", "/devices", { name: "Pixel" })).body;
  assert.match(made.secret, /^dev_/);
  assert.ok(!(await store.getConfig("channel.devices"))!.includes(made.secret));
  const listed = (await h.api("GET", "/devices")).body.devices;
  assert.equal(listed[0].name, "Pixel"); assert.equal(listed[0].hash, undefined); assert.equal(listed[0].online, false);
  const open = (token: string) => h.channel(token);
  assert.equal((await open("dev_wrong")).status, 401);
  assert.equal((await open(TOKEN)).status, 401);
  assert.equal((await open(made.secret)).status, 200);
  assert.deepEqual(store.accepted, [made.device.id]);
  assert.equal((await h.api("DELETE", `/devices/${made.device.id}`)).status, 200);
  assert.equal((await open(made.secret)).status, 401);                                    // revoked at once
});

const pkg = (id: string, agent: string, status = "active", tools: string[] = []) =>
  ({ id, agent, acts: ["leer"], tools, exp: Math.floor(Date.now() / 1000) + 3600, status });

test("registry: valid prefixes only, no takeover by another phone, candidates per agent and tool", async () => {
  const store = new MemoryStore();
  await register(store, "dev1", { providers: [{ app: "com.zirakusa.stone", prefix: "music_", title: "Stone",
    packages: [pkg("pkg_all0000001", "*"), pkg("pkg_claude00001", "claude", "active", ["library_*"]), pkg("pkg_pending001", "claude", "pending"), { id: "bad" }] },
    { app: "com.x", prefix: "BAD" }] });
  await register(store, "dev2", { providers: [{ app: "com.evil", prefix: "music_", packages: [] }] });
  const r = (await providerFor(store, "music_"))!;
  assert.equal(r.device, "dev1"); assert.equal(r.packages.length, 3);
  assert.deepEqual(candidates(r, "claude", "library_search"), ["pkg_claude00001", "pkg_all0000001"]);
  assert.deepEqual(candidates(r, "claude", "player_state"), ["pkg_all0000001"]);
  assert.deepEqual(candidates(r, "chatgpt", null), ["pkg_all0000001"]);
  assert.equal(await providerFor(store, "BAD"), null);
});

/** A phone whose Gateway forwards to a Stone that answers like the real one (checks the grant). */
function phone(store: MemoryStore, online = true) {
  const seen: any[] = [];
  store.channel = {
    online: () => online,
    async request(_device, app, message: any) {
      seen.push({ app, message });
      const grant = message.params._meta["com.zirakusa.gatewayhub/grant"];
      if (message.method === "tools/list") return { result: { tools: [{ name: "player_state", description: "Now playing", inputSchema: { type: "object" } }] } };
      if (!grant.packages.length) return { error: { code: -32001, message: "NO_PACKAGE" } };
      return { result: { content: [{ type: "text", text: `ran ${message.params.name} for ${grant.agent}` }] } };
    },
  };
  return seen;
}

test("music goes through the phone's channel with the agent's packages; the relay is only the fallback", async () => {
  const stone = fakeStone();
  const store = new MemoryStore();
  const h = hub(store, stone.fetcher);
  await h.api("POST", "/stone", { link: STONE_LINK });
  const device = (await h.api("POST", "/devices", { name: "Pixel" })).body.device.id;
  await register(store, device, { providers: [{ app: "com.zirakusa.stone", prefix: "music_", title: "Stone", packages: [pkg("pkg_all0000001", "*")] }] });
  const seen = phone(store);
  const relayCalls = stone.calls.length;
  const names = (await h.rpc("tools/list")).body.result.tools.map((t: any) => t.name);
  assert.ok(names.includes("music_player_state"));
  const ran = await h.rpc("tools/call", { name: "music_player_state", arguments: {} });
  assert.equal(ran.body.result.content[0].text, "ran player_state for owner");
  assert.equal(seen.at(-1).app, "com.zirakusa.stone");
  assert.deepEqual(seen.at(-1).message.params._meta["com.zirakusa.gatewayhub/grant"].packages, ["pkg_all0000001"]);
  assert.equal(stone.calls.length, relayCalls);                                           // nothing went to the relay
  // An agent with no package for it is refused by the hub before reaching the phone.
  await register(store, device, { providers: [{ app: "com.zirakusa.stone", prefix: "music_", title: "Stone", packages: [pkg("pkg_other00001", "someone")] }] });
  const denied = await h.rpc("tools/call", { name: "music_player_state", arguments: {} });
  assert.equal(denied.body.result.structuredContent.error.code, "NO_PACKAGE");
  // Phone offline: back to the relay, as today.
  phone(store, false);
  const viaRelay = await h.rpc("tools/call", { name: "music_player_state", arguments: {} });
  assert.equal(viaRelay.body.result.content[0].text, "ran player_state");
});
