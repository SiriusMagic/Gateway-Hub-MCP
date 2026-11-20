var __defProp = Object.defineProperty;
var __name = (target, value) => __defProp(target, "name", { value, configurable: true });

// src/auth.ts
async function digest(value) {
  return new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)));
}
__name(digest, "digest");
async function sameSecret(given, expected) {
  const [a, b] = await Promise.all([digest(given), digest(expected)]);
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
  return diff === 0;
}
__name(sameSecret, "sameSecret");
function givenToken(request, url) {
  const auth = request.headers.get("authorization") ?? "";
  const bearer = /^bearer\s+(.+)$/i.exec(auth)?.[1]?.trim();
  return bearer || request.headers.get("x-api-key")?.trim() || url.searchParams.get("token")?.trim() || null;
}
__name(givenToken, "givenToken");
var Limiter = class {
  static {
    __name(this, "Limiter");
  }
  hits = /* @__PURE__ */ new Map();
  max;
  windowMs;
  constructor(max, windowMs = 6e4) {
    this.max = max;
    this.windowMs = windowMs;
  }
  /** Counts one hit; true when [key] is over the limit. */
  hit(key2, now = Date.now()) {
    if (this.hits.size > 5e3) this.hits.clear();
    let entry = this.hits.get(key2);
    if (!entry || now - entry.since > this.windowMs) {
      entry = { since: now, count: 0 };
      this.hits.set(key2, entry);
    }
    return ++entry.count > this.max;
  }
  blocked(key2, now = Date.now()) {
    const entry = this.hits.get(key2);
    return !!entry && now - entry.since <= this.windowMs && entry.count >= this.max;
  }
};

