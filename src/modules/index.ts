// The list of apps the hub offers. To add one: create src/modules/<app>/index.ts following
// ./types.ts (see ../../MODULES.md) and add it here. Nothing else changes.
import { type HubModule } from "./types.ts";
import { musicModule } from "./music/index.ts";

export const modules: HubModule[] = [musicModule];
