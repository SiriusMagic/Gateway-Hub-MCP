// Adding another phone: the owner's Gateway asks for a one-use code (5 minutes); the other phone's
// Gateway redeems it for its own channel credential. Only the code's hash is kept, and wrong codes count
// as wrong tokens (the caller is blocked after 20 a minute).
import type { HubStoreApi } from "../modules/types.ts";
import { sha256Hex } from "../agents.ts";
import { sameSecret } from "../auth.ts";
import { createDevice } from "./devices.ts";

const KEY = "channel.pairing";
const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // no 0/O, 1/I
export const PAIRING_MS = 5 * 60_000;

export async function createPairing(store: HubStoreApi, now = Date.now()) {
  const bytes = crypto.getRandomValues(new Uint8Array(10));
  const code = [...bytes].map((b) => ALPHABET[b % ALPHABET.length]).join("");
  await store.setConfig(KEY, JSON.stringify({ hash: await sha256Hex(code), expiresAt: now + PAIRING_MS }));
  return { code, expiresAt: now + PAIRING_MS };
}

/** Exchanges a valid code (once) for a device credential; null when the code is wrong or expired. */
export async function redeemPairing(store: HubStoreApi, rawCode: string, name: string, now = Date.now()) {
  const code = String(rawCode ?? "").toUpperCase().replace(/[^A-Z0-9]/g, "");
  const saved = await store.getConfig(KEY);
  if (!saved || code.length !== 10) return null;
  const { hash, expiresAt } = JSON.parse(saved) as { hash: string; expiresAt: number };
  if (now > expiresAt) { await store.setConfig(KEY, null); return null; }
  if (!await sameSecret(await sha256Hex(code), hash)) return null;
  await store.setConfig(KEY, null);                                  // one use
  return createDevice(store, name);
}