// src/agents.ts
var OWNER = { id: "owner", name: "Propietario", modules: ["*"], readOnly: false };
var MAX_AGENTS = 50;
async function sha256Hex(value) {
  const digest2 = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return [...new Uint8Array(digest2)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(sha256Hex, "sha256Hex");
function newSecret(prefix, space = null) {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  const random = btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `${prefix}_${space ? `s${space}_` : ""}${random}`;
}
__name(newSecret, "newSecret");
function newId() {
  return [...crypto.getRandomValues(new Uint8Array(6))].map((b) => b.toString(16).padStart(2, "0")).join("");
}
__name(newId, "newId");
async function principalFor(token, isOwner, store, now = Date.now()) {
  if (await isOwner(token)) return OWNER;
  if (!token.startsWith("gw_")) return null;
  const agent = await store.agentByHash(await sha256Hex(token));
  if (!agent || agent.paused || agent.expiresAt && agent.expiresAt <= now) return null;
  if (now - agent.lastUsed > 6e4) await store.agentsSet(agent.id, { lastUsed: now });
  return { id: agent.id, name: agent.name, modules: agent.modules, readOnly: agent.readOnly };
}
__name(principalFor, "principalFor");
var allowsModule = /* @__PURE__ */ __name((p, tool) => p.modules.includes("*") || p.modules.some((m) => tool.startsWith(m)), "allowsModule");
var allows = /* @__PURE__ */ __name((p, tool) => allowsModule(p, tool.name) && (!p.readOnly || tool.annotations?.readOnlyHint === true), "allows");
function agentStatus(agent, now = Date.now()) {
  return agent.paused ? "paused" : agent.expiresAt && agent.expiresAt <= now ? "expired" : "active";
}
__name(agentStatus, "agentStatus");

// src/modules/types.ts
var ToolError = class extends Error {
  static {
    __name(this, "ToolError");
  }
  code;
  constructor(code, message) {
    super(message);
    this.code = code;
  }
};
function text(message, structured) {
  return structured === void 0 ? { content: [{ type: "text", text: message }] } : { content: [{ type: "text", text: message }], structuredContent: structured };
}
__name(text, "text");

// src/environment.ts
var ENVIRONMENT_STATES = ["active", "paused", "disconnected"];
var KEY = "env.state";
async function environmentState(store) {
  const saved = await store.getConfig(KEY);
  return ENVIRONMENT_STATES.includes(saved) ? saved : "active";
}
__name(environmentState, "environmentState");
async function setEnvironmentState(store, raw) {
  const state = raw;
  if (!ENVIRONMENT_STATES.includes(state)) throw new ToolError("INVALID_ARGUMENT", "Estado: active, paused o disconnected.");
  if (state === "disconnected") await store.setConfig("channel.providers", null);
  await store.setConfig(KEY, state === "active" ? null : state);
  return state;
}
__name(setEnvironmentState, "setEnvironmentState");
async function requireActive(store) {
  const state = await environmentState(store);
  if (state === "paused") throw new ToolError("PAUSED", "The owner paused the MCP environment in Gateway Hub. Try again later.");
  if (state === "disconnected") throw new ToolError("DISCONNECTED", "The owner disconnected the MCP environment in Gateway Hub.");
}
__name(requireActive, "requireActive");

// src/mcp.ts
var VERSIONS = ["2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05"];
var MAX_BATCH = 20;
var rpcError = /* @__PURE__ */ __name((id, code, message) => ({ jsonrpc: "2.0", id: id ?? null, error: { code, message } }), "rpcError");
async function handleRpc(body, router, ctx, version, caller) {
  let parsed;
  try {
    parsed = JSON.parse(body);
  } catch {
    return rpcError(null, -32700, "Invalid JSON");
  }
  if (Array.isArray(parsed)) {
    if (parsed.length === 0 || parsed.length > MAX_BATCH) return rpcError(null, -32600, `A batch holds 1 to ${MAX_BATCH} messages`);
    const replies = (await Promise.all(parsed.map((m) => one(m, router, ctx, version, caller)))).filter((r) => r !== null);
    return replies.length ? replies : null;
  }
  return one(parsed, router, ctx, version, caller);
}
__name(handleRpc, "handleRpc");
async function one(message, router, ctx, version, caller) {
  if (typeof message !== "object" || message === null || typeof message.method !== "string")
    return rpcError(message?.id, -32600, "Invalid request");
  if (message.id === void 0 || message.id === null) return null;
  const params = message.params ?? {};
  const ok = /* @__PURE__ */ __name((result) => ({ jsonrpc: "2.0", id: message.id, result }), "ok");
  switch (message.method) {
    case "initialize":
      return ok({
        protocolVersion: VERSIONS.includes(params.protocolVersion) ? params.protocolVersion : VERSIONS[0],
        capabilities: { tools: { listChanged: false } },
        serverInfo: { name: "gateway-hub", title: "Gateway Hub", version },
        instructions: "One link for several apps. Every tool name starts with its app: " + router.modules.filter((m) => caller.principal.modules.includes("*") || caller.principal.modules.includes(m.prefix)).map((m) => `${m.prefix}* = ${m.title} (${m.summary})`).join("; ") + ". Errors carry a code in structuredContent.error.code." + (caller.principal.readOnly ? " This link is READ-ONLY: only tools that read are listed." : "")
      });
    case "ping":
      return ok({});
    case "tools/list":
      return ok({ tools: (await router.listTools(ctx)).filter((t) => allows(caller.principal, t)) });
    case "tools/call":
      return ok(await call(params, router, ctx, caller));
    case "resources/list":
      return ok({ resources: [] });
    case "resources/templates/list":
      return ok({ resourceTemplates: [] });
    case "prompts/list":
      return ok({ prompts: [] });
    default:
      return rpcError(message.id, -32601, `Method not found: ${message.method}`);
  }
}
__name(one, "one");
async function call(params, router, ctx, caller) {
  const name = params.name;
  const args = params.arguments ?? {};
  const started = Date.now();
  let result;
  try {
    if (typeof name !== "string") throw new ToolError("INVALID_REQUEST", "params.name must be the tool name.");
    if (typeof args !== "object" || args === null || Array.isArray(args)) throw new ToolError("INVALID_ARGUMENT", "arguments must be an object.");
    await requireActive(ctx.store);
    const tool = await router.describe(name, ctx);
    if (tool && !allows(caller.principal, tool)) throw new ToolError(
      "FORBIDDEN",
      caller.principal.readOnly ? `This link is read-only; '${name}' changes something.` : `This link may not use '${name}'.`
    );
    result = await router.call(name, args, ctx);
    return result;
  } catch (error) {
    const known = error instanceof ToolError;
    if (!known) console.error(JSON.stringify({ event: "tool_failed", tool: String(name), error: String(error) }));
    const code = known ? error.code : "INTERNAL";
    const message = known ? error.message : "The tool failed unexpectedly.";
    result = { content: [{ type: "text", text: `[${code}] ${message}` }], structuredContent: { ok: false, error: { code, message } }, isError: true };
    return result;
  } finally {
    await caller.record(typeof name === "string" ? name.slice(0, 64) : "?", result?.isError !== true, Date.now() - started).catch(() => {
    });
  }
}
__name(call, "call");

// src/validate.ts
function validateArgs(schema, args) {
  const properties = schema.properties ?? {};
  for (const key2 of schema.required ?? []) {
    if (args[key2] === void 0 || args[key2] === null) throw new ToolError("INVALID_ARGUMENT", `'${key2}' is required.`);
  }
  for (const [key2, value] of Object.entries(args)) {
    const rule = properties[key2];
    if (!rule) {
      if (schema.additionalProperties === false) throw new ToolError("INVALID_ARGUMENT", `Unknown argument '${key2}'.`);
      continue;
    }
    check(key2, rule, value);
  }
}
__name(validateArgs, "validateArgs");
function check(key2, rule, value) {
  const fail = /* @__PURE__ */ __name((why) => {
    throw new ToolError("INVALID_ARGUMENT", `'${key2}' ${why}.`);
  }, "fail");
  switch (rule.type) {
    case "string":
      if (typeof value !== "string") fail("must be text");
      if (rule.maxLength !== void 0 && value.length > rule.maxLength) fail(`is longer than ${rule.maxLength} characters`);
      if (rule.minLength !== void 0 && value.length < rule.minLength) fail(`needs at least ${rule.minLength} characters`);
      break;
    case "number":
    case "integer":
      if (typeof value !== "number" || !Number.isFinite(value)) fail("must be a number");
      if (rule.type === "integer" && !Number.isInteger(value)) fail("must be a whole number");
      if (rule.minimum !== void 0 && value < rule.minimum) fail(`must be at least ${rule.minimum}`);
      if (rule.maximum !== void 0 && value > rule.maximum) fail(`must be at most ${rule.maximum}`);
      break;
    case "boolean":
      if (typeof value !== "boolean") fail("must be true or false");
      break;
  }
  if (rule.enum && !rule.enum.includes(value)) fail(`must be one of ${rule.enum.join(", ")}`);
}
__name(check, "check");

// src/router.ts
var PREFIX = /^[a-z][a-z0-9]{0,15}_$/;
var TOOL_NAME = /^[a-z][a-z0-9_]{1,63}$/;
function createRouter(modules2) {
  for (const m of modules2) {
    if (!PREFIX.test(m.prefix)) throw new Error(`Invalid module prefix: ${m.prefix}`);
    const clash = modules2.find((o) => o !== m && (o.prefix.startsWith(m.prefix) || m.prefix.startsWith(o.prefix)));
    if (clash) throw new Error(`Prefixes ${m.prefix} and ${clash.prefix} overlap`);
  }
  const owner = /* @__PURE__ */ __name((name) => modules2.find((m) => name.startsWith(m.prefix)), "owner");
  async function toolsOf(m, ctx) {
    try {
      return (await m.tools(ctx)).filter((t) => t.name.startsWith(m.prefix) && TOOL_NAME.test(t.name));
    } catch (error) {
      console.warn(JSON.stringify({ event: "module_tools_failed", module: m.prefix, error: String(error) }));
      return [];
    }
  }
  __name(toolsOf, "toolsOf");
  return {
    modules: modules2,
    async listTools(ctx) {
      const lists = await Promise.all(modules2.map((m) => toolsOf(m, ctx)));
      return lists.flat();
    },
    async describe(name, ctx) {
      const module = owner(name);
      return module ? (await toolsOf(module, ctx)).find((t) => t.name === name) : void 0;
    },
    async call(name, args, ctx) {
      const module = owner(name);
      if (!module) throw new ToolError(
        "UNKNOWN_TOOL",
        `No module handles '${name}'. Modules: ${modules2.map((m) => m.prefix + "*").join(", ")}.`
      );
      if (!module.validatesOwnArgs) {
        const tool = (await toolsOf(module, ctx)).find((t) => t.name === name);
        if (!tool) throw new ToolError("UNKNOWN_TOOL", `Unknown tool '${name}'. Call tools/list.`);
        validateArgs(tool.inputSchema, args);
      }
      return module.call(name, args, ctx);
    }
  };
}
__name(createRouter, "createRouter");

// src/channel/registry.ts
var KEY2 = "channel.providers";
var PREFIX2 = /^[a-z][a-z0-9]{0,15}_$/;
var APP = /^[a-zA-Z][a-zA-Z0-9_.]{2,100}$/;
var ID = /^pkg_[A-Za-z0-9_-]{8,64}$/;
var MAX_PACKAGES = 64;
async function providers(store) {
  try {
    return JSON.parse(await store.getConfig(KEY2) ?? "{}");
  } catch {
    return {};
  }
}
__name(providers, "providers");
async function providerFor(store, prefix) {
  return (await providers(store))[prefix] ?? null;
}
__name(providerFor, "providerFor");
var strings = /* @__PURE__ */ __name((v, max) => Array.isArray(v) ? v.filter((x) => typeof x === "string").map((x) => x.slice(0, 80)).slice(0, max) : [], "strings");
async function register(store, device, message) {
  const all = await providers(store);
  for (const [prefix, record] of Object.entries(all)) if (record.device === device) delete all[prefix];
  const list = Array.isArray(message.providers) ? message.providers.slice(0, 10) : [];
  for (const raw of list) {
    const prefix = String(raw?.prefix ?? ""), app = String(raw?.app ?? "");
    if (!PREFIX2.test(prefix) || !APP.test(app)) continue;
    const base = prefix.slice(0, -1).slice(0, 14);
    const free = [prefix, ...[2, 3, 4, 5, 6, 7, 8, 9].map((n) => `${base}${n}_`)].find((p) => !all[p] || all[p].device === device);
    if (!free) continue;
    const packages = (Array.isArray(raw.packages) ? raw.packages : []).slice(0, MAX_PACKAGES).map((p) => ({
      id: String(p?.id ?? ""),
      agent: String(p?.agent ?? ""),
      acts: strings(p?.acts, 5),
      tools: strings(p?.tools, 100),
      exp: Number(p?.exp) || 0,
      status: String(p?.status ?? "")
    })).filter((p) => ID.test(p.id) && /^(\*|[A-Za-z0-9_-]{1,40})$/.test(p.agent));
    all[free] = { device, app, prefix: free, title: String(raw.title ?? app).slice(0, 60), packages, seen: Date.now() };
  }
  await store.setConfig(KEY2, JSON.stringify(all));
}
__name(register, "register");
function candidates(record, agent, tool, now = Date.now()) {
  const covers = /* @__PURE__ */ __name((p) => tool === null || p.tools.length === 0 || p.tools.some((t) => t.endsWith("*") ? tool.startsWith(t.slice(0, -1)) : t === tool), "covers");
  return record.packages.filter((p) => p.status === "active" && p.exp * 1e3 > now && (p.agent === agent || p.agent === "*") && covers(p)).sort((a, b) => (a.agent === "*" ? 1 : 0) - (b.agent === "*" ? 1 : 0)).slice(0, 8).map((p) => p.id);
}
__name(candidates, "candidates");

// src/modules/music/channel.ts
var GRANT = "com.zirakusa.gatewayhub/grant";
async function channelRoute(ctx, prefix) {
  if (!ctx.channel) return null;
  const record = await providerFor(ctx.store, prefix);
  return record && ctx.channel.online(record.device) ? record : null;
}
__name(channelRoute, "channelRoute");
async function send(ctx, record, method, params, packages, timeoutMs) {
  const message = {
    jsonrpc: "2.0",
    id: 1,
    method,
    params: { ...params, _meta: { [GRANT]: { agent: ctx.agent ?? "owner", packages, package: packages[0] } } }
  };
  const reply = await ctx.channel.request(record.device, record.app, message, timeoutMs);
  if (reply?.error) {
    const text2 = String(reply.error.message ?? "");
    throw new ToolError(/^[A-Z][A-Z_]{2,40}/.exec(text2)?.[0] ?? "PROVIDER_ERROR", text2.slice(0, 300) || "The app refused the request.");
  }
  if (!reply?.result) throw new ToolError("PROVIDER_ERROR", "The app answered something that is not MCP.");
  return reply.result;
}
__name(send, "send");
async function channelTools(ctx, record) {
  const packages = candidates(record, ctx.agent ?? "owner", null);
  if (!packages.length) return [];
  const result = await send(ctx, record, "tools/list", {}, packages, 6e3);
  return (result.tools ?? []).map((t) => ({ ...t, name: record.prefix + t.name, description: `[${record.title}] ${t.description}` }));
}
__name(channelTools, "channelTools");
async function channelCall(ctx, record, name, args) {
  const tool = name.slice(record.prefix.length);
  const packages = candidates(record, ctx.agent ?? "owner", tool);
  if (!packages.length) throw new ToolError(
    "NO_PACKAGE",
    `No active package lets this agent use '${name}'. The owner issues one in Gateway Hub \u203A Esc\xE1ner \u203A ${record.title} \u203A Paquetes.`
  );
  return await send(ctx, record, "tools/call", { name: tool, arguments: args }, packages, 75e3);
}
__name(channelCall, "channelCall");

// src/modules/music/index.ts
var MUSIC = "music_";
var statusTool = {
  name: "music_status",
  description: "Whether Stone is connected to the hub through its phone's Gateway Hub app.",
  inputSchema: { type: "object", properties: {} },
  annotations: { title: "Estado de Stone", readOnlyHint: true }
};
var musicModule = {
  prefix: MUSIC,
  title: "M\xFAsica (Stone)",
  summary: "Stone on the phone: play, search the library, queue, lyrics, tags, playlists, equalizer, video.",
  validatesOwnArgs: true,
  // Stone validates every argument itself
  androidPackage: "com.zirakusa.stone.kotlin.preview",
  async status(ctx) {
    const route = await channelRoute(ctx, MUSIC);
    if (route) return { linked: true, online: true, tools: route.packages.length, detail: "En l\xEDnea por el canal de Gateway Hub." };
    const linked = !!await providerFor(ctx.store, MUSIC);
    return { linked, online: false, tools: 0, detail: linked ? "Vinculada; abre Gateway Hub en el tel\xE9fono de Stone." : "Sin vincular: Gateway Hub \u203A Esc\xE1ner \u203A Stone \u203A Vincular." };
  },
  async tools(ctx) {
    const route = await channelRoute(ctx, MUSIC);
    if (!route) return [statusTool];
    try {
      return [statusTool, ...await channelTools(ctx, route)];
    } catch {
      return [statusTool];
    }
  },
  async call(name, args, ctx) {
    const route = await channelRoute(ctx, MUSIC);
    if (name === "music_status") return route ? text("Stone is connected through Gateway Hub.", { linked: true, online: true }) : text("Stone is not connected: the owner opens Gateway Hub on the phone with Stone.", { linked: false, online: false });
    if (!route) throw new ToolError("PROVIDER_OFFLINE", "Stone is not connected right now (open Gateway Hub on the phone).");
    return channelCall(ctx, route, name, args);
  }
};

// src/modules/index.ts
var modules = [musicModule];

// src/admin/layout.ts
var ADMIN_HEADERS = {
  "Content-Type": "text/html; charset=utf-8",
  "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'"
};
function esc(value) {
  return String(value).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}
__name(esc, "esc");
function form(token, action, inner, extra = "") {
  return `<form method="post" action="/admin" ${extra}><input type="hidden" name="token" value="${esc(token)}"><input type="hidden" name="action" value="${esc(action)}">${inner}</form>`;
}
__name(form, "form");
function page(body) {
  return `<!doctype html><html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><meta name="referrer" content="no-referrer">
<title>Gateway Hub</title><style>
:root{color-scheme:light dark;--bg:#111015;--card:#1d1b24;--text:#f3f0fa;--muted:#a49eb3;--line:#3a3646;--gold:#e8b931;--bad:#ff7a7a;--good:#7ddc9a}
@media (prefers-color-scheme:light){:root{--bg:#f6f4fa;--card:#fff;--text:#1b1724;--muted:#5d566b;--line:#ddd8e6}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:16px/1.5 system-ui,sans-serif}
main{max-width:560px;margin:0 auto;padding:20px 16px 40px}h1{font-size:22px;margin:0}h2{font-size:17px;margin:0 0 8px}
.card{background:var(--card);border-radius:16px;padding:16px;margin:14px 0}.row{display:flex;gap:8px;align-items:center;flex-wrap:wrap}
.muted,small{color:var(--muted)}.good{color:var(--good)}.bad{color:var(--bad)}
label{display:block;font-weight:600;margin:10px 0 4px}label.inline{display:inline-flex;gap:6px;font-weight:400;margin:4px 12px 4px 0}
input[type=text],input[type=password],input[type=url],input[type=number]{width:100%;padding:11px;border-radius:10px;border:1px solid var(--line);background:transparent;color:inherit;font-size:15px}
button{padding:10px 14px;border:0;border-radius:10px;background:var(--gold);color:#1b1724;font-weight:700;font-size:15px;margin-top:10px}
button.ghost{background:transparent;color:var(--text);border:1px solid var(--line);margin-top:0;padding:6px 10px;font-size:13px}
.agent{border-top:1px solid var(--line);padding:10px 0}.agent:first-child{border-top:0}
code,.secret{display:block;word-break:break-all;background:var(--bg);border-radius:10px;padding:10px;font-size:13px;margin-top:6px}
table{width:100%;border-collapse:collapse;font-size:14px}td,th{text-align:left;padding:4px 2px;border-bottom:1px solid var(--line)}td.n,th.n{text-align:right}
</style></head><body><main>${body}</main></body></html>`;
}
__name(page, "page");

// src/admin/views.ts
var loginPage = /* @__PURE__ */ __name((message = "") => page(`<h1>Gateway Hub</h1><p class="muted">Un solo enlace MCP para varias apps.</p>
${message ? `<p class="bad">${esc(message)}</p>` : ""}
<div class="card"><form method="post" action="/admin" autocomplete="off"><input type="hidden" name="action" value="login">
<label for="t">Acceso (direcci\xF3n + token) o solo el token del propietario</label><input id="t" name="token" type="password" required>
<button>Entrar</button></form></div>`), "loginPage");
var date = /* @__PURE__ */ __name((ms) => ms ? new Date(ms).toISOString().slice(0, 16).replace("T", " ") + " UTC" : "nunca", "date");
function appsCard(d) {
  const rows = d.modules.map((m) => `<li><b>${esc(m.title)}</b> <small>${esc(m.prefix)}*</small>${m.prefix === "music_" ? d.stoneLinked ? ' <span class="good">vinculado</span>' : ' <span class="bad">sin vincular</span>' : ' <span class="good">listo</span>'}</li>`).join("");
  return `<div class="card"><h2>Apps</h2><ul>${rows}</ul><small>Stone y las dem\xE1s apps se vinculan desde la app Gateway Hub del tel\xE9fono (Esc\xE1ner \u203A Vincular).</small></div>`;
}
__name(appsCard, "appsCard");
var ENV_LABEL = {
  active: '<span class="good">conectado</span>',
  paused: '<span class="bad">en pausa</span>',
  disconnected: '<span class="bad">desconectado</span>'
};
function environmentCard(d) {
  const button = /* @__PURE__ */ __name((state, label) => d.environment === state ? "" : form(d.token, "environment", `<input type="hidden" name="state" value="${state}"><button class="ghost">${label}</button>`), "button");
  return `<div class="card"><h2>Entorno MCP</h2><p>Estado: ${ENV_LABEL[d.environment]}</p>
<small>Pausar conserva enlaces y ajustes; Desconectar adem\xE1s desvincula Stone. En ambos casos no se ejecuta ninguna herramienta.</small>
<div class="row">${button("active", "Conectar")}${button("paused", "Pausar")}${button("disconnected", "Desconectar del entorno")}</div></div>`;
}
__name(environmentCard, "environmentCard");
function agentRow(d, a) {
  const status = agentStatus(a);
  const label = { active: '<span class="good">activo</span>', paused: '<span class="bad">pausado</span>', expired: '<span class="bad">caducado</span>' }[status];
  const toggle = form(d.token, a.paused ? "agent_resume" : "agent_pause", `<input type="hidden" name="id" value="${esc(a.id)}"><button class="ghost">${a.paused ? "Reanudar" : "Pausar"}</button>`);
  const revoke = form(d.token, "agent_revoke", `<input type="hidden" name="id" value="${esc(a.id)}"><button class="ghost">Revocar</button>`);
  return `<div class="agent"><b>${esc(a.name)}</b> ${label}<br><small>${a.modules.includes("*") ? "todas las apps" : esc(a.modules.join(" "))} \xB7
${a.readOnly ? "solo lectura" : "lectura y escritura"} \xB7 caduca: ${a.expiresAt ? esc(date(a.expiresAt)) : "nunca"} \xB7 \xFAltimo uso: ${esc(date(a.lastUsed))}</small>
<div class="row">${toggle}${revoke}</div></div>`;
}
__name(agentRow, "agentRow");
function agentsCard(d) {
  const created = d.newLink ? `<div class="card"><h2 class="good">Enlace de \xAB${esc(d.newLink.name)}\xBB</h2>
<small>C\xF3pialo ahora: no se vuelve a mostrar. P\xE9galo en Claude o ChatGPT como conector personalizado.</small>
<div class="secret">${esc(d.newLink.url)}</div><small>O con cabecera: Authorization: Bearer</small><div class="secret">${esc(d.newLink.token)}</div></div>` : "";
  const apps = d.modules.map((m) => `<label class="inline"><input type="checkbox" name="apps" value="${esc(m.prefix)}" checked>${esc(m.title)}</label>`).join("");
  const create = form(d.token, "agent_create", `<label for="n">Nombre</label><input id="n" name="name" type="text" maxlength="40" placeholder="Claude, ChatGPT\u2026" required>
<label>Apps</label>${apps}<label class="inline"><input type="checkbox" name="readonly" value="1">Solo lectura</label>
<label for="days">Caduca en d\xEDas (0 = nunca)</label><input id="days" name="days" type="number" min="0" max="365" value="90"><button>Crear enlace</button>`, 'autocomplete="off"');
  const list = d.agents.length ? d.agents.map((a) => agentRow(d, a)).join("") : '<p class="muted">A\xFAn no hay agentes.</p>';
  return `${created}<div class="card"><h2>Agentes (un enlace por IA)</h2>${list}</div><div class="card"><h2>Nuevo enlace</h2>${create}</div>`;
}
__name(agentsCard, "agentsCard");
var table = /* @__PURE__ */ __name((title, rows) => rows.length ? `<h3>${esc(title)}</h3><table><tr><th>Nombre</th><th class="n">Llamadas</th><th class="n">Errores</th><th class="n">ms</th></tr>${rows.map((r) => `<tr><td>${esc(r.name)}</td><td class="n">${r.calls}</td><td class="n">${r.errors}</td><td class="n">${Math.round(r.avgMs)}</td></tr>`).join("")}</table>` : "", "table");
function metricsCard(d) {
  const names = new Map(d.agents.map((a) => [a.id, a.name]));
  const byAgent = d.week.byAgent.map((r) => ({ ...r, name: r.name === "owner" ? "Propietario" : names.get(r.name) ?? `(revocado ${r.name})` }));
  return `<div class="card"><h2>M\xE9tricas</h2><p>24 h: <b>${d.day.calls}</b> llamadas, <b>${d.day.errors}</b> con error \xB7 7 d\xEDas: <b>${d.week.calls}</b> / <b>${d.week.errors}</b></p>
${table("Por agente (7 d\xEDas)", byAgent)}${table("Por app (7 d\xEDas)", d.week.byApp)}${table("Herramientas m\xE1s usadas (7 d\xEDas)", d.week.byTool.slice(0, 10))}</div>`;
}
__name(metricsCard, "metricsCard");
function panelPage(d) {
  const notice = d.notice ? `<p class="${d.notice.error ? "bad" : "good"}">${esc(d.notice.text)}</p>` : "";
  return page(`<h1>Gateway Hub</h1><p class="muted">${esc(d.origin)}/mcp</p>${notice}${environmentCard(d)}${agentsCard(d)}${appsCard(d)}${metricsCard(d)}
${form(d.token, "login", `<button class="ghost">Actualizar</button>`)}`);
}
__name(panelPage, "panelPage");

// src/channel/devices.ts
var MAX_DEVICES = 10;
var KEY3 = "channel.devices";
async function listDevices(store) {
  try {
    return JSON.parse(await store.getConfig(KEY3) ?? "[]");
  } catch {
    return [];
  }
}
__name(listDevices, "listDevices");
async function createDevice(store, rawName) {
  const name = String(rawName ?? "").trim().slice(0, 40) || "Tel\xE9fono";
  const devices = await listDevices(store);
  if (devices.length >= MAX_DEVICES) throw new ToolError("LIMIT", `M\xE1ximo ${MAX_DEVICES} tel\xE9fonos.`);
  const secret = newSecret("dev", await store.getConfig("space.id"));
  const device = { id: newId(), name, hash: await sha256Hex(secret), created: Date.now() };
  await store.setConfig(KEY3, JSON.stringify([...devices, device]));
  return { device: { id: device.id, name, created: device.created }, secret };
}
__name(createDevice, "createDevice");
async function deleteDevice(store, id) {
  const devices = await listDevices(store);
  if (!devices.some((d) => d.id === id)) return false;
  await store.setConfig(KEY3, JSON.stringify(devices.filter((d) => d.id !== id)));
  return true;
}
__name(deleteDevice, "deleteDevice");
async function deviceFor(store, secret) {
  if (!secret.startsWith("dev_")) return null;
  const hash = await sha256Hex(secret);
  for (const d of await listDevices(store)) if (await sameSecret(hash, d.hash)) return d;
  return null;
}
__name(deviceFor, "deviceFor");

// src/admin/service.ts
var DAY = 864e5;
var publicAgent = /* @__PURE__ */ __name((a) => ({
  id: a.id,
  name: a.name,
  modules: a.modules,
  readOnly: a.readOnly,
  paused: a.paused,
  status: agentStatus(a),
  expiresAt: a.expiresAt,
  created: a.created,
  lastUsed: a.lastUsed
}), "publicAgent");
async function overview(store, ctx, modules2, version) {
  const now = Date.now();
  return {
    version,
    modules: modules2.map((m) => ({ prefix: m.prefix, title: m.title, summary: m.summary, androidPackage: m.androidPackage ?? null })),
    stoneLinked: !!(await providers(store))["music_"],
    environment: await environmentState(store),
    devices: await devicesView(store, ctx),
    agents: (await store.agentsList()).map(publicAgent),
    metrics: { day: await store.metricsSummary(now - DAY), week: await store.metricsSummary(now - 7 * DAY) }
  };
}
__name(overview, "overview");
async function scan(ctx, modules2) {
  const results = await Promise.all(modules2.map(async (m) => {
    const status = m.status ? await m.status(ctx).catch((e) => ({ linked: false, online: false, tools: 0, detail: String(e) })) : { linked: true, online: true, tools: (await m.tools(ctx)).length, detail: "Integrada en el hub." };
    return { prefix: m.prefix, title: m.title, androidPackage: m.androidPackage ?? null, remote: !!m.status, ...status };
  }));
  return { scannedAt: Date.now(), apps: results };
}
__name(scan, "scan");
async function createAgent(store, modules2, origin, input) {
  const name = String(input.name ?? "").trim().slice(0, 40);
  if (!name) throw new ToolError("INVALID_ARGUMENT", "Ponle un nombre al agente.");
  if ((await store.agentsList()).length >= MAX_AGENTS) throw new ToolError("LIMIT", `M\xE1ximo ${MAX_AGENTS} agentes.`);
  const known = new Set(modules2.map((m) => m.prefix));
  const apps = [...new Set((Array.isArray(input.apps) ? input.apps : []).map(String))].filter((p) => known.has(p));
  if (!apps.length) throw new ToolError("INVALID_ARGUMENT", "Elige al menos una app.");
  const days = Math.min(365, Math.max(0, Math.floor(Number(input.days) || 0)));
  const secret = newSecret("gw", await store.getConfig("space.id")), now = Date.now();
  const agent = {
    id: newId(),
    name,
    tokenHash: await sha256Hex(secret),
    modules: apps.length === known.size ? ["*"] : apps,
    readOnly: input.readOnly === true,
    paused: false,
    expiresAt: days ? now + days * DAY : 0,
    created: now,
    lastUsed: 0
  };
  await store.agentsCreate(agent);
  return { agent: publicAgent(agent), token: secret, url: `${origin}/mcp?token=${secret}` };
}
__name(createAgent, "createAgent");
async function setPaused(store, id, paused) {
  if (!(await store.agentsList()).some((a) => a.id === id)) return false;
  await store.agentsSet(id, { paused });
  return true;
}
__name(setPaused, "setPaused");
var ENVIRONMENT_NOTES = {
  active: "Entorno MCP conectado: las apps vuelven a ejecutar herramientas.",
  paused: "Entorno MCP en pausa: se conservan enlaces y ajustes, pero no se ejecuta nada.",
  disconnected: "Entorno MCP desconectado: Stone se desvincul\xF3 y no se ejecuta nada."
};
async function changeEnvironment(store, raw) {
  const state = await setEnvironmentState(store, raw);
  return { state, message: ENVIRONMENT_NOTES[state] };
}
__name(changeEnvironment, "changeEnvironment");
async function devicesView(store, ctx) {
  const served = Object.values(await providers(store));
  return (await listDevices(store)).map((d) => ({
    id: d.id,
    name: d.name,
    created: d.created,
    online: ctx.channel?.online(d.id) ?? false,
    providers: served.filter((p) => p.device === d.id).map((p) => ({ prefix: p.prefix, app: p.app, title: p.title, packages: p.packages.length }))
  }));
}
__name(devicesView, "devicesView");

// src/admin/actions.ts
function ownerTokenFrom(pasted) {
  return /hub_[A-Za-z0-9_-]{20,}/.exec(pasted)?.[0] ?? pasted.trim();
}
__name(ownerTokenFrom, "ownerTokenFrom");
var html = /* @__PURE__ */ __name((status, body, security) => new Response(body, { status, headers: { ...security, ...ADMIN_HEADERS } }), "html");
async function handleAdmin(request, url, deps) {
  const { store, ctx, modules: modules2, security } = deps;
  if (request.method === "GET") return html(200, loginPage(), security);
  if (request.method !== "POST") return html(405, loginPage("M\xE9todo no permitido."), security);
  const form3 = await request.formData();
  const pasted = String(form3.get("token") ?? "");
  if (/stn_[A-Za-z0-9_-]{10,}|\/d\/[a-z0-9]{12,40}\/mcp/.test(pasted))
    return html(400, loginPage("Eso es un enlace de Stone. Aqu\xED va el token del propietario (hub_\u2026); Stone se vincula desde la app Gateway Hub."), security);
  const token = ownerTokenFrom(pasted);
  if (!await deps.isOwner(token)) return html(401, loginPage("Token del propietario incorrecto."), security);
  const view = /* @__PURE__ */ __name(async (extra = {}, status = 200) => {
    const now = Date.now();
    const data = {
      token,
      origin: url.origin,
      modules: modules2,
      stoneLinked: !!(await providers(store))["music_"],
      environment: await environmentState(store),
      agents: await store.agentsList(),
      day: await store.metricsSummary(now - DAY),
      week: await store.metricsSummary(now - 7 * DAY),
      ...extra
    };
    return html(status, panelPage(data), security);
  }, "view");
  const fail = /* @__PURE__ */ __name((text2) => view({ notice: { text: text2, error: true } }, 400), "fail");
  const field = /* @__PURE__ */ __name((name) => String(form3.get(name) ?? "").trim(), "field");
  try {
    switch (field("action")) {
      case "login":
        return view();
      case "environment":
        return view({ notice: { text: (await changeEnvironment(store, field("state"))).message } });
      case "agent_create": {
        const made = await createAgent(
          store,
          modules2,
          url.origin,
          { name: field("name"), apps: form3.getAll("apps").map(String), readOnly: form3.get("readonly") === "1", days: Number(field("days")) }
        );
        return view({ newLink: { name: made.agent.name, url: made.url, token: made.token } });
      }
      case "agent_pause":
      case "agent_resume": {
        const pause = field("action") === "agent_pause";
        if (!await setPaused(store, field("id"), pause)) return fail("Ese agente no existe.");
        return view({ notice: { text: pause ? "Agente pausado." : "Agente reanudado." } });
      }
      case "agent_revoke":
        return await store.agentsDelete(field("id")) ? view({ notice: { text: "Enlace revocado: deja de funcionar ya." } }) : fail("Ese agente no existe.");
    }
  } catch (error) {
    return fail(error.message);
  }
  return fail("Acci\xF3n desconocida.");
}
__name(handleAdmin, "handleAdmin");

// src/channel/pairing.ts
var KEY4 = "channel.pairing";
var ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
var PAIRING_MS = 5 * 6e4;
async function createPairing(store, now = Date.now()) {
  const bytes = crypto.getRandomValues(new Uint8Array(10));
  const code = [...bytes].map((b) => ALPHABET[b % ALPHABET.length]).join("");
  await store.setConfig(KEY4, JSON.stringify({ hash: await sha256Hex(code), expiresAt: now + PAIRING_MS }));
  return { code, expiresAt: now + PAIRING_MS };
}
__name(createPairing, "createPairing");
async function redeemPairing(store, rawCode, name, now = Date.now()) {
  const code = String(rawCode ?? "").toUpperCase().replace(/[^A-Z0-9]/g, "");
  const saved = await store.getConfig(KEY4);
  if (!saved || code.length !== 10) return null;
  const { hash, expiresAt } = JSON.parse(saved);
  if (now > expiresAt) {
    await store.setConfig(KEY4, null);
    return null;
  }
  if (!await sameSecret(await sha256Hex(code), hash)) return null;
  await store.setConfig(KEY4, null);
  return createDevice(store, name);
}
__name(redeemPairing, "redeemPairing");

// src/admin/api.ts
var send2 = /* @__PURE__ */ __name((status, body, security) => new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", ...security } }), "send");
async function handleAdminApi(request, url, deps) {
  const { store, ctx, modules: modules2, security } = deps;
  const ok = /* @__PURE__ */ __name((body2) => send2(200, body2, security), "ok");
  const error = /* @__PURE__ */ __name((status, code, message) => send2(status, { error: { code, message } }, security), "error");
  const path = url.pathname.slice("/api/admin".length) || "/";
  const method = request.method;
  let body = {};
  if (method === "POST") {
    try {
      body = await request.json();
    } catch {
      return error(400, "INVALID_JSON", "El cuerpo debe ser JSON.");
    }
    if (typeof body !== "object" || body === null || Array.isArray(body)) return error(400, "INVALID_JSON", "El cuerpo debe ser un objeto JSON.");
  }
  try {
    if (path === "/state" && method === "GET") return ok({
      ...await overview(store, ctx, modules2, deps.version),
      shared: deps.shared === true,
      publicUrl: `${url.origin}/mcp`
    });
    if (path === "/connect-code" && method === "POST") {
      if (!deps.connectCode) return error(404, "NOT_FOUND", "Este hub no tiene enlace p\xFAblico.");
      return ok(await deps.connectCode());
    }
    if (path === "/activity" && method === "GET") {
      const since = Math.max(Date.now() - 7 * DAY, Math.floor(Number(url.searchParams.get("since")) || 0));
      return ok({ environment: await environmentState(store), ...await store.metricsSummary(since) });
    }
    if (path === "/scan" && method === "GET") return ok(await scan(ctx, modules2));
    if (path === "/environment" && method === "POST") return ok(await changeEnvironment(store, String(body.state ?? "")));
    if (path === "/pairing" && method === "POST") return ok(await createPairing(store));
    if (path === "/devices" && method === "GET") return ok({ devices: await devicesView(store, ctx) });
    if (path === "/devices" && method === "POST") return ok(await createDevice(store, String(body.name ?? "")));
    const device = /^\/devices\/([0-9a-f]{12})$/.exec(path);
    if (device && method === "DELETE") {
      if (!await deleteDevice(store, device[1])) return error(404, "NOT_FOUND", "Ese tel\xE9fono no existe.");
      deps.dropDevice?.(device[1]);
      return ok({ id: device[1] });
    }
    if (path === "/agents" && method === "POST") return ok(await createAgent(
      store,
      modules2,
      url.origin,
      { name: String(body.name ?? ""), apps: body.apps, readOnly: body.readOnly === true, days: Number(body.days) }
    ));
    const agent = /^\/agents\/([0-9a-f]{12})(\/pause|\/resume)?$/.exec(path);
    if (agent && method === "POST" && agent[2]) {
      return await setPaused(store, agent[1], agent[2] === "/pause") ? ok({ id: agent[1] }) : error(404, "NOT_FOUND", "Ese agente no existe.");
    }
    if (agent && method === "DELETE" && !agent[2]) {
      return await store.agentsDelete(agent[1]) ? ok({ id: agent[1] }) : error(404, "NOT_FOUND", "Ese agente no existe.");
    }
  } catch (failure) {
    if (failure instanceof ToolError) return error(400, failure.code, failure.message);
    console.error(JSON.stringify({ event: "admin_api_failed", path, error: String(failure) }));
    return error(500, "INTERNAL", "Algo fall\xF3 en el hub.");
  }
  return error(404, "NOT_FOUND", "Ruta desconocida de la API de administraci\xF3n.");
}
__name(handleAdminApi, "handleAdminApi");

// src/spaces.ts
var SPACE_IN_TOKEN = /^(?:hub|gw|dev)_s([0-9a-f]{16})_[A-Za-z0-9_-]{20,}$/;
var SPACE_KEY = "space.id";
var OWNER_KEY = "space.owner";
var spaceOf = /* @__PURE__ */ __name((token) => token ? SPACE_IN_TOKEN.exec(token)?.[1] ?? null : null, "spaceOf");
var newSpaceId = /* @__PURE__ */ __name(() => [...crypto.getRandomValues(new Uint8Array(8))].map((b) => b.toString(16).padStart(2, "0")).join(""), "newSpaceId");
async function setupSpace(store, space, phone) {
  if (!/^[0-9a-f]{16}$/.test(space)) throw new Error("bad space id");
  if (await store.getConfig(SPACE_KEY)) throw new Error("space already set up");
  await store.setConfig(SPACE_KEY, space);
  const owner = newSecret("hub", space);
  await store.setConfig(OWNER_KEY, await sha256Hex(owner));
  const device = await createDevice(store, phone);
  return { owner, device: device.device, secret: device.secret };
}
__name(setupSpace, "setupSpace");
async function isSpaceOwner(store, token) {
  const saved = await store.getConfig(OWNER_KEY);
  if (!saved || spaceOf(token) !== await store.getConfig(SPACE_KEY)) return false;
  const given = await sha256Hex(token);
  let diff = 0;
  for (let i = 0; i < saved.length; i++) diff |= saved.charCodeAt(i) ^ given.charCodeAt(i);
  return diff === 0 && saved.length === given.length;
}
__name(isSpaceOwner, "isSpaceOwner");
async function issueAgent(store, rawName) {
  const space = await store.getConfig(SPACE_KEY);
  if (!space) throw new Error("space not set up");
  const agents = await store.agentsList();
  if (agents.length >= MAX_AGENTS) {
    const oldest = [...agents].sort((a, b) => Math.max(a.lastUsed, a.created) - Math.max(b.lastUsed, b.created))[0];
    await store.agentsDelete(oldest.id);
  }
  const secret = newSecret("gw", space), now = Date.now();
  const agent = {
    id: newId(),
    name: String(rawName ?? "").trim().slice(0, 40) || "IA",
    tokenHash: await sha256Hex(secret),
    modules: ["*"],
    readOnly: false,
    paused: false,
    expiresAt: 0,
    created: now,
    lastUsed: 0
  };
  await store.agentsCreate(agent);
  return secret;
}
__name(issueAgent, "issueAgent");

// src/channel/dynamic.ts
var overlaps = /* @__PURE__ */ __name((a, b) => a.startsWith(b) || b.startsWith(a), "overlaps");
async function channelModules(ctx, builtIn) {
  const taken = builtIn.map((m) => m.prefix);
  const out = [];
  for (const record of Object.values(await providers(ctx.store))) {
    if (taken.some((p) => overlaps(p, record.prefix))) continue;
    taken.push(record.prefix);
    const online = /* @__PURE__ */ __name(() => !!ctx.channel?.online(record.device), "online");
    out.push({
      prefix: record.prefix,
      title: record.title,
      summary: `${record.title} on another phone (through Gateway Hub).`,
      androidPackage: record.app,
      validatesOwnArgs: true,
      async status() {
        return { linked: true, online: online(), tools: record.packages.length, detail: online() ? "En l\xEDnea por el canal." : "Tel\xE9fono sin conectar." };
      },
      async tools(c) {
        return online() ? channelTools(c, record) : [];
      },
      async call(name, args, c) {
        if (!online()) throw new ToolError("PROVIDER_OFFLINE", "That phone is not connected to the hub right now.");
        return channelCall(c, record, name, args);
      }
    });
  }
  return out;
}
__name(channelModules, "channelModules");

// src/serve.ts
var VERSION = "0.1.0";
var MAX_BODY = 1 << 20;
var SECURITY = {
  "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
  "X-Content-Type-Options": "nosniff",
  "Cache-Control": "no-store",
  "Referrer-Policy": "no-referrer",
  "X-Frame-Options": "DENY"
};
var CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type, Accept, Mcp-Session-Id, Mcp-Protocol-Version, X-Api-Key",
  "Access-Control-Expose-Headers": "Mcp-Session-Id"
};
var routers = /* @__PURE__ */ new WeakMap();
function routerFor(list) {
  let router = routers.get(list);
  if (!router) {
    router = createRouter(list);
    routers.set(list, router);
  }
  return router;
}
__name(routerFor, "routerFor");
function json(status, body, extra = {}) {
  return new Response(
    body === null ? null : JSON.stringify(body),
    { status, headers: { "Content-Type": "application/json", ...SECURITY, ...CORS, ...extra } }
  );
}
__name(json, "json");
var rpcError2 = /* @__PURE__ */ __name((status, message, extra = {}) => json(status, { jsonrpc: "2.0", id: null, error: { code: -32001, message } }, extra), "rpcError");
function rejected(store, caller, missing) {
  store.failures.hit(caller);
  console.warn(JSON.stringify({ event: "auth_rejected", caller, missing }));
}
__name(rejected, "rejected");
async function serve(request, env, store, fetcher = fetch, modules2 = modules, opts = {}) {
  const url = new URL(request.url);
  const caller = request.headers.get("CF-Connecting-IP") || "local";
  if (store.requests.hit(caller)) {
    console.warn(JSON.stringify({ event: "rate_limited", caller }));
    return rpcError2(429, "Gateway Hub: too many requests. Wait a minute.", { "Retry-After": "60" });
  }
  const owner = env.HUB_TOKEN;
  const space = opts.tenant ? await store.getConfig(SPACE_KEY) : null;
  if (opts.tenant && !space) {
    rejected(store, caller, false);
    return rpcError2(401, "Gateway Hub: unknown link.");
  }
  if (!opts.tenant && !owner) return rpcError2(503, "Gateway Hub has no owner token yet (npx wrangler secret put HUB_TOKEN).");
  const isOwner = /* @__PURE__ */ __name(async (token) => opts.tenant ? isSpaceOwner(store, token) : !!owner && sameSecret(token, owner), "isOwner");
  if (store.failures.blocked(caller)) return rpcError2(429, "Gateway Hub: too many wrong tokens. Wait a minute.", { "Retry-After": "60" });
  if (Number(request.headers.get("content-length") ?? 0) > MAX_BODY) return rpcError2(413, "Gateway Hub: body larger than 1 MiB.");
  const ctx = { store, env, channel: store.channel, fetch: /* @__PURE__ */ __name((input, init) => fetcher(input, init), "fetch") };
  if (url.pathname === "/pair") {
    if (request.method !== "POST") return rpcError2(405, "Gateway Hub: POST the pairing code.", { Allow: "POST" });
    let body = {};
    try {
      body = await request.json();
    } catch {
    }
    const made = await redeemPairing(store, String(body.code ?? ""), String(body.name ?? ""));
    if (!made) {
      rejected(store, caller, !body.code);
      return rpcError2(401, "Gateway Hub: wrong or expired pairing code.");
    }
    return json(200, made);
  }
  if (url.pathname === "/channel") {
    const bearer = /^bearer\s+(.+)$/i.exec(request.headers.get("authorization") ?? "")?.[1]?.trim() ?? "";
    const device = bearer ? await deviceFor(store, bearer) : null;
    if (!device) {
      rejected(store, caller, !bearer);
      return rpcError2(401, "Gateway Hub: unknown device.");
    }
    if (request.headers.get("Upgrade") !== "websocket" || !store.acceptChannel) return rpcError2(426, "Gateway Hub: WebSocket required.");
    return store.acceptChannel(device.id);
  }
  if (url.pathname === "/mcp") {
    if (request.method === "DELETE") return json(200, null);
    if (request.method !== "POST") return rpcError2(405, "Gateway Hub: send MCP requests with POST.", { Allow: "POST, DELETE" });
    const token = givenToken(request, url);
    const principal = token ? await principalFor(token, isOwner, store) : null;
    if (!principal) {
      rejected(store, caller, !token);
      return rpcError2(
        401,
        "Gateway Hub: missing, wrong, paused or expired link.",
        { "WWW-Authenticate": `Bearer resource_metadata="${url.origin}/.well-known/oauth-protected-resource"` }
      );
    }
    const body = await request.text();
    if (body.length > MAX_BODY) return rpcError2(413, "Gateway Hub: body larger than 1 MiB.");
    ctx.agent = principal.id;
    const dynamic = ctx.channel ? await channelModules(ctx, modules2) : [];
    const reply = await handleRpc(
      body,
      dynamic.length ? createRouter([...modules2, ...dynamic]) : routerFor(modules2),
      ctx,
      VERSION,
      { principal, record: /* @__PURE__ */ __name((tool, ok, ms) => store.metricsRecord(principal.id, tool, ok, ms), "record") }
    );
    const session = body.includes('"initialize"') ? { "Mcp-Session-Id": crypto.randomUUID() } : {};
    return reply === null ? json(202, null, session) : json(200, reply, session);
  }
  if (url.pathname === "/admin" || url.pathname === "/setup") {
    return handleAdmin(request, url, { store, ctx, modules: modules2, security: SECURITY, async isOwner(token) {
      if (token && await isOwner(token)) return true;
      rejected(store, caller, !token);
      return false;
    } });
  }
  if (url.pathname.startsWith("/api/admin/")) {
    const bearer = /^bearer\s+(.+)$/i.exec(request.headers.get("authorization") ?? "")?.[1]?.trim() ?? "";
    if (!bearer || !await isOwner(bearer)) {
      rejected(store, caller, !bearer);
      return new Response(
        JSON.stringify({ error: { code: "UNAUTHORIZED", message: "Token del propietario incorrecto." } }),
        { status: 401, headers: { "Content-Type": "application/json", "WWW-Authenticate": "Bearer", ...SECURITY } }
      );
    }
    const all = ctx.channel ? [...modules2, ...await channelModules(ctx, modules2)] : modules2;
    return handleAdminApi(request, url, {
      store,
      ctx,
      modules: all,
      security: SECURITY,
      version: VERSION,
      dropDevice: store.dropChannel?.bind(store),
      shared: !!space,
      connectCode: opts.connectCode
    });
  }
  return rpcError2(404, "Gateway Hub: unknown path. Use POST /mcp, or open /admin.");
}
__name(serve, "serve");

// src/oauth/connect.ts
var KEY5 = "oauth.connect";
var CONNECT_MS = 10 * 6e4;
var MAX_LIVE = 2e4;
async function live(store, now) {
  let all = {};
  try {
    all = JSON.parse(await store.getConfig(KEY5) ?? "{}");
  } catch {
  }
  for (const [hash, entry] of Object.entries(all)) if (entry.exp <= now) delete all[hash];
  return all;
}
__name(live, "live");
async function createConnectCode(store, space, now = Date.now()) {
  const all = await live(store, now);
  for (const [hash2, entry] of Object.entries(all)) if (entry.space === space) delete all[hash2];
  if (Object.keys(all).length >= MAX_LIVE) throw new Error("too many live codes");
  let code = "", hash = "";
  do {
    const n = crypto.getRandomValues(new Uint32Array(1))[0] % 1e8;
    code = String(n).padStart(8, "0");
    hash = await sha256Hex(code);
  } while (all[hash]);
  all[hash] = { space, exp: now + CONNECT_MS };
  await store.setConfig(KEY5, JSON.stringify(all));
  return { code, expiresAt: now + CONNECT_MS };
}
__name(createConnectCode, "createConnectCode");
async function redeemConnectCode(store, raw, now = Date.now()) {
  const code = String(raw ?? "").replace(/[\s-]/g, "");
  if (!/^\d{8}$/.test(code)) return null;
  const all = await live(store, now);
  const hash = await sha256Hex(code);
  const entry = all[hash];
  if (!entry) return null;
  delete all[hash];
  await store.setConfig(KEY5, JSON.stringify(all));
  return entry.space;
}
__name(redeemConnectCode, "redeemConnectCode");

// src/oauth/page.ts
var PAGE_HEADERS = {
  "Content-Type": "text/html; charset=utf-8",
  // No form-action: after the code, the answer redirects back to the AI app (another site).
  "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'"
};
function authorizePage(v) {
  const hidden = Object.entries(v.fields).map(([k, x]) => `<input type="hidden" name="${esc(k)}" value="${esc(x)}">`).join("");
  return page(`<h1>Conectar con tu tel\xE9fono</h1>
<div class="card"><h2>\xAB${esc(v.client)}\xBB quiere usar tus apps</h2>
<p class="muted">Abre <b>Gateway Hub</b> en tu tel\xE9fono y escribe aqu\xED el c\xF3digo de 8 n\xFAmeros que ves.</p>
<form method="post" action="/oauth/authorize">${hidden}
<label for="code">C\xF3digo</label>
<input type="text" id="code" name="code" inputmode="numeric" autocomplete="one-time-code" maxlength="12" placeholder="1234 5678" autofocus required>
${v.error ? `<p class="bad">${esc(v.error)}</p>` : ""}
<button type="submit">Conectar</button></form>
<p class="muted"><small>Despu\xE9s vuelves a ${esc(v.host)}. Si no pediste esto, cierra la p\xE1gina.</small></p></div>`);
}
__name(authorizePage, "authorizePage");
function messagePage(title, text2) {
  return page(`<h1>${esc(title)}</h1><div class="card"><p>${esc(text2)}</p></div>`);
}
__name(messagePage, "messagePage");

// src/oauth/seal.ts
var KEY6 = "oauth.key";
var b64 = /* @__PURE__ */ __name((bytes) => btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, ""), "b64");
var unb64 = /* @__PURE__ */ __name((text2) => Uint8Array.from(atob(text2.replace(/-/g, "+").replace(/_/g, "/")), (c) => c.charCodeAt(0)), "unb64");
async function key(store) {
  let raw = await store.getConfig(KEY6);
  if (!raw) {
    raw = b64(crypto.getRandomValues(new Uint8Array(32)));
    await store.setConfig(KEY6, raw);
  }
  return crypto.subtle.importKey("raw", unb64(raw), { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]);
}
__name(key, "key");
async function seal(store, kind, data) {
  const body = b64(new TextEncoder().encode(JSON.stringify(data)));
  const sig = new Uint8Array(await crypto.subtle.sign("HMAC", await key(store), new TextEncoder().encode(`${kind}.${body}`)));
  return `${kind}.${body}.${b64(sig)}`;
}
__name(seal, "seal");
async function unseal(store, kind, value) {
  if (typeof value !== "string" || value.length > 4096) return null;
  const [k, body, sig] = value.split(".");
  if (k !== kind || !body || !sig) return null;
  try {
    const ok = await crypto.subtle.verify("HMAC", await key(store), unb64(sig), new TextEncoder().encode(`${kind}.${body}`));
    return ok ? JSON.parse(new TextDecoder().decode(unb64(body))) : null;
  } catch {
    return null;
  }
}
__name(unseal, "unseal");
async function pkceMatches(verifier, challenge) {
  if (typeof verifier !== "string" || !/^[A-Za-z0-9._~-]{43,128}$/.test(verifier)) return false;
  return b64(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier)))) === challenge;
}
__name(pkceMatches, "pkceMatches");

