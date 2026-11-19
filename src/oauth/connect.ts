// The 8-digit code Gateway shows: the person types it on the public link's sign-in page, and that is how the
// hub knows whose phone the AI app should reach. One use, 10 minutes; only its hash is kept, and wrong codes
// count as wrong tokens (the caller is blocked after 20 a minute).
import type { HubStoreApi } from "../modules/types.ts";
import { sha256Hex } from "../agents.ts";

const KEY = "oauth.connect";
export const CONNECT_MS = 10 * 60_000;
const MAX_LIVE = 20_000;
type Live = Record<string, { space: string; exp: number }>;

async function live(store: HubStoreApi, now: number): Promise<Live> {
  let all: Live = {};
  try { all = JSON.parse(await store.getConfig(KEY) ?? "{}"); } catch { /* start over */ }
  for (const [hash, entry] of Object.entries(all)) if (entry.exp <= now) delete all[hash];
  return all;
}

/** A new code for [space]; any earlier code of that space stops working. */
export async function createConnectCode(store: HubStoreApi, space: string, now = Date.now()) {
  const all = await live(store, now);
  for (const [hash, entry] of Object.entries(all)) if (entry.space === space) delete all[hash];
  if (Object.keys(all).length >= MAX_LIVE) throw new Error("too many live codes");
  let code = "", hash = "";
  do {
    const n = crypto.getRandomValues(new Uint32Array(1))[0] % 100_000_000;
    code = String(n).padStart(8, "0"); hash = await sha256Hex(code);
  } while (all[hash]);
  all[hash] = { space, exp: now + CONNECT_MS };
  await store.setConfig(KEY, JSON.stringify(all));
  return { code, expiresAt: now + CONNECT_MS };
}

/** The space of a valid code, which is used up; null when it is wrong or expired. Spaces and dashes are ignored. */
export async function redeemConnectCode(store: HubStoreApi, raw: unknown, now = Date.now()): Promise<string | null> {
  const code = String(raw ?? "").replace(/[\s-]/g, "");
  if (!/^\d{8}$/.test(code)) return null;
  const all = await live(store, now);
  const hash = await sha256Hex(code);
  const entry = all[hash];
  if (!entry) return null;
  delete all[hash];
  await store.setConfig(KEY, JSON.stringify(all));
  return entry.space;
}
