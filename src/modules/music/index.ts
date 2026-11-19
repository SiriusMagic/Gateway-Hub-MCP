// Stone (music and video player on the phone) as a hub module: every Stone tool appears as music_<tool>.
// Stone is reached only through its phone's channel (Gateway Hub app → local bridge). Stone keeps checking
// the execution packages, each tool's risk and the on-screen confirmations itself.
import { type HubModule, type Tool, ToolError, type ToolResult, text } from "../types.ts";
import { providerFor } from "../../channel/registry.ts";
import { channelCall, channelRoute, channelTools } from "./channel.ts";

export const MUSIC = "music_";

const statusTool: Tool = { name: "music_status", description: "Whether Stone is connected to the hub through its phone's Gateway Hub app.",
  inputSchema: { type: "object", properties: {} }, annotations: { title: "Estado de Stone", readOnlyHint: true } };

export const musicModule: HubModule = {
  prefix: MUSIC,
  title: "Música (Stone)",
  summary: "Stone on the phone: play, search the library, queue, lyrics, tags, playlists, equalizer, video.",
  validatesOwnArgs: true, // Stone validates every argument itself
  androidPackage: "com.zirakusa.stone.kotlin.preview",
  async status(ctx) {
    const route = await channelRoute(ctx, MUSIC);
    if (route) return { linked: true, online: true, tools: route.packages.length, detail: "En línea por el canal de Gateway Hub." };
    const linked = !!await providerFor(ctx.store, MUSIC);
    return { linked, online: false, tools: 0, detail: linked ? "Vinculada; abre Gateway Hub en el teléfono de Stone." : "Sin vincular: Gateway Hub › Escáner › Stone › Vincular." };
  },
  async tools(ctx) {
    const route = await channelRoute(ctx, MUSIC);
    if (!route) return [statusTool];
    try { return [statusTool, ...await channelTools(ctx, route)]; } catch { return [statusTool]; }
  },
  async call(name, args, ctx): Promise<ToolResult> {
    const route = await channelRoute(ctx, MUSIC);
    if (name === "music_status") return route ? text("Stone is connected through Gateway Hub.", { linked: true, online: true })
      : text("Stone is not connected: the owner opens Gateway Hub on the phone with Stone.", { linked: false, online: false });
    if (!route) throw new ToolError("PROVIDER_OFFLINE", "Stone is not connected right now (open Gateway Hub on the phone).");
    return channelCall(ctx, route, name, args);
  },
};
