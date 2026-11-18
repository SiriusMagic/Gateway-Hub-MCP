// A small built-in app used only by the tests (the hub ships with Stone alone): one tool that reads,
// one that writes and one that deletes, so permissions, validation and metrics can be checked.
import { type HubModule, ToolError, text } from "../src/modules/types.ts";
import { musicModule } from "../src/modules/music/index.ts";

const key = { type: "string", minLength: 1, maxLength: 20 };
export const demoModule: HubModule = {
  prefix: "demo_",
  title: "Demo",
  summary: "Test app: save and read short values.",
  async tools() {
    return [
      { name: "demo_get", description: "Read a saved value.", annotations: { readOnlyHint: true },
        inputSchema: { type: "object", additionalProperties: false, required: ["key"], properties: { key } } },
      { name: "demo_save", description: "Save a value.",
        inputSchema: { type: "object", additionalProperties: false, required: ["key", "value"],
          properties: { key, value: { type: "string", maxLength: 100 } } } },
      { name: "demo_delete", description: "Delete a value.", annotations: { destructiveHint: true },
        inputSchema: { type: "object", additionalProperties: false, required: ["key"], properties: { key } } },
    ];
  },
  async call(name, args, ctx) {
    const slot = `demo.${args.key}`;
    if (name === "demo_save") { await ctx.store.setConfig(slot, String(args.value)); return text("saved", { key: args.key }); }
    const value = await ctx.store.getConfig(slot);
    if (value === null) throw new ToolError("NOT_FOUND", `No value '${args.key}'.`);
    if (name === "demo_get") return text(value, { key: args.key, value });
    if (name === "demo_delete") { await ctx.store.setConfig(slot, null); return text("deleted"); }
    throw new ToolError("UNKNOWN_TOOL", `Unknown demo tool '${name}'.`);
  },
};

export const TEST_MODULES: HubModule[] = [musicModule, demoModule];
