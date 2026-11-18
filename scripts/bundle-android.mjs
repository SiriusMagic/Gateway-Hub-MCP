// Puts the hub Worker, as Cloudflare runs it, inside the Gateway Hub APK (assets/hub-worker.js), so the
// app can deploy or update each person's own hub with their Cloudflare token. Run: npm run bundle:android
import { execFileSync } from "node:child_process";
import { mkdtempSync, readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const out = mkdtempSync(join(tmpdir(), "hub-bundle-"));
execFileSync("npx", ["wrangler", "deploy", "--dry-run", "--outdir", out], { stdio: "inherit" });
const assets = "android/app/src/main/assets";
mkdirSync(assets, { recursive: true });
writeFileSync(join(assets, "hub-worker.js"), readFileSync(join(out, "index.js")));
const version = /export const VERSION = "([^"]+)"/.exec(readFileSync("src/serve.ts", "utf8"))[1];
const compat = /compatibility_date = "([^"]+)"/.exec(readFileSync("wrangler.toml", "utf8"))[1];
writeFileSync(join(assets, "hub-worker.json"), JSON.stringify({ version, compatibilityDate: compat, mainModule: "index.js",
  durableObject: { binding: "HUB", className: "HubStore", migrationTag: "v1" } }, null, 2) + "\n");
console.log(`assets/hub-worker.js updated (hub ${version}).`);
