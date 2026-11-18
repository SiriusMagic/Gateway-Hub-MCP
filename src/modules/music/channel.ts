// Stone through the phone's channel (Gateway Hub app → local bridge), the path that replaces the relay.
// The hub picks the execution packages that could cover the call; Stone verifies them and decides.
import { type HubContext, type Tool, ToolError, type ToolResult } from "../types.ts";
import { type ProviderRecord, candidates, providerFor } from "../../channel/registry.ts";

const GRANT = "com.zirakusa.gatewayhub/grant";

/** Stone's registration when its phone is connected right now; null means "use the relay". */
export async function channelRoute(ctx: HubContext, prefix: string): Promise<ProviderRecord | null> {
  if (!ctx.channel) return null;
  const record = await providerFor(ctx.store, prefix);
  return record && ctx.channel.online(record.device) ? record : null;
}

async function send(ctx: HubContext, record: ProviderRecord, method: string, params: Record<string, unknown>, packages: string[], timeoutMs: number) {
  const message = { jsonrpc: "2.0", id: 1, method,
    params: { ...params, _meta: { [GRANT]: { agent: ctx.agent ?? "owner", packages, package: packages[0] } } } };
  const reply = await ctx.channel!.request(record.device, record.app, message, timeoutMs);
  if (reply?.error) {
    const text = String(reply.error.message ?? "");
    throw new ToolError(/^[A-Z][A-Z_]{2,40}/.exec(text)?.[0] ?? "PROVIDER_ERROR", text.slice(0, 300) || "The app refused the request.");
  }
  if (!reply?.result) throw new ToolError("PROVIDER_ERROR", "The app answered something that is not MCP.");
  return reply.result;
}

/** The tools this agent's packages open, with the prefix (live: Stone filters them by package). */
export async function channelTools(ctx: HubContext, record: ProviderRecord): Promise<Tool[]> {
  const packages = candidates(record, ctx.agent ?? "owner", null);
  if (!packages.length) return [];
  const result = await send(ctx, record, "tools/list", {}, packages, 6_000) as { tools?: Tool[] };
  return (result.tools ?? []).map((t) => ({ ...t, name: record.prefix + t.name, description: `[${record.title}] ${t.description}` }));
}

export async function channelCall(ctx: HubContext, record: ProviderRecord, name: string, args: Record<string, unknown>): Promise<ToolResult> {
  const tool = name.slice(record.prefix.length);
  const packages = candidates(record, ctx.agent ?? "owner", tool);
  if (!packages.length) throw new ToolError("NO_PACKAGE",
    `No active package lets this agent use '${name}'. The owner issues one in Gateway Hub › Escáner › ${record.title} › Paquetes.`);
  return await send(ctx, record, "tools/call", { name: tool, arguments: args }, packages, 75_000) as ToolResult;
}
