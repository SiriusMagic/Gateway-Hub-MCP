// MCP over JSON-RPC 2.0 (Streamable HTTP, JSON answers). Same small surface as Stone's server:
// initialize, ping, tools/list, tools/call (+ empty resources/prompts lists some clients ask for).
import { type HubContext, ToolError, type ToolResult } from "./modules/types.ts";
import { type Router } from "./router.ts";
import { type Principal, allows } from "./agents.ts";

const VERSIONS = ["2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05"];
const MAX_BATCH = 20;
type Message = { jsonrpc?: string; id?: unknown; method?: unknown; params?: Record<string, unknown> };
/** Who is calling and where each tool call is reported (metrics). */
export interface Caller { principal: Principal; record(tool: string, ok: boolean, ms: number): Promise<void> }

const rpcError = (id: unknown, code: number, message: string) => ({ jsonrpc: "2.0", id: id ?? null, error: { code, message } });

/** Returns the reply to send (null = nothing, for notifications). */
export async function handleRpc(body: string, router: Router, ctx: HubContext, version: string, caller: Caller): Promise<unknown> {
  let parsed: unknown;
  try { parsed = JSON.parse(body); } catch { return rpcError(null, -32700, "Invalid JSON"); }
  if (Array.isArray(parsed)) {
    if (parsed.length === 0 || parsed.length > MAX_BATCH) return rpcError(null, -32600, `A batch holds 1 to ${MAX_BATCH} messages`);
    const replies = (await Promise.all(parsed.map((m) => one(m as Message, router, ctx, version, caller)))).filter((r) => r !== null);
    return replies.length ? replies : null;
  }
  return one(parsed as Message, router, ctx, version, caller);
}

async function one(message: Message, router: Router, ctx: HubContext, version: string, caller: Caller): Promise<unknown> {
  if (typeof message !== "object" || message === null || typeof message.method !== "string")
    return rpcError(message?.id, -32600, "Invalid request");
  if (message.id === undefined || message.id === null) return null; // notification
  const params = message.params ?? {};
  const ok = (result: unknown) => ({ jsonrpc: "2.0", id: message.id, result });
  switch (message.method) {
    case "initialize":
      return ok({
        protocolVersion: VERSIONS.includes(params.protocolVersion as string) ? params.protocolVersion : VERSIONS[0],
        capabilities: { tools: { listChanged: false } },
        serverInfo: { name: "gateway-hub", title: "Gateway Hub", version },
        instructions: "One link for several apps. Every tool name starts with its app: " +
          router.modules.filter((m) => caller.principal.modules.includes("*") || caller.principal.modules.includes(m.prefix)).map((m) => `${m.prefix}* = ${m.title} (${m.summary})`).join("; ") +
          ". Errors carry a code in structuredContent.error.code." +
          (caller.principal.readOnly ? " This link is READ-ONLY: only tools that read are listed." : ""),
      });
    case "ping": return ok({});
    case "tools/list": return ok({ tools: (await router.listTools(ctx)).filter((t) => allows(caller.principal, t)) });
    case "tools/call": return ok(await call(params, router, ctx, caller));
    case "resources/list": return ok({ resources: [] });
    case "resources/templates/list": return ok({ resourceTemplates: [] });
    case "prompts/list": return ok({ prompts: [] });
    default: return rpcError(message.id, -32601, `Method not found: ${message.method}`);
  }
}

async function call(params: Record<string, unknown>, router: Router, ctx: HubContext, caller: Caller): Promise<ToolResult> {
  const name = params.name;
  const args = params.arguments ?? {};
  const started = Date.now();
  let result: ToolResult | undefined;
  try {
    if (typeof name !== "string") throw new ToolError("INVALID_REQUEST", "params.name must be the tool name.");
    if (typeof args !== "object" || args === null || Array.isArray(args)) throw new ToolError("INVALID_ARGUMENT", "arguments must be an object.");
    const tool = await router.describe(name, ctx);
    // Not shown to this agent = not callable by it (app not allowed, or a change on a read-only link).
    if (tool && !allows(caller.principal, tool)) throw new ToolError("FORBIDDEN",
      caller.principal.readOnly ? `This link is read-only; '${name}' changes something.` : `This link may not use '${name}'.`);
    result = await router.call(name, args as Record<string, unknown>, ctx);
    return result;
  } catch (error) {
    const known = error instanceof ToolError;
    // Unexpected failures are logged here and reported without internals.
    if (!known) console.error(JSON.stringify({ event: "tool_failed", tool: String(name), error: String(error) }));
    const code = known ? error.code : "INTERNAL";
    const message = known ? error.message : "The tool failed unexpectedly.";
    result = { content: [{ type: "text", text: `[${code}] ${message}` }], structuredContent: { ok: false, error: { code, message } }, isError: true };
    return result;
  } finally {
    await caller.record(typeof name === "string" ? name.slice(0, 64) : "?", result?.isError !== true, Date.now() - started).catch(() => {});
  }
}
