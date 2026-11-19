// The MCP environment as a whole, set by the owner (app or panel): connected, paused or disconnected.
// Paused keeps every link and setting but refuses every tool call; disconnected also forgets the apps the
// phones registered, so their session is gone until their Gateway Hub registers them again.
import { ToolError, type HubStoreApi } from "./modules/types.ts";

export type EnvironmentState = "active" | "paused" | "disconnected";
export const ENVIRONMENT_STATES: EnvironmentState[] = ["active", "paused", "disconnected"];
const KEY = "env.state";

export async function environmentState(store: HubStoreApi): Promise<EnvironmentState> {
  const saved = await store.getConfig(KEY);
  return ENVIRONMENT_STATES.includes(saved as EnvironmentState) ? saved as EnvironmentState : "active";
}

/** Changes the state; "disconnected" also forgets Stone's link and its cached tools. */
export async function setEnvironmentState(store: HubStoreApi, raw: string): Promise<EnvironmentState> {
  const state = raw as EnvironmentState;
  if (!ENVIRONMENT_STATES.includes(state)) throw new ToolError("INVALID_ARGUMENT", "Estado: active, paused o disconnected.");
  // Disconnecting forgets every phone's registered apps: nothing routes until their Gateway registers again.
  if (state === "disconnected") await store.setConfig("channel.providers", null);
  await store.setConfig(KEY, state === "active" ? null : state);
  return state;
}

/** Throws a coded error the AI can read when the owner paused or disconnected the environment. */
export async function requireActive(store: HubStoreApi): Promise<void> {
  const state = await environmentState(store);
  if (state === "paused") throw new ToolError("PAUSED", "The owner paused the MCP environment in Gateway Hub. Try again later.");
  if (state === "disconnected") throw new ToolError("DISCONNECTED", "The owner disconnected the MCP environment in Gateway Hub.");
}
