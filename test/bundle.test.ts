// The Worker inside the APK (assets/hub-worker.js) must be the current one: bump VERSION and run
// "npm run bundle:android" whenever src/ changes in a way the app should deploy.
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { VERSION } from "../src/serve.ts";

test("the APK carries the hub Worker of this version, with no secret inside", () => {
  const meta = JSON.parse(readFileSync("android/app/src/main/assets/hub-worker.json", "utf8"));
  const script = readFileSync("android/app/src/main/assets/hub-worker.js", "utf8");
  assert.equal(meta.version, VERSION);
  assert.ok(script.includes(`VERSION = "${VERSION}"`));
  assert.match(script, /HubStore = class extends DurableObject/);
  assert.doesNotMatch(script, /hub_[A-Za-z0-9_-]{20,}|dev_[A-Za-z0-9_-]{20,}|gw_[A-Za-z0-9_-]{20,}/);
});
