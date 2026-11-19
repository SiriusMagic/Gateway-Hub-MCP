// The shared hub: every person's space only answers its own secrets.
import { test } from "node:test";
import assert from "node:assert/strict";
import { issueAgent, setupSpace, spaceOf } from "../src/spaces.ts";
import { MAX_AGENTS } from "../src/agents.ts";
import { MemoryStore, TOKEN, hub } from "./helpers.ts";

const SPACE = "0123456789abcdef";

test("secrets name their space; old single-owner secrets do not", () => {
  assert.equal(spaceOf(`gw_s${SPACE}_${"A".repeat(43)}`), SPACE);
  assert.equal(spaceOf(`dev_s${SPACE}_${"b-".repeat(20)}`), SPACE);
  assert.equal(spaceOf(TOKEN), null);
  assert.equal(spaceOf("gw_" + "A".repeat(43)), null);
  assert.equal(spaceOf(`gw_sXYZ456789abcdef_${"A".repeat(43)}`), null);   // not hex
  assert.equal(spaceOf(null), null);
});

test("a space: its owner secret opens it, HUB_TOKEN and other spaces' secrets do not", async () => {
  const store = new MemoryStore();
  const made = await setupSpace(store, SPACE, "Pixel");
  assert.match(made.owner, new RegExp(`^hub_s${SPACE}_`)); assert.match(made.secret, new RegExp(`^dev_s${SPACE}_`));
  assert.equal(made.device.name, "Pixel");
  assert.ok(![...store.config.values()].some((v) => v.includes(made.owner)));          // only its hash is kept
  await assert.rejects(setupSpace(store, SPACE, "again"));                              // set up once
  const h = hub(store, fetch, { tenant: true });
  const state = await h.api("GET", "/state", undefined, made.owner);
  assert.equal(state.status, 200); assert.equal(state.body.shared, true); assert.equal(state.body.publicUrl, "https://hub.example/mcp");
  assert.equal((await h.api("GET", "/state", undefined, TOKEN)).status, 401);
  const other = await setupSpace(new MemoryStore(), "fedcba9876543210", "Otro");
  assert.equal((await h.api("GET", "/state", undefined, other.owner)).status, 401);
  // Agents made in the space carry it, and work only there.
  const agent = (await h.api("POST", "/agents", { name: "Claude", apps: ["demo_"], days: 0 }, made.owner)).body;
  assert.equal(spaceOf(agent.token), SPACE);
  assert.equal((await h.rpc("tools/list", {}, agent.token)).status, 200);
  assert.equal((await h.rpc("tools/list", {}, other.owner)).status, 401);
});

test("a space that was never set up answers nothing", async () => {
  const h = hub(new MemoryStore(), fetch, { tenant: true });
  assert.equal((await h.rpc("tools/list", {}, `gw_s${SPACE}_${"A".repeat(43)}`)).status, 401);
  assert.equal((await h.api("GET", "/state", undefined, TOKEN)).status, 401);
});

test("connect codes: only a shared space can ask for one", async () => {
  const store = new MemoryStore();
  const { owner } = await setupSpace(store, SPACE, "Pixel");
  const withCodes = hub(store, fetch, { tenant: true, connectCode: async () => ({ code: "12345678", expiresAt: 1 }) });
  assert.deepEqual((await withCodes.api("POST", "/connect-code", {}, owner)).body, { code: "12345678", expiresAt: 1 });
  assert.equal((await hub().api("POST", "/connect-code", {})).status, 404);             // single-owner hub
});

test("AIs connected by the public link get every app; a full space drops the longest unused", async () => {
  const store = new MemoryStore();
  await setupSpace(store, SPACE, "Pixel");
  const token = await issueAgent(store, "  Claude  ");
  assert.equal(spaceOf(token), SPACE);
  const [agent] = await store.agentsList();
  assert.deepEqual([agent.name, agent.modules, agent.readOnly, agent.expiresAt], ["Claude", ["*"], false, 0]);
  await assert.rejects(issueAgent(new MemoryStore(), "x"));                              // no space, no agent
  for (let i = 1; i < MAX_AGENTS; i++) await issueAgent(store, `IA ${i}`);
  await store.agentsSet(agent.id, { lastUsed: Date.now() + 1000 });                      // the first one is in use
  await issueAgent(store, "Nueva");
  const names = (await store.agentsList()).map((a) => a.name);
  assert.equal(names.length, MAX_AGENTS);
  assert.ok(names.includes("Claude") && names.includes("Nueva") && !names.includes("IA 1"));
});
