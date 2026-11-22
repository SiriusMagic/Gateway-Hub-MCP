// Token checks and per-caller limits. The hub token is a Cloudflare secret (wrangler secret put
// HUB_TOKEN); it is compared through SHA-256 in constant time and never logged.

async function digest(value: string): Promise<Uint8Array> {
  return new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)));
}

export async function sameSecret(given: string, expected: string): Promise<boolean> {
  const [a, b] = await Promise.all([digest(given), digest(expected)]);
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
  return diff === 0;
}

/**
 * Why the owner token is too weak to guard the hub (and, through it, every phone the hub reaches), or null when it is
 * strong. The owner token is the one secret a person chooses, so it must not be guessable: at least 32 characters,
 * enough different characters, and not a run like "abcdef…" or "0123…". Generate one with `openssl rand -base64 32`.
 */
export function weakOwnerToken(token: string): "SHORT" | "LOW_VARIETY" | "SEQUENCE" | null {
  if (token.length < MIN_OWNER_TOKEN) return "SHORT";
  if (new Set(token).size < 12) return "LOW_VARIETY";
  let steps = 0;
  for (let i = 1; i < token.length; i++) if (Math.abs(token.charCodeAt(i) - token.charCodeAt(i - 1)) <= 1) steps++;
  if (steps / (token.length - 1) > 0.6) return "SEQUENCE";
  return null;
}
export const MIN_OWNER_TOKEN = 32;

/** Bearer header, X-Api-Key, or ?token= for apps that only accept a link (ChatGPT). */
export function givenToken(request: Request, url: URL): string | null {
  const auth = request.headers.get("authorization") ?? "";
  const bearer = /^bearer\s+(.+)$/i.exec(auth)?.[1]?.trim();
  return bearer || request.headers.get("x-api-key")?.trim() || url.searchParams.get("token")?.trim() || null;
}

/** Fixed-window counter per key; the Durable Object is a single instance, so counts are global. */
export class Limiter {
  private hits = new Map<string, { since: number; count: number }>();
  private max: number;
  private windowMs: number;
  constructor(max: number, windowMs = 60_000) { this.max = max; this.windowMs = windowMs; }
  /** Counts one hit; true when [key] is over the limit. */
  hit(key: string, now = Date.now()): boolean {
    if (this.hits.size > 5_000) this.hits.clear();
    let entry = this.hits.get(key);
    if (!entry || now - entry.since > this.windowMs) { entry = { since: now, count: 0 }; this.hits.set(key, entry); }
    return ++entry.count > this.max;
  }
  blocked(key: string, now = Date.now()): boolean {
    const entry = this.hits.get(key);
    return !!entry && now - entry.since <= this.windowMs && entry.count >= this.max;
  }
}
