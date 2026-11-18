// What each phone says it offers: its provider apps (one prefix each) and the execution packages its
// Gateway issued. The hub never signs packages: it only picks which ones could cover a call, and the app
// verifies them.
import type { HubStoreApi } from "../modules/types.ts";

export interface PackageRecord { id: string; agent: string; acts: string[]; tools: string[]; exp: number; status: string }
export interface ProviderRecord { device: string; app: string; prefix: string; title: string; packages: PackageRecord[]; seen: number }

const KEY = "channel.providers";
const PREFIX = /^[a-z][a-z0-9]{0,15}_$/;
const APP = /^[a-zA-Z][a-zA-Z0-9_.]{2,100}$/;
const ID = /^pkg_[A-Za-z0-9_-]{8,64}$/;
const MAX_PACKAGES = 64;

export async function providers(store: HubStoreApi): Promise<Record<string, ProviderRecord>> {
  try { return JSON.parse(await store.getConfig(KEY) ?? "{}"); } catch { return {}; }
}

export async function providerFor(store: HubStoreApi, prefix: string): Promise<ProviderRecord | null> {
  return (await providers(store))[prefix] ?? null;
}

const strings = (v: unknown, max: number) => Array.isArray(v) ? v.filter((x) => typeof x === "string").map((x) => x.slice(0, 80)).slice(0, max) : [];

/** Saves a phone's registration. A prefix another phone already serves is not taken over. */
export async function register(store: HubStoreApi, device: string, message: Record<string, unknown>): Promise<void> {
  const all = await providers(store);
  for (const [prefix, record] of Object.entries(all)) if (record.device === device) delete all[prefix];
  const list = Array.isArray(message.providers) ? message.providers.slice(0, 10) : [];
  for (const raw of list as Record<string, unknown>[]) {
    const prefix = String(raw?.prefix ?? ""), app = String(raw?.app ?? "");
    if (!PREFIX.test(prefix) || !APP.test(app) || (all[prefix] && all[prefix].device !== device)) continue;
    const packages = (Array.isArray(raw.packages) ? raw.packages : []).slice(0, MAX_PACKAGES)
      .map((p: Record<string, unknown>) => ({ id: String(p?.id ?? ""), agent: String(p?.agent ?? ""), acts: strings(p?.acts, 5),
        tools: strings(p?.tools, 100), exp: Number(p?.exp) || 0, status: String(p?.status ?? "") }))
      .filter((p: PackageRecord) => ID.test(p.id) && /^(\*|[A-Za-z0-9_-]{1,40})$/.test(p.agent));
    all[prefix] = { device, app, prefix, title: String(raw.title ?? app).slice(0, 60), packages, seen: Date.now() };
  }
  await store.setConfig(KEY, JSON.stringify(all));
}

/** Active packages that could cover [tool] (without its prefix) for [agent], the agent's own first; at most 8. */
export function candidates(record: ProviderRecord, agent: string, tool: string | null, now = Date.now()): string[] {
  const covers = (p: PackageRecord) => tool === null || p.tools.length === 0 ||
    p.tools.some((t) => t.endsWith("*") ? tool.startsWith(t.slice(0, -1)) : t === tool);
  return record.packages
    .filter((p) => p.status === "active" && p.exp * 1000 > now && (p.agent === agent || p.agent === "*") && covers(p))
    .sort((a, b) => (a.agent === "*" ? 1 : 0) - (b.agent === "*" ? 1 : 0))
    .slice(0, 8).map((p) => p.id);
}
