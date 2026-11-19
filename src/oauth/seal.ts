// Small signed values for the public link's sign-in: client ids (dynamic registration) and one-time
// authorization codes carry their own data, signed with HMAC-SHA256, so the directory stores no table of
// clients. The key is random, made on first use and kept only in the directory's storage.
import type { HubStoreApi } from "../modules/types.ts";

const KEY = "oauth.key";
const b64 = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const unb64 = (text: string) => Uint8Array.from(atob(text.replace(/-/g, "+").replace(/_/g, "/")), (c) => c.charCodeAt(0));

async function key(store: HubStoreApi): Promise<CryptoKey> {
  let raw = await store.getConfig(KEY);
  if (!raw) { raw = b64(crypto.getRandomValues(new Uint8Array(32))); await store.setConfig(KEY, raw); }
  return crypto.subtle.importKey("raw", unb64(raw), { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]);
}

/** "<kind>.<data>.<signature>": [kind] keeps a client id from passing for a code. */
export async function seal(store: HubStoreApi, kind: string, data: unknown): Promise<string> {
  const body = b64(new TextEncoder().encode(JSON.stringify(data)));
  const sig = new Uint8Array(await crypto.subtle.sign("HMAC", await key(store), new TextEncoder().encode(`${kind}.${body}`)));
  return `${kind}.${body}.${b64(sig)}`;
}

/** The data inside a value [seal] made with the same [kind], or null when it was changed or is something else. */
export async function unseal<T>(store: HubStoreApi, kind: string, value: unknown): Promise<T | null> {
  if (typeof value !== "string" || value.length > 4096) return null;
  const [k, body, sig] = value.split(".");
  if (k !== kind || !body || !sig) return null;
  try {
    const ok = await crypto.subtle.verify("HMAC", await key(store), unb64(sig), new TextEncoder().encode(`${kind}.${body}`));
    return ok ? JSON.parse(new TextDecoder().decode(unb64(body))) as T : null;
  } catch { return null; }
}

/** PKCE S256: base64url(SHA-256(verifier)) must equal the challenge the AI app sent first. */
export async function pkceMatches(verifier: unknown, challenge: string): Promise<boolean> {
  if (typeof verifier !== "string" || !/^[A-Za-z0-9._~-]{43,128}$/.test(verifier)) return false;
  return b64(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier)))) === challenge;
}
