// Phase 5: another phone joins with a one-use code, and an app on it is routed under its own prefix.
import { test } from "node:test";
import assert from "node:assert/strict";
import { register } from "../src/channel/registry.ts";
import { MemoryStore, hub } from "./helpers.ts";

test("pairing: the code works once, expires, and wrong codes count as wrong tokens", async () => {
  const store = new MemoryStore();
  const h = hub(store);
  const { code, expiresAt } = (await h.api("POST", "/pairing", {})).body;
  assert.match(code, /^[A-HJ-NP-Z2-9]{10}$/); assert.ok(expiresAt > Date.now());
  assert.ok(!(await store.getConfig("channel.pairing"))!.includes(code));               // only its hash
  assert.equal((await h.pair("WRONGCODE2")).status, 401);
  const joined = await h.pair(code.toLowerCase().replace(/(.{5})/, "$1-"), "Tablet");   // case and dashes do not matter
  assert.equal(joined.status, 200); assert.match(joined.body.secret, /^dev_/); assert.equal(joined.body.device.name, "Tablet");
  assert.equal((await h.pair(code)).status, 401);                                       // used up
  const late = (await h.api("POST", "/pairing", {})).body.code;
  const saved = JSON.parse((await store.getConfig("channel.pairing"))!); saved.expiresAt = Date.now() - 1;
  await store.setConfig("channel.pairing", JSON.stringify(saved));
  assert.equal((await h.pair(late)).status, 401);                                       // expired
  for (let i = 0; i < 20; i++) await h.pair("ZZZZZZZZZZ", "x", "8.8.8.8");
  assert.equal((await h.pair("ZZZZZZZZZZ", "x", "8.8.8.8")).status, 429);              // throttled like wrong tokens
});

test("a second phone's Stone gets music2_ and is routed through its own channel", async () => {
  const store = new MemoryStore();
  const h = hub(store);
  const pkg = { id: "pkg_tablet00001", agent: "*", acts: ["leer"], tools: [], exp: Math.floor(Date.now() / 1000) + 3600, status: "active" };
  await register(store, "phone", { providers: [{ app: "com.zirakusa.stone", prefix: "music_", title: "Stone", packages: [pkg] }] });
  await register(store, "tablet", { providers: [{ app: "com.zirakusa.stone", prefix: "music_", title: "Stone (tablet)", packages: [pkg] }] });
  const calls: string[] = [];
  store.channel = {
    online: (d) => d === "tablet",
    async request(device, _app, message: any) {
      calls.push(device);
      if (message.method === "tools/list") return { result: { tools: [{ name: "player_state", description: "Now", inputSchema: { type: "object" } }] } };
      return { result: { content: [{ type: "text", text: `ran on ${device}` }] } };
    },
  };
  const names = (await h.rpc("tools/list")).body.result.tools.map((t: any) => t.name);
  assert.ok(names.includes("music2_player_state"));
  const ran = await h.rpc("tools/call", { name: "music2_player_state", arguments: {} });
  assert.equal(ran.body.result.content[0].text, "ran on tablet");
  assert.ok(calls.every((d) => d === "tablet"));
  const modules = (await h.api("GET", "/state")).body.modules.map((m: any) => m.prefix);
  assert.ok(modules.includes("music2_"));                                                // agents can be limited to it
});
