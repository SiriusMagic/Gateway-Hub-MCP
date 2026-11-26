import assert from "node:assert/strict";
import { test } from "node:test";
import { serve } from "../src/serve.ts";
import playTitle from "../src/codemode/stone-plan.codemode.js";
import { type RunStoneCode } from "../src/codemode/stone.ts";
import { parseStoneLink } from "../src/modules/music/stone.ts";
import { MemoryStore, RELAY, STONE_LINK, TOKEN } from "./helpers.ts";

const track = (id: string, title: string, artist = "Artista") => ({ id, title, artist, isVideo: false });
const run: RunStoneCode = (methods) => playTitle(methods);

async function setup(songs = [track("1", "Alone")], linked = true, playback = true) {
  const store = new MemoryStore();
  if (linked) await store.setConfig("music.link", JSON.stringify(parseStoneLink(STONE_LINK, RELAY)));
  const calls: { name: string; args: any }[] = [];
  let playing = "";
  const relay: typeof fetch = async (input, init) => {
    assert.equal(String(input), `https://${RELAY}/d/abcdefghijkl12/mcp`);
    assert.equal(init?.redirect, "manual");
    assert.match(new Headers(init?.headers).get("Authorization") ?? "", /^Bearer stn_/);
    const { params } = JSON.parse(String(init?.body));
    calls.push({ name: params.name, args: params.arguments });
    if (params.name === "player_play_song") playing = params.arguments.songId;
    const result = params.name === "library_search" ? { results: songs, total: songs.length }
      : params.name === "player_state" ? { songId: playing, playing: playback, volumePercent: 53 }
        : { ok: true };
    return Response.json({ result: { structuredContent: result } });
  };
  async function request(args: unknown, token = TOKEN, executor: RunStoneCode | null = run) {
    const req = new Request("https://hub.example/api/admin/code-mode/stone", { method: "POST",
      headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(args) });
    const response = await serve(req, { HUB_TOKEN: TOKEN, STONE_RELAY_HOST: RELAY }, store, relay, executor ?? undefined);
    return { status: response.status, body: await response.json() as any, response };
  }
  return { request, calls };
}

test("Code Mode preserves the title, applies settings, plays once and checks the active track", async () => {
  const { request, calls } = await setup([track("2", "Alone Again"), track("1", "Alone")]);
  const { body, response } = await request({ titulo: "Alone", volumen: 50, ecualizador: true });
  assert.equal(body.status, "playing");
  assert.equal(body.track.id, "1");
  assert.equal(body.steps.volume.actual, 53);
  assert.match(response.headers.get("Server-Timing") ?? "", /^code-mode;dur=/);
  assert.deepEqual(calls, [
    { name: "library_search", args: { query: "Alone", kind: "audio", limit: 20 } },
    { name: "player_set_volume", args: { percent: 50 } },
    { name: "equalizer_set_enabled", args: { enabled: true } },
    { name: "player_play_song", args: { songId: "1" } },
    { name: "player_state", args: {} },
  ]);
});

test("Code Mode asks for a choice and never picks another track for a missing title or invalid selection", async () => {
  const { request, calls } = await setup([track("1", "Alone", "A"), track("2", "Alone", "B")]);
  const ambiguous = await request({ titulo: "Alone" });
  assert.deepEqual(ambiguous.body.matches.map((match: any) => match.artist), ["A", "B"]);
  assert.equal((await request({ titulo: "Missing" })).body.steps.search.code, "TRACK_NOT_FOUND");
  assert.equal((await request({ titulo: "Alone", idPista: "bad" })).body.steps.search.code, "INVALID_SELECTION");
  assert.equal(calls.filter((call) => call.name !== "library_search").length, 0);
  assert.equal((await request({ titulo: "Alone", idPista: "2" })).body.track.id, "2");
});

test("Code Mode rejects unauthorized, unlinked, disabled and invalid calls before contacting Stone", async () => {
  const { request, calls } = await setup([], false);
  assert.equal((await request({ titulo: "Alone" }, "wrong")).status, 401);
  assert.equal((await request({ titulo: "Alone" }, TOKEN, null)).body.error.code, "CODE_MODE_DISABLED");
  assert.equal((await request({ titulo: "Alone", code: "return 5" })).status, 400);
  assert.equal((await request({ titulo: "Alone", volumen: 101 })).status, 400);
  assert.equal((await request({ titulo: "a".repeat(2100) })).status, 400);
  assert.equal((await request({ titulo: "Alone" })).body.error.code, "NOT_LINKED");
  assert.deepEqual(calls, []);
});

test("Code Mode retries readbacks only and does not claim unconfirmed playback", async () => {
  const { request, calls } = await setup([track("1", "Alone")], true, false);
  assert.equal((await request({ titulo: "Alone" })).body.steps.play.code, "PLAY_NOT_CONFIRMED");
  assert.equal(calls.filter((call) => call.name === "player_play_song").length, 1);
  assert.equal(calls.filter((call) => call.name === "player_state").length, 3);
});
