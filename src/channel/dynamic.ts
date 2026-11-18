// Apps on other phones (or a second copy of an app) are routed without writing a module for them: every
// registered provider whose prefix no built-in module uses becomes a module that goes through its phone.
import type { HubContext, HubModule } from "../modules/types.ts";
import { ToolError } from "../modules/types.ts";
import { providers } from "./registry.ts";
import { channelCall, channelTools } from "../modules/music/channel.ts";

const overlaps = (a: string, b: string) => a.startsWith(b) || b.startsWith(a);

export async function channelModules(ctx: HubContext, builtIn: HubModule[]): Promise<HubModule[]> {
  const taken = builtIn.map((m) => m.prefix);
  const out: HubModule[] = [];
  for (const record of Object.values(await providers(ctx.store))) {
    if (taken.some((p) => overlaps(p, record.prefix))) continue;
    taken.push(record.prefix);
    const online = () => !!ctx.channel?.online(record.device);
    out.push({
      prefix: record.prefix, title: record.title, summary: `${record.title} on another phone (through Gateway Hub).`,
      androidPackage: record.app, validatesOwnArgs: true,
      async status() { return { linked: true, online: online(), tools: record.packages.length, detail: online() ? "En línea por el canal." : "Teléfono sin conectar." }; },
      async tools(c) { return online() ? channelTools(c, record) : []; },
      async call(name, args, c) {
        if (!online()) throw new ToolError("PROVIDER_OFFLINE", "That phone is not connected to the hub right now.");
        return channelCall(c, record, name, args);
      },
    });
  }
  return out;
}
