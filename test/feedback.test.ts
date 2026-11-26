import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import test from "node:test";
import { fixture, proposal } from "../scripts/intent-review.ts";
import { SqlFeedbackQueue } from "../src/feedback/queue.ts";
import { serve } from "../src/serve.ts";
import { MemoryStore, RELAY, TOKEN } from "./helpers.ts";

const REVIEW = "review_test_Q7vX2pL9mZ4rT8wK1nB6cY3hD5sF0gJe";
const sampleRecord = (original = "Calcula 12 más 8 y arranca música en segundo plano Nombre privado con el volumen a 40 y el ecualizador activo") => ({
  id: crypto.randomUUID(), actionId: `${crypto.randomUUID()}:1`, original, status: "corregido",
  version: "2", step: 1, plan: [{ action: "CALCULATE" }, { action: "UNKNOWN" }],
  result: { ok: false, text: "texto privado" }, expected: "Reproduce música que elegí, de Identidad Privada",
});
function setup() {
  const db = new DatabaseSync(":memory:");
  const sql = {
    exec(statement: string, ...args: string[]) {
      const stmt = db.prepare(statement);
      if (/^SELECT\b/.test(statement)) return { toArray: () => stmt.all(...args) };
      const { changes } = stmt.run(...args);
      return { rowsWritten: Number(changes) };
    }
  } as unknown as SqlStorage;
  const queue = new SqlFeedbackQueue(sql);
  const store = Object.assign(new MemoryStore(), { feedback: queue });
  async function api(path: string, token?: string, body?: unknown, method = "POST") {
    const headers = { Authorization: token ? `Bearer ${token}` : "" };
    const response = await serve(new Request(`https://hub.example/api/feedback/${path}`, {
      method, headers, body: body === undefined ? undefined : JSON.stringify(body),
    }), { HUB_TOKEN: TOKEN, FEEDBACK_REVIEW_TOKEN: REVIEW, STONE_RELAY_HOST: RELAY }, store);
    return { status: response.status, data: await response.json() as any };
  }
  async function register() {
    const installation = crypto.randomUUID();
    const { status, data } = await api("credentials", TOKEN, { installation });
    assert.equal(status, 201);
    return { token: data.token, installation };
  }
  return { db, queue, api, register };
}

test("feedback requires scoped credentials, persists private evidence and deduplicates retries", async () => {
  const { api, register, db } = setup();
  const { token, installation } = await register();
  const event = sampleRecord();
  assert.equal((await api("events", TOKEN, event)).status, 401);
  assert.equal((await api("events", token, { ...event, step: 9 })).status, 400);
  assert.equal((await api("events", token, event)).status, 202);
  assert.equal((await api("events", token, event)).status, 202);
  assert.equal((await api("private", REVIEW, undefined, "GET")).status, 401);
  assert.equal((await api("private", token, undefined, "GET")).status, 401);
  const events = (await api("private", TOKEN, undefined, "GET")).data.events;
  assert.equal(events.length, 1);
  assert.equal(events[0].event.original, event.original);
  assert.equal((await api("credentials", TOKEN, { installation }, "DELETE")).status, 200);
  assert.equal((await api("events", token, event)).status, 401);
  assert.equal((await api("credentials", TOKEN, { installation })).status, 403);
  db.close();
});

test("review prioritizes independent installations, leases once, and strips all private fields", async () => {
  const { api, register, db } = setup();
  const one = await register(), two = await register();
  for (const [device, event] of [[one, sampleRecord()], [one, sampleRecord()], [two, sampleRecord()],
    [two, sampleRecord("empieza música Otro nombre")], [one, { ...sampleRecord(), status: "funcionó" }]] as const) {
    assert.equal((await api("events", device.token, event)).status, 202);
  }
  assert.equal((await api("review/lease", one.token)).status, 401);
  const { data } = await api("review/lease", REVIEW);
  assert.equal(data.cases.length, 2);
  assert.equal(data.cases[0].installations, 2);
  assert.equal(data.cases[0].sample.variant, "arranca");
  assert.doesNotMatch(JSON.stringify(data), /privad|Identidad|installation.*[0-9a-f]{8}-[0-9a-f]{4}/i);
  assert.deepEqual((await api("review/lease", REVIEW)).data.cases, []);
  const row = data.cases[0];
  assert.equal((await api("review/finish", REVIEW, { key: row.key, lease: crypto.randomUUID(), status: "proposed" })).status, 409);
  assert.equal((await api("review/finish", REVIEW, { key: row.key, lease: data.lease, status: "rejected" })).status, 200);
  assert.deepEqual((await api("review/lease", REVIEW)).data.cases, []);
  assert.equal(proposal({ key: row.key, addMusicPrefix: "arranca", code: "private" }, row.key, row.sample), false);
  assert.equal(proposal({ key: row.key, addMusicPrefix: "arranca" }, row.key, row.sample), true);
  assert.equal(fixture(row.sample).phrase, "Calcula dos más 2 y arranca música en segundo plano Canción del Faro con el volumen a 50 y el ecualizador activo");
  db.close();
});
