// Contract every hub module follows. A module owns one prefix ("music_") and every tool it lists
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

/** An AI client with its own link: which apps it may use, read-only or not, until when. */
export interface Agent {
  id: string; name: string; tokenHash: string;
  /** Module prefixes it may use ("music_"…), or ["*"] for every app. */
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
}

/** Hub-level storage: agents and metrics (not given to modules). */
export interface HubAdminStore {
  agentsCreate(agent: Agent): Promise<void>;
  agentsList(): Promise<Agent[]>;
  agentByHash(hash: string): Promise<Agent | null>;
  agentsSet(id: string, changes: Partial<Pick<Agent, "paused" | "lastUsed">>): Promise<void>;
  agentsDelete(id: string): Promise<boolean>;
  metricsRecord(agentId: string, tool: string, ok: boolean, ms: number): Promise<void>;
  metricsSummary(since: number): Promise<MetricsSummary>;
}

export interface HubEnv { HUB_TOKEN?: string }

export interface HubContext {
  store: HubStoreApi;
  env: HubEnv;
  /** The phones' channel (absent in tests that do not need it). */
  channel?: import("../channel/core.ts").Channel;
  /** Who is calling: an agent id or "owner" (set for /mcp requests). */
  agent?: string;
  /** Injected so tests can fake the network. */
  fetch: typeof fetch;
}

export interface HubModule {
  /** Lowercase letters/digits ending in "_": "music_", "weather_". */
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

export interface ModuleStatus { linked: boolean; online: boolean; tools: number; detail: string }

/** An expected failure with a stable code the LLM can read and act on. */
export class ToolError extends Error {
  code: string;
  constructor(code: string, message: string) { super(message); this.code = code; }
}

export function text(message: string, structured?: unknown): ToolResult {
  return structured === undefined ? { content: [{ type: "text", text: message }] }
    : { content: [{ type: "text", text: message }], structuredContent: structured };
}