// src/oauth/directory.ts
var CODE_MS = 5 * 6e4;
var OAUTH_PATHS = [
  "/register",
  "/oauth/register",
  "/oauth/authorize",
  "/oauth/token",
  "/.well-known/oauth-authorization-server",
  "/.well-known/oauth-protected-resource",
  "/.well-known/oauth-protected-resource/mcp"
];
var html2 = /* @__PURE__ */ __name((status, body, extra = {}) => new Response(body, { status, headers: { ...SECURITY, ...PAGE_HEADERS, ...extra } }), "html");
var oauthError = /* @__PURE__ */ __name((status, error, description) => json(status, { error, error_description: description }), "oauthError");
var goodRedirect = /* @__PURE__ */ __name((raw) => {
  if (typeof raw !== "string" || raw.length > 512) return false;
  try {
    const u = new URL(raw);
    return !u.hash && (u.protocol === "https:" || u.protocol === "http:" && ["localhost", "127.0.0.1", "[::1]"].includes(u.hostname));
  } catch {
    return false;
  }
}, "goodRedirect");
async function form2(request) {
  const type = request.headers.get("content-type") ?? "";
  try {
    if (type.includes("application/json")) return Object.fromEntries(Object.entries(await request.json()).map(([k, v]) => [k, String(v)]));
    return Object.fromEntries(new URLSearchParams(await request.text()));
  } catch {
    return {};
  }
}
__name(form2, "form");
async function handleDirectory(request, url, store, deps) {
  const caller = request.headers.get("CF-Connecting-IP") || "local";
  const origin = url.origin, path = url.pathname, method = request.method;
  if (store.requests.hit(caller)) return json(429, { error: "slow_down", error_description: "Too many requests. Wait a minute." }, { "Retry-After": "60" });
  if (path.startsWith("/.well-known/oauth-protected-resource") && method === "GET")
    return json(200, { resource: `${origin}/mcp`, authorization_servers: [origin], bearer_methods_supported: ["header"] });
  if (path === "/.well-known/oauth-authorization-server" && method === "GET") return json(200, {
    issuer: origin,
    authorization_endpoint: `${origin}/oauth/authorize`,
    token_endpoint: `${origin}/oauth/token`,
    registration_endpoint: `${origin}/oauth/register`,
    response_types_supported: ["code"],
    grant_types_supported: ["authorization_code"],
    code_challenge_methods_supported: ["S256"],
    token_endpoint_auth_methods_supported: ["none"],
    scopes_supported: ["mcp"]
  });
  if (path === "/register" && method === "POST") {
    if (store.signups.hit(caller)) return json(429, { error: { code: "SLOW_DOWN", message: "Demasiados registros desde esta red. Prueba en una hora." } }, { "Retry-After": "3600" });
    const body = await form2(request);
    const made = await deps.setupSpace(newSpaceId(), String(body.name ?? "").slice(0, 40));
    return json(200, { hub: origin, ...made });
  }
  if (path === "/oauth/register" && method === "POST") {
    const body = await (async () => {
      try {
        return await request.json();
      } catch {
        return {};
      }
    })();
    const uris = Array.isArray(body.redirect_uris) ? body.redirect_uris.slice(0, 5) : [];
    if (!uris.length || !uris.every(goodRedirect)) return oauthError(400, "invalid_redirect_uri", "redirect_uris must be https (or http to localhost).");
    const name = String(body.client_name ?? "").trim().slice(0, 60) || "IA";
    const client_id = await seal(store, "cl", { u: uris, n: name });
    return json(201, {
      client_id,
      client_name: name,
      redirect_uris: uris,
      token_endpoint_auth_method: "none",
      grant_types: ["authorization_code"],
      response_types: ["code"],
      client_id_issued_at: Math.floor(Date.now() / 1e3)
    });
  }
  if (path === "/oauth/authorize" && (method === "GET" || method === "POST")) {
    const p = method === "GET" ? Object.fromEntries(url.searchParams) : await form2(request);
    const client = await unseal(store, "cl", p.client_id);
    if (!client) return html2(400, messagePage("Enlace no v\xE1lido", "Esta app no se registr\xF3 bien. Vuelve a a\xF1adir el conector en tu IA."));
    const redirect = p.redirect_uri || (client.u.length === 1 ? client.u[0] : "");
    if (!client.u.includes(redirect)) return html2(400, messagePage("Enlace no v\xE1lido", "La direcci\xF3n de vuelta no coincide. Vuelve a a\xF1adir el conector."));
    const back = /* @__PURE__ */ __name((params) => {
      const target = new URL(redirect);
      for (const [k, v] of Object.entries({ ...params, ...p.state ? { state: p.state } : {} })) target.searchParams.set(k, v);
      return new Response(null, { status: 302, headers: { ...SECURITY, Location: target.toString() } });
    }, "back");
    if ((p.response_type ?? "code") !== "code") return back({ error: "unsupported_response_type" });
    if (!/^[A-Za-z0-9_-]{43}$/.test(p.code_challenge ?? "") || (p.code_challenge_method ?? "S256") !== "S256")
      return back({ error: "invalid_request", error_description: "PKCE S256 is required." });
    const fields = { client_id: p.client_id, redirect_uri: redirect, code_challenge: p.code_challenge, ...p.state ? { state: p.state } : {} };
    const view = { client: client.n, host: new URL(redirect).host, fields };
    if (method === "GET") return html2(200, authorizePage(view));
    if (store.failures.blocked(caller)) return html2(429, authorizePage({ ...view, error: "Demasiados intentos. Espera un minuto." }));
    const space = await redeemConnectCode(store, p.code);
    if (!space) {
      store.failures.hit(caller);
      return html2(401, authorizePage({ ...view, error: "Ese c\xF3digo no sirve. Mira el c\xF3digo de Gateway Hub (cambia cada 10 minutos)." }));
    }
    const code = await seal(store, "co", {
      s: space,
      c: p.client_id,
      r: redirect,
      ch: p.code_challenge,
      n: client.n,
      e: Date.now() + CODE_MS,
      id: newSpaceId()
    });
    return back({ code });
  }
  if (path === "/oauth/token" && method === "POST") {
    const p = await form2(request);
    if (p.grant_type !== "authorization_code") return oauthError(400, "unsupported_grant_type", "Only authorization_code.");
    const code = await unseal(store, "co", p.code);
    if (!code || code.e < Date.now() || code.c !== p.client_id || p.redirect_uri && p.redirect_uri !== code.r) {
      store.failures.hit(caller);
      return oauthError(400, "invalid_grant", "The code is wrong, expired or for another app.");
    }
    if (!await pkceMatches(p.code_verifier, code.ch)) return oauthError(400, "invalid_grant", "PKCE check failed.");
    if (!await useOnce(store, code.id, code.e)) return oauthError(400, "invalid_grant", "The code was already used.");
    const token = await deps.issueAgent(code.s, code.n);
    return json(200, { access_token: token, token_type: "Bearer", scope: "mcp" });
  }
  return json(404, { error: "not_found" });
}
__name(handleDirectory, "handleDirectory");
async function useOnce(store, id, exp, now = Date.now()) {
  let used = {};
  try {
    used = JSON.parse(await store.getConfig("oauth.used") ?? "{}");
  } catch {
  }
  for (const [k, e] of Object.entries(used)) if (e < now) delete used[k];
  if (used[id]) return false;
  used[id] = exp;
  await store.setConfig("oauth.used", JSON.stringify(used));
  return true;
}
__name(useOnce, "useOnce");

