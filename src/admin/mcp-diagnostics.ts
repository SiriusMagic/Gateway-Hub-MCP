import { REMOTE_APPS } from "../modules/remote/index.ts";

export const TRACE_ID = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/;

const APPS = new Set(["music_", ...REMOTE_APPS.map(({ prefix }) => prefix)]);
const STONE_TOOLS = new Set(["player_play_by_title", "player_state"]);
const STAGES = new Set(["conexion", "permiso_local", "token", "ejecucion", "respuesta"]);
const CODES = new Set([
  "APP_MISSING", "PROVIDER_UNAVAILABLE", "HUB_LOCAL_DISABLED", "LOCAL_DISABLED", "NO_PERMISSION",
  "NOT_SAME_DEVELOPER", "NOT_READY", "STONE_OFFLINE", "STONE_UNREACHABLE", "STONE_TOKEN", "UNAUTHORIZED",
  "STONE_NOT_LINKED", "APP_NOT_LINKED", "NO_HUB", "NETWORK", "RATE_LIMITED", "STONE_ERROR", "TOOL_UNAVAILABLE",
  "PLAYER_NOT_READY", "VOLUME_REJECTED", "PLAYBACK_FAILED", "PLAYBACK_UNCONFIRMED", "RESULT_UNCONFIRMED",
  "TRACK_NOT_FOUND", "AMBIGUOUS_TITLE", "INVALID_SELECTION", "INVALID_PARAM", "EQ_NOT_ACTIVE", "EQ_BATTERY_MODE", "OTHER",
]);

export interface McpReport { id: string; app: string; tool: string; stage: string; code: string; version: string; ms: number }
export interface McpFailureGroup { app: string; tool: string; stage: string; code: string; count: number; last: number }

export function mcpReport(body: Record<string, unknown>): McpReport | null {
  const { id, app, tool, stage, code, version, ms } = body;
  if (Object.keys(body).length !== 7 || typeof id !== "string" || !TRACE_ID.test(id) ||
      typeof app !== "string" || !APPS.has(app) || typeof tool !== "string" ||
      (app === "music_" ? !STONE_TOOLS.has(tool) : tool !== "ping") || typeof stage !== "string" || !STAGES.has(stage) ||
      typeof code !== "string" || !CODES.has(code) || typeof version !== "string" ||
      !/^\d{1,5}(?:\.\d{1,5}){1,3}$/.test(version) || typeof ms !== "number" || !Number.isInteger(ms) ||
      ms < 0 || ms > 120_000) return null;
  return { id, app, tool, stage, code, version, ms };
}
