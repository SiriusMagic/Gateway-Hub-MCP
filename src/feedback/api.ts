import { sameSecret, weakOwnerToken } from "../auth.ts";
import { object, project } from "./projection.ts";
import type { Event, FeedbackQueue, Finish, Vote } from "./queue.ts";

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const votes = ["funcionó", "falló", "corregido"] satisfies Vote[];
const finishes = ["proposed", "no_proposal", "rejected", "already_fixed", "temporary_error"] satisfies Finish[];
const cap = 24_576;

async function digest(value: string) {
  return Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value))),
    (byte) => byte.toString(16).padStart(2, "0")).join("");
}

async function payload(request: Request) {
  const stream = request.body?.getReader();
  if (!stream) return null;
  let length = 0;
  const decoder = new TextDecoder();
  let text = "";
  try {
    while (true) {
      const next = await stream.read();
      if (next.done) break;
      length += next.value.byteLength;
      if (length > cap) { await stream.cancel(); return null; }
      text += decoder.decode(next.value, { stream: true });
    }
    return object(JSON.parse(text + decoder.decode()));
  } catch { return null; }
}

export async function feedback(request: Request, path: string, queue: FeedbackQueue, owner: string,
    review: string | undefined, rejected: () => void, now = Date.now()): Promise<Response> {
  const reply = (status: number, body: unknown) => Response.json(body, { status,
    headers: { "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff", "Referrer-Policy": "no-referrer",
      "Strict-Transport-Security": "max-age=31536000; includeSubDomains", "X-Frame-Options": "DENY" } });
  const unauthorized = () => { rejected(); return reply(401, { error: "UNAUTHORIZED" }); };
  const bearer = /^bearer\s+(\S+)$/i.exec(request.headers.get("authorization") ?? "")?.[1] ?? "";
  const method = request.method;
  const invalid = () => reply(400, { error: "INVALID_FEEDBACK" });

  if (path === "credentials" || path === "private") {
    if (!bearer || !await sameSecret(bearer, owner)) return unauthorized();
    if (path === "private" && method === "GET") return reply(200, { events: queue.recent() });
    if (path !== "credentials" || (method !== "POST" && method !== "DELETE")) return reply(405, { error: "METHOD" });
    const data = await payload(request);
    if (!data || typeof data.installation !== "string" || !uuid.test(data.installation)) return invalid();
    if (method === "DELETE") { queue.revoke(data.installation); return reply(200, { ok: true }); }
    const token = "fb_" + crypto.randomUUID() + crypto.randomUUID();
    return queue.issue(data.installation, await digest(token)) ? reply(201, { token }) : reply(403, { error: "REVOKED" });
  }

  if (path === "events" && method === "POST") {
    const installation = bearer.startsWith("fb_") ? queue.authorize(await digest(bearer)) : null;
    if (!installation) return unauthorized();
    const data = await payload(request);
    if (!data || typeof data.id !== "string" || !uuid.test(data.id) ||
        typeof data.actionId !== "string" || !/^[-0-9a-f]{36}:\d{1,2}$/.test(data.actionId) ||
        !votes.includes(data.status as Vote) || typeof data.original !== "string" || typeof data.version !== "string" ||
        !Array.isArray(data.plan) || !object(data.result) || !Number.isInteger(data.step) ||
        Number(data.step) < 0 || Number(data.step) >= data.plan.length) return invalid();
    queue.record(installation, data as Event, project(data), now);
    return reply(202, { ok: true });
  }

  if (path === "review/lease" || path === "review/finish") {
    if (!review || weakOwnerToken(review)) return reply(503, { error: "REVIEW_NOT_CONFIGURED" });
    if (!bearer || !await sameSecret(bearer, review) || await sameSecret(bearer, owner)) return unauthorized();
    if (method !== "POST") return reply(405, { error: "METHOD" });
    if (path === "review/lease") return reply(200, queue.lease(now));
    const data = await payload(request);
    if (!data || typeof data.key !== "string" || !/^[0-5](?:-[01]){5}$/.test(data.key) ||
        typeof data.lease !== "string" || !uuid.test(data.lease) || !finishes.includes(data.status as Finish)) return invalid();
    return queue.finish(data.key, data.lease, data.status as Finish, now) ? reply(200, { ok: true }) : reply(409, { error: "LEASE_EXPIRED" });
  }
  return reply(404, { error: "NOT_FOUND" });
}