// src/store.ts
import { DurableObject } from "cloudflare:workers";

// src/channel/core.ts
var MAX_CHANNEL_BYTES = 1 << 20;
var MAX_PENDING = 72;
var ChannelCore = class {
  static {
    __name(this, "ChannelCore");
  }
  pending = /* @__PURE__ */ new Map();
  next = 1;
  socketOf;
  onRegister;
  constructor(socketOf, onRegister) {
    this.socketOf = socketOf;
    this.onRegister = onRegister;
  }
  online(device) {
    return !!this.socketOf(device);
  }
  request(device, app, message, timeoutMs) {
    const socket = this.socketOf(device);
    if (!socket) return Promise.reject(new ToolError("PROVIDER_OFFLINE", "The phone is not connected to the hub (open Gateway Hub on it)."));
    if (this.pending.size >= MAX_PENDING) return Promise.reject(new ToolError("BUSY", "The phone is busy. Try again soon."));
    const id = String(this.next++);
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        if (!this.pending.delete(id)) return;
        try {
          socket.send(JSON.stringify({ t: "cancel", id }));
        } catch {
        }
        reject(new ToolError("PROVIDER_TIMEOUT", "The app on the phone did not answer in time; the request was cancelled."));
      }, timeoutMs);
      this.pending.set(id, { device, resolve, reject, timer });
      try {
        socket.send(JSON.stringify({ t: "req", id, app, message }));
      } catch {
        clearTimeout(timer);
        this.pending.delete(id);
        reject(new ToolError("PROVIDER_OFFLINE", "The phone disconnected."));
      }
    });
  }
  /** One message from [device]: an answer ("res") or its list of apps and packages ("register"). */
  async receive(device, data) {
    const text2 = typeof data === "string" ? data : new TextDecoder().decode(data);
    if (text2.length > MAX_CHANNEL_BYTES) return;
    let message;
    try {
      message = JSON.parse(text2);
    } catch {
      return;
    }
    if (typeof message !== "object" || message === null) return;
    if (message.t === "res") {
      const waiting = this.pending.get(String(message.id));
      if (!waiting || waiting.device !== device) return;
      this.pending.delete(String(message.id));
      clearTimeout(waiting.timer);
      waiting.resolve(message.reply);
    } else if (message.t === "register") {
      await this.onRegister(device, message).catch((e) => console.warn(JSON.stringify({ event: "register_failed", device, error: String(e) })));
    }
  }
  /** The phone's socket closed: everything waiting for it fails at once. */
  closed(device) {
    for (const [id, w] of this.pending) if (w.device === device) {
      clearTimeout(w.timer);
      this.pending.delete(id);
      w.reject(new ToolError("PROVIDER_OFFLINE", "The phone disconnected."));
    }
  }
};

