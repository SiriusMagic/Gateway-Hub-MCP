// The hub's single Durable Object: storage (SQLite, parameterized queries only), global limits,
// and the request handling for /mcp and /admin.
import { DurableObject } from "cloudflare:workers";
import type { Agent, CallStat, HubAdminStore, HubEnv, HubStoreApi, MetricsSummary } from "./modules/types.ts";
import { serve } from "./serve.ts";
import { Limiter } from "./auth.ts";
import { ChannelCore } from "./channel/core.ts";
import { register } from "./channel/registry.ts";

type Row = Record<string, string | number>;
const toAgent = (r: Row): Agent => ({ id: String(r.id), name: String(r.name), tokenHash: String(r.token_hash),
  modules: JSON.parse(String(r.modules)), readOnly: r.read_only === 1, paused: r.paused === 1,
  expiresAt: Number(r.expires_at), created: Number(r.created), lastUsed: Number(r.last_used) });

export class HubStore extends DurableObject<HubEnv> implements HubStoreApi, HubAdminStore {
  requests = new Limiter(120);   // per caller and minute
  failures = new Limiter(20);    // wrong tokens per caller and minute
  private sql: SqlStorage;

  constructor(ctx: DurableObjectState, env: HubEnv) {
    super(ctx, env);
    this.sql = ctx.storage.sql;
    this.sql.exec("CREATE TABLE IF NOT EXISTS config(key TEXT PRIMARY KEY, value TEXT NOT NULL)");
    this.sql.exec(`CREATE TABLE IF NOT EXISTS agents(id TEXT PRIMARY KEY, name TEXT NOT NULL, token_hash TEXT NOT NULL UNIQUE,
      modules TEXT NOT NULL, read_only INTEGER NOT NULL, paused INTEGER NOT NULL, expires_at INTEGER NOT NULL,
      created INTEGER NOT NULL, last_used INTEGER NOT NULL)`);
    this.sql.exec(`CREATE TABLE IF NOT EXISTS calls(at INTEGER NOT NULL, agent TEXT NOT NULL, tool TEXT NOT NULL,
      ok INTEGER NOT NULL, ms INTEGER NOT NULL)`);
    this.sql.exec("CREATE INDEX IF NOT EXISTS calls_at ON calls(at)");
    // Keep-alive pings are answered without waking the object.
    ctx.setWebSocketAutoResponse(new WebSocketRequestResponsePair("ping", "pong"));
  }

  fetch(request: Request): Promise<Response> { return serve(request, this.env, this); }

  // ---- phones' channel (WebSocket hibernation: one socket per device, tagged with its id) ----
  channel = new ChannelCore((device) => this.ctx.getWebSockets(device)[0], (device, message) => register(this, device, message));

  acceptChannel(device: string): Response {
    for (const old of this.ctx.getWebSockets(device)) { try { old.close(1000, "replaced"); } catch { /* already closed */ } }
    const [client, server] = Object.values(new WebSocketPair());
    this.ctx.acceptWebSocket(server, [device]);
    return new Response(null, { status: 101, webSocket: client });
  }
  dropChannel(device: string) {
    for (const socket of this.ctx.getWebSockets(device)) { try { socket.close(4001, "revoked"); } catch { /* closed */ } }
    this.channel.closed(device);
  }
  async webSocketMessage(socket: WebSocket, data: string | ArrayBuffer) { await this.channel.receive(this.ctx.getTags(socket)[0] ?? "", data); }
  async webSocketClose(socket: WebSocket) { this.channel.closed(this.ctx.getTags(socket)[0] ?? ""); try { socket.close(1000, "closed"); } catch { /* closed */ } }

  async getConfig(key: string) {
    const rows = this.sql.exec("SELECT value FROM config WHERE key = ?", key).toArray() as Row[];
    return rows.length ? String(rows[0].value) : null;
  }
  async setConfig(key: string, value: string | null) {
    if (value === null) this.sql.exec("DELETE FROM config WHERE key = ?", key);
    else this.sql.exec("INSERT INTO config(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value", key, value);
  }

  // ---- agents (only the SHA-256 of each token is stored) ----
  async agentsCreate(a: Agent) {
    this.sql.exec("INSERT INTO agents VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)", a.id, a.name, a.tokenHash, JSON.stringify(a.modules),
      a.readOnly ? 1 : 0, a.paused ? 1 : 0, a.expiresAt, a.created, a.lastUsed);
  }
  async agentsList() {
    return (this.sql.exec("SELECT * FROM agents ORDER BY created DESC").toArray() as Row[]).map(toAgent);
  }
  async agentByHash(hash: string) {
    const rows = this.sql.exec("SELECT * FROM agents WHERE token_hash = ?", hash).toArray() as Row[];
    return rows.length ? toAgent(rows[0]) : null;
  }
  async agentsSet(id: string, changes: { paused?: boolean; lastUsed?: number }) {
    if (changes.paused !== undefined) this.sql.exec("UPDATE agents SET paused = ? WHERE id = ?", changes.paused ? 1 : 0, id);
    if (changes.lastUsed !== undefined) this.sql.exec("UPDATE agents SET last_used = ? WHERE id = ?", changes.lastUsed, id);
  }
  async agentsDelete(id: string) {
    return this.sql.exec("DELETE FROM agents WHERE id = ?", id).rowsWritten > 0;
  }

  // ---- metrics: one row per tool call, kept 30 days ----
  private recorded = 0;
  async metricsRecord(agent: string, tool: string, ok: boolean, ms: number) {
    const now = Date.now();
    this.sql.exec("INSERT INTO calls VALUES(?, ?, ?, ?, ?)", now, agent, tool, ok ? 1 : 0, Math.max(0, Math.round(ms)));
    if (++this.recorded % 200 === 1) this.sql.exec("DELETE FROM calls WHERE at < ?", now - 30 * 86_400_000);
  }
  async metricsSummary(since: number): Promise<MetricsSummary> {
    const total = this.sql.exec("SELECT COUNT(*) AS calls, COALESCE(SUM(1 - ok), 0) AS errors FROM calls WHERE at >= ?", since).one() as Row;
    const group = (expr: string): CallStat[] => (this.sql.exec(`SELECT ${expr} AS name, COUNT(*) AS calls, SUM(1 - ok) AS errors,
      AVG(ms) AS avg FROM calls WHERE at >= ? GROUP BY name ORDER BY calls DESC LIMIT 50`, since).toArray() as Row[])
      .map((r) => ({ name: String(r.name), calls: Number(r.calls), errors: Number(r.errors), avgMs: Number(r.avg) }));
    return { since, calls: Number(total.calls), errors: Number(total.errors), byAgent: group("agent"),
      // The app is the tool's prefix, up to and including the first "_".
      byApp: group("substr(tool, 1, instr(tool, '_'))"), byTool: group("tool") };
  }
}
