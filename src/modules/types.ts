import type { McpFailureGroup, McpReport } from "../admin/mcp-diagnostics.ts";

// Contract every hub module follows. A module owns one prefix ("calc_") and every tool it lists
// starts with it; the router (../router.ts) uses that prefix to send each call to its module.

export interface Tool {
  name: string;
  description: string;
  inputSchema: Record<string, unknown>;
  annotations?: { title?: string; readOnlyHint?: boolean; destructiveHint?: boolean };
}

export interface ToolResult {
  content: { type: "text"; text: string }[];
  structuredContent?: unknown;
  isError?: boolean;
}

export interface Note { id: string; title: string; body: string; created: number; updated: number }

/** An AI client with its own link: which apps it may use, read-only or not, until when. */
export interface Agent {
  id: string; name: string; tokenHash: string;
  /** Module prefixes it may use ("calc_"…), or ["*"] for every app. */
  modules: string[];
  readOnly: boolean; paused: boolean;
  /** 0 = never expires. */
  expiresAt: number; created: number; lastUsed: number;
}

export interface CallStat { name: string; calls: number; errors: number; avgMs: number }
export interface MetricsSummary { since: number; calls: number; errors: number; byAgent: CallStat[]; byApp: CallStat[]; byTool: CallStat[] }

/** Persistent storage the modules may use (a Durable Object in production, memory in tests). */
export interface HubStoreApi {
  getConfig(key: string): Promise<string | null>;
  setConfig(key: string, value: string | null): Promise<void>;
  notesCreate(title: string, body: string): Promise<Note>;
  notesList(query: string, limit: number): Promise<Note[]>;
  notesGet(id: string): Promise<Note | null>;
  notesUpdate(id: string, title: string | null, body: string | null): Promise<Note | null>;
  notesDelete(id: string): Promise<boolean>;
  notesCount(): Promise<number>;
}

/** Hub-level storage: agents and metrics (not given to modules). */
export interface HubAdminStore {
  agentsCreate(agent: Agent): Promise<void>;
  agentsList(): Promise<Agent[]>;
  agentByHash(hash: string): Promise<Agent | null>;
  agentsSet(id: string, changes: Partial<Pick<Agent, "paused" | "lastUsed">>): Promise<void>;
  agentsDelete(id: string): Promise<boolean>;
  metricsRecord(agentId: string, tool: string, ok: boolean, ms: number): Promise<void>;
  mcpReport(report: McpReport): Promise<void>;
  mcpFailures(): Promise<McpFailureGroup[]>;
  metricsSummary(since: number): Promise<MetricsSummary>;
}

export interface HubEnv { STONE_RELAY_HOST: string; HUB_TOKEN?: string }

export interface HubContext {
  traceId?: string;
  store: HubStoreApi;
  env: HubEnv;
  /** Injected so tests can fake the network. */
  fetch: typeof fetch;
}

export interface HubModule {
  /** Lowercase letters/digits ending in "_": "music_", "notes_". */
  prefix: string;
  title: string;
  /** One line for the LLM: what this module is for. */
  summary: string;
  /** Android package of the app behind this module, so the hub app's scanner can match it (e.g. Stone). */
  androidPackage?: string;
  /**
   * Read-only check of a remote app: linked to the hub, answering now. Never turns anything on.
   * Built-in modules leave it out (always ready).
   */
  status?(ctx: HubContext): Promise<ModuleStatus>;
  /** true when the module checks its own arguments (e.g. a remote app); otherwise the router does. */
  validatesOwnArgs?: boolean;
  tools(ctx: HubContext): Promise<Tool[]>;
  call(name: string, args: Record<string, unknown>, ctx: HubContext): Promise<ToolResult>;
}

/** What a remote app says about the token the hub uses (Stone: initialize › _meta), never the secret. */
export interface LinkedToken {
  name: string;
  /** "llm" or "hub": the app keeps the permissions (the hub is a bridge). "redirect": the hub manages them. */
  kind: string;
  usage: string;
  mode: string;
}

export interface ModuleStatus {
  linked: boolean; online: boolean; tools: number; detail: string;
  token?: LinkedToken;
  /** Who decides permissions for this app: the app itself ("app") or the hub ("hub", the Modo MCP that Arka turns on). */
  managedBy?: "app" | "hub";
}

/** An expected failure with a stable code the LLM can read and act on. */
export class ToolError extends Error {
  code: string;
  constructor(code: string, message: string) { super(message); this.code = code; }
}

export function text(message: string, structured?: unknown): ToolResult {
  return structured === undefined ? { content: [{ type: "text", text: message }] }
    : { content: [{ type: "text", text: message }], structuredContent: structured };
}