// src/store.ts
var toAgent = /* @__PURE__ */ __name((r) => ({
  id: String(r.id),
  name: String(r.name),
  tokenHash: String(r.token_hash),
  modules: JSON.parse(String(r.modules)),
  readOnly: r.read_only === 1,
  paused: r.paused === 1,
  expiresAt: Number(r.expires_at),
  created: Number(r.created),
  lastUsed: Number(r.last_used)
}), "toAgent");
var HubStore = class extends DurableObject {
  static {
    __name(this, "HubStore");
  }
  requests = new Limiter(120);
  // per caller and minute
  failures = new Limiter(20);
  // wrong tokens per caller and minute
  signups = new Limiter(5, 36e5);
  // new spaces per caller and hour (directory only)
  sql;
  constructor(ctx, env) {
    super(ctx, env);
    this.sql = ctx.storage.sql;
    this.sql.exec("CREATE TABLE IF NOT EXISTS config(key TEXT PRIMARY KEY, value TEXT NOT NULL)");
    this.sql.exec(`CREATE TABLE IF NOT EXISTS agents(id TEXT PRIMARY KEY, name TEXT NOT NULL, token_hash TEXT NOT NULL UNIQUE,
      modules TEXT NOT NULL, read_only INTEGER NOT NULL, paused INTEGER NOT NULL, expires_at INTEGER NOT NULL,
      created INTEGER NOT NULL, last_used INTEGER NOT NULL)`);
    this.sql.exec(`CREATE TABLE IF NOT EXISTS calls(at INTEGER NOT NULL, agent TEXT NOT NULL, tool TEXT NOT NULL,
      ok INTEGER NOT NULL, ms INTEGER NOT NULL)`);
    this.sql.exec("CREATE INDEX IF NOT EXISTS calls_at ON calls(at)");
    ctx.setWebSocketAutoResponse(new WebSocketRequestResponsePair("ping", "pong"));
  }
  /** "main": the single-owner hub (HUB_TOKEN). */
  fetch(request) {
    return serve(request, this.env, this);
  }
  // ---- the shared hub (called by the Worker over RPC, never reachable from outside) ----
  hub(name) {
    return this.env.HUB.get(this.env.HUB.idFromName(name));
  }
  /** u_<space>: one person's hub. Its sign-in codes come from the directory. */
  tenant(request) {
    return serve(request, this.env, this, fetch, void 0, { tenant: true, connectCode: /* @__PURE__ */ __name(async () => {
      const space = await this.getConfig(SPACE_KEY);
      if (!space) throw new Error("space not set up");
      return this.hub("directory").createConnect(space);
    }, "connectCode") });
  }
  /** "directory": new spaces and the public link's sign-in. */
  directory(request) {
    return handleDirectory(request, new URL(request.url), this, {
      setupSpace: /* @__PURE__ */ __name((space, phone) => this.hub(`u_${space}`).setupSpace(space, phone), "setupSpace"),
      issueAgent: /* @__PURE__ */ __name((space, name) => this.hub(`u_${space}`).issueAgent(name), "issueAgent")
    });
  }
  setupSpace(space, phone) {
    return setupSpace(this, space, phone);
  }
  issueAgent(name) {
    return issueAgent(this, name);
  }
  createConnect(space) {
    return createConnectCode(this, space);
  }
  // ---- phones' channel (WebSocket hibernation: one socket per device, tagged with its id) ----
  channel = new ChannelCore((device) => this.ctx.getWebSockets(device)[0], (device, message) => register(this, device, message));
  acceptChannel(device) {
    for (const old of this.ctx.getWebSockets(device)) {
      try {
        old.close(1e3, "replaced");
      } catch {
      }
    }
    const [client, server] = Object.values(new WebSocketPair());
    this.ctx.acceptWebSocket(server, [device]);
    return new Response(null, { status: 101, webSocket: client });
  }
  dropChannel(device) {
    for (const socket of this.ctx.getWebSockets(device)) {
      try {
        socket.close(4001, "revoked");
      } catch {
      }
    }
    this.channel.closed(device);
  }
  async webSocketMessage(socket, data) {
    await this.channel.receive(this.ctx.getTags(socket)[0] ?? "", data);
  }
  async webSocketClose(socket) {
    this.channel.closed(this.ctx.getTags(socket)[0] ?? "");
    try {
      socket.close(1e3, "closed");
    } catch {
    }
  }
  async getConfig(key2) {
    const rows = this.sql.exec("SELECT value FROM config WHERE key = ?", key2).toArray();
    return rows.length ? String(rows[0].value) : null;
  }
  async setConfig(key2, value) {
    if (value === null) this.sql.exec("DELETE FROM config WHERE key = ?", key2);
    else this.sql.exec("INSERT INTO config(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value", key2, value);
  }
  // ---- agents (only the SHA-256 of each token is stored) ----
  async agentsCreate(a) {
    this.sql.exec(
      "INSERT INTO agents VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)",
      a.id,
      a.name,
      a.tokenHash,
      JSON.stringify(a.modules),
      a.readOnly ? 1 : 0,
      a.paused ? 1 : 0,
      a.expiresAt,
      a.created,
      a.lastUsed
    );
  }
  async agentsList() {
    return this.sql.exec("SELECT * FROM agents ORDER BY created DESC").toArray().map(toAgent);
  }
  async agentByHash(hash) {
    const rows = this.sql.exec("SELECT * FROM agents WHERE token_hash = ?", hash).toArray();
    return rows.length ? toAgent(rows[0]) : null;
  }
  async agentsSet(id, changes) {
    if (changes.paused !== void 0) this.sql.exec("UPDATE agents SET paused = ? WHERE id = ?", changes.paused ? 1 : 0, id);
    if (changes.lastUsed !== void 0) this.sql.exec("UPDATE agents SET last_used = ? WHERE id = ?", changes.lastUsed, id);
  }
  async agentsDelete(id) {
    return this.sql.exec("DELETE FROM agents WHERE id = ?", id).rowsWritten > 0;
  }
  // ---- metrics: one row per tool call, kept 30 days ----
  recorded = 0;
  async metricsRecord(agent, tool, ok, ms) {
    const now = Date.now();
    this.sql.exec("INSERT INTO calls VALUES(?, ?, ?, ?, ?)", now, agent, tool, ok ? 1 : 0, Math.max(0, Math.round(ms)));
    if (++this.recorded % 200 === 1) this.sql.exec("DELETE FROM calls WHERE at < ?", now - 30 * 864e5);
  }
  async metricsSummary(since) {
    const total = this.sql.exec("SELECT COUNT(*) AS calls, COALESCE(SUM(1 - ok), 0) AS errors FROM calls WHERE at >= ?", since).one();
    const group = /* @__PURE__ */ __name((expr) => this.sql.exec(`SELECT ${expr} AS name, COUNT(*) AS calls, SUM(1 - ok) AS errors,
      AVG(ms) AS avg FROM calls WHERE at >= ? GROUP BY name ORDER BY calls DESC LIMIT 50`, since).toArray().map((r) => ({ name: String(r.name), calls: Number(r.calls), errors: Number(r.errors), avgMs: Number(r.avg) })), "group");
    return {
      since,
      calls: Number(total.calls),
      errors: Number(total.errors),
      byAgent: group("agent"),
      // The app is the tool's prefix, up to and including the first "_".
      byApp: group("substr(tool, 1, instr(tool, '_'))"),
      byTool: group("tool")
    };
  }
};

// src/index.ts
var local = /* @__PURE__ */ __name((url) => url.hostname === "127.0.0.1" || url.hostname === "localhost", "local");
var index_default = {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.protocol !== "https:" && !local(url)) return json(400, { error: "Gateway Hub: use https://" });
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: { ...CORS, ...SECURITY } });
    if (url.pathname === "/") return new Response("Gateway Hub OK\n", { headers: { "Content-Type": "text/plain", ...SECURITY } });
    const hub = /* @__PURE__ */ __name((name) => env.HUB.get(env.HUB.idFromName(name)), "hub");
    if (OAUTH_PATHS.includes(url.pathname)) return hub("directory").directory(request);
    const token = givenToken(request, url);
    const space = spaceOf(token);
    if (space) return hub(`u_${space}`).tenant(request);
    if (url.pathname === "/mcp" && !token && request.method === "POST") return json(
      401,
      { jsonrpc: "2.0", id: null, error: { code: -32001, message: "Gateway Hub: sign in first." } },
      { "WWW-Authenticate": `Bearer resource_metadata="${url.origin}/.well-known/oauth-protected-resource"` }
    );
    return hub("main").fetch(request);
  }
};
export {
  HubStore,
  index_default as default
};
//# sourceMappingURL=index.js.map
