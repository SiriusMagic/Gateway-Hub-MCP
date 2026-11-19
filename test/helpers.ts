import type { Agent, CallStat, HubAdminStore, HubStoreApi, MetricsSummary } from "../src/modules/types.ts";
import { Limiter } from "../src/auth.ts";
import { type ServeOptions, serve } from "../src/serve.ts";
import { TEST_MODULES } from "./demo-module.ts";

export const TOKEN = "hub_test_token_0123456789abcdefghijklmnop";

export class MemoryStore implements HubStoreApi, HubAdminStore {
  channel?: import("../src/channel/core.ts").Channel;
  accepted: string[] = [];
  acceptChannel?: (device: string) => Response;
  requests = new Limiter(120);
  failures = new Limiter(20);
  config = new Map<string, string>();
  async getConfig(k: string) { return this.config.get(k) ?? null; }
  async setConfig(k: string, v: string | null) { if (v === null) this.config.delete(k); else this.config.set(k, v); }
  agents = new Map<string, Agent>();
  calls: { at: number; agent: string; tool: string; ok: boolean; ms: number }[] = [];
  async agentsCreate(a: Agent) { this.agents.set(a.id, { ...a }); }
  async agentsList() { return [...this.agents.values()].sort((a, b) => b.created - a.created); }
  async agentByHash(h: string) { return [...this.agents.values()].find((a) => a.tokenHash === h) ?? null; }
  async agentsSet(id: string, c: Partial<Agent>) { const a = this.agents.get(id); if (a) Object.assign(a, c); }
  async agentsDelete(id: string) { return this.agents.delete(id); }
  async metricsRecord(agent: string, tool: string, ok: boolean, ms: number) { this.calls.push({ at: Date.now(), agent, tool, ok, ms }); }
  async metricsSummary(since: number): Promise<MetricsSummary> {
    const rows = this.calls.filter((c) => c.at >= since);
    const group = (key: (c: typeof rows[0]) => string): CallStat[] => {
      const m = new Map<string, CallStat>();
      for (const c of rows) { const k = key(c), g = m.get(k) ?? { name: k, calls: 0, errors: 0, avgMs: 0 };
        g.avgMs = (g.avgMs * g.calls + c.ms) / (g.calls + 1); g.calls++; if (!c.ok) g.errors++; m.set(k, g); }
      return [...m.values()].sort((a, b) => b.calls - a.calls);
    };
    return { since, calls: rows.length, errors: rows.filter((c) => !c.ok).length, byAgent: group((c) => c.agent),
      byApp: group((c) => c.tool.slice(0, c.tool.indexOf("_") + 1)), byTool: group((c) => c.tool) };
  }
}

export function hub(store = new MemoryStore(), fetcher: typeof fetch = fetch, opts: ServeOptions = {}) {
  const env = { HUB_TOKEN: TOKEN };
  return {
    store,
    async rpc(method: string, params: unknown = {}, token: string | null = TOKEN, ip = "1.1.1.1") {
      const headers: Record<string, string> = { "Content-Type": "application/json", "CF-Connecting-IP": ip };
      if (token) headers.Authorization = `Bearer ${token}`;
      const response = await serve(new Request("https://hub.example/mcp", { method: "POST", headers,
        body: JSON.stringify({ jsonrpc: "2.0", id: 7, method, params }) }), env, store, fetcher, TEST_MODULES, opts);
      return { status: response.status, body: response.status === 202 ? null : await response.json() as any, response };
    },
    async api(method: string, path: string, body?: unknown, token: string | null = TOKEN, ip = "1.1.1.1") {
      const headers: Record<string, string> = { "CF-Connecting-IP": ip, "Content-Type": "application/json" };
      if (token) headers.Authorization = `Bearer ${token}`;
      const response = await serve(new Request(`https://hub.example/api/admin${path}`, { method, headers,
        body: body === undefined ? undefined : JSON.stringify(body) }), env, store, fetcher, TEST_MODULES, opts);
      return { status: response.status, body: await response.json() as any, response };
    },
    async pair(code: string, name = "Tablet", ip = "1.1.1.1") {
      const response = await serve(new Request("https://hub.example/pair", { method: "POST", headers: { "CF-Connecting-IP": ip,
        "Content-Type": "application/json" }, body: JSON.stringify({ code, name }) }), env, store, fetcher, TEST_MODULES, opts);
      return { status: response.status, body: await response.json() as any };
    },
    async channel(token: string, ip = "1.1.1.1") {
      const response = await serve(new Request("https://hub.example/channel", { headers: { Authorization: `Bearer ${token}`,
        Upgrade: "websocket", "CF-Connecting-IP": ip } }), env, store, fetcher, TEST_MODULES, opts);
      return { status: response.status };
    },
    async admin(fields: Record<string, string | string[]>, ip = "1.1.1.1") {
      const body = new URLSearchParams();
      for (const [k, v] of Object.entries(fields)) for (const x of [v].flat()) body.append(k, x);
      const response = await serve(new Request("https://hub.example/admin", { method: "POST",
        headers: { "CF-Connecting-IP": ip }, body }), env, store, fetcher, TEST_MODULES, opts);
      return { status: response.status, html: await response.text() };
    },
  };
}
