// Sections of the admin panel. Every value shown goes through esc().
import type { Agent, CallStat, HubModule, MetricsSummary } from "../modules/types.ts";
import { agentStatus } from "../agents.ts";
import { esc, form, page } from "./layout.ts";

export interface PanelData {
  token: string; origin: string; modules: HubModule[]; stoneLinked: boolean;
  agents: Agent[]; day: MetricsSummary; week: MetricsSummary;
  notice?: { text: string; error?: boolean }; newLink?: { name: string; url: string; token: string };
}

export const loginPage = (message = "") => page(`<h1>Gateway Hub</h1><p class="muted">Un solo enlace MCP para varias apps.</p>
${message ? `<p class="bad">${esc(message)}</p>` : ""}
<div class="card"><form method="post" action="/admin" autocomplete="off"><input type="hidden" name="action" value="login">
<label for="t">Acceso (dirección + token) o solo el token del propietario</label><input id="t" name="token" type="password" required>
<button>Entrar</button></form></div>`);

const date = (ms: number) => ms ? new Date(ms).toISOString().slice(0, 16).replace("T", " ") + " UTC" : "nunca";

function appsCard(d: PanelData): string {
  const rows = d.modules.map((m) => `<li><b>${esc(m.title)}</b> <small>${esc(m.prefix)}*</small>${m.prefix === "music_"
    ? (d.stoneLinked ? ' <span class="good">vinculado</span>' : ' <span class="bad">sin vincular</span>') : ' <span class="good">listo</span>'}</li>`).join("");
  const stone = form(d.token, "link_stone", `<label for="l">Enlace de Stone</label>
<input id="l" name="link" type="url" placeholder="https://…/d/…/mcp?token=stn_…" required>
<small>Stone › Ajustes › Modo MCP › Copiar (copia un enlace solo para el hub).</small><br><button>Vincular Stone</button>`);
  const unlink = d.stoneLinked ? form(d.token, "unlink_stone", `<button class="ghost">Desvincular Stone</button>`) : "";
  return `<div class="card"><h2>Apps</h2><ul>${rows}</ul>${stone}${unlink}</div>`;
}

function agentRow(d: PanelData, a: Agent): string {
  const status = agentStatus(a);
  const label = { active: '<span class="good">activo</span>', paused: '<span class="bad">pausado</span>', expired: '<span class="bad">caducado</span>' }[status];
  const toggle = form(d.token, a.paused ? "agent_resume" : "agent_pause", `<input type="hidden" name="id" value="${esc(a.id)}"><button class="ghost">${a.paused ? "Reanudar" : "Pausar"}</button>`);
  const revoke = form(d.token, "agent_revoke", `<input type="hidden" name="id" value="${esc(a.id)}"><button class="ghost">Revocar</button>`);
  return `<div class="agent"><b>${esc(a.name)}</b> ${label}<br><small>${a.modules.includes("*") ? "todas las apps" : esc(a.modules.join(" "))} ·
${a.readOnly ? "solo lectura" : "lectura y escritura"} · caduca: ${a.expiresAt ? esc(date(a.expiresAt)) : "nunca"} · último uso: ${esc(date(a.lastUsed))}</small>
<div class="row">${toggle}${revoke}</div></div>`;
}

function agentsCard(d: PanelData): string {
  const created = d.newLink ? `<div class="card"><h2 class="good">Enlace de «${esc(d.newLink.name)}»</h2>
<small>Cópialo ahora: no se vuelve a mostrar. Pégalo en Claude o ChatGPT como conector personalizado.</small>
<div class="secret">${esc(d.newLink.url)}</div><small>O con cabecera: Authorization: Bearer</small><div class="secret">${esc(d.newLink.token)}</div></div>` : "";
  const apps = d.modules.map((m) => `<label class="inline"><input type="checkbox" name="apps" value="${esc(m.prefix)}" checked>${esc(m.title)}</label>`).join("");
  const create = form(d.token, "agent_create", `<label for="n">Nombre</label><input id="n" name="name" type="text" maxlength="40" placeholder="Claude, ChatGPT…" required>
<label>Apps</label>${apps}<label class="inline"><input type="checkbox" name="readonly" value="1">Solo lectura</label>
<label for="days">Caduca en días (0 = nunca)</label><input id="days" name="days" type="number" min="0" max="365" value="90"><button>Crear enlace</button>`, 'autocomplete="off"');
  const list = d.agents.length ? d.agents.map((a) => agentRow(d, a)).join("") : '<p class="muted">Aún no hay agentes.</p>';
  return `${created}<div class="card"><h2>Agentes (un enlace por IA)</h2>${list}</div><div class="card"><h2>Nuevo enlace</h2>${create}</div>`;
}

const table = (title: string, rows: CallStat[]) => rows.length ? `<h3>${esc(title)}</h3><table><tr><th>Nombre</th><th class="n">Llamadas</th><th class="n">Errores</th><th class="n">ms</th></tr>${
  rows.map((r) => `<tr><td>${esc(r.name)}</td><td class="n">${r.calls}</td><td class="n">${r.errors}</td><td class="n">${Math.round(r.avgMs)}</td></tr>`).join("")}</table>` : "";

function metricsCard(d: PanelData): string {
  const names = new Map(d.agents.map((a) => [a.id, a.name]));
  const byAgent = d.week.byAgent.map((r) => ({ ...r, name: r.name === "owner" ? "Propietario" : names.get(r.name) ?? `(revocado ${r.name})` }));
  return `<div class="card"><h2>Métricas</h2><p>24 h: <b>${d.day.calls}</b> llamadas, <b>${d.day.errors}</b> con error · 7 días: <b>${d.week.calls}</b> / <b>${d.week.errors}</b></p>
${table("Por agente (7 días)", byAgent)}${table("Por app (7 días)", d.week.byApp)}${table("Herramientas más usadas (7 días)", d.week.byTool.slice(0, 10))}</div>`;
}

export function panelPage(d: PanelData): string {
  const notice = d.notice ? `<p class="${d.notice.error ? "bad" : "good"}">${esc(d.notice.text)}</p>` : "";
  return page(`<h1>Gateway Hub</h1><p class="muted">${esc(d.origin)}/mcp</p>${notice}${agentsCard(d)}${appsCard(d)}${metricsCard(d)}
${form(d.token, "login", `<button class="ghost">Actualizar</button>`)}`);
}
