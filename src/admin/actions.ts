// /admin (and the old /setup): owner-only panel. Every POST carries the owner token; there are no
// cookies, so a foreign page cannot act on the owner's behalf (no CSRF).
import type { Agent, HubAdminStore, HubContext, HubModule, HubStoreApi, ToolError } from "../modules/types.ts";
import { MAX_AGENTS, newAgentToken, newId, sha256Hex } from "../agents.ts";
import { parseStoneLink, stoneRpc } from "../modules/music/stone.ts";
import { savedLink } from "../modules/music/index.ts";
import { ADMIN_HEADERS } from "./layout.ts";
import { type PanelData, loginPage, panelPage } from "./views.ts";

type Store = HubStoreApi & HubAdminStore;
const DAY = 86_400_000;
const html = (status: number, body: string, security: Record<string, string>) =>
  new Response(body, { status, headers: { ...security, ...ADMIN_HEADERS } });

export interface AdminDeps {
  store: Store; ctx: HubContext; modules: HubModule[]; security: Record<string, string>;
  /** Checks the owner token (counting failures per caller). */
  isOwner(token: string): Promise<boolean>;
}

export async function handleAdmin(request: Request, url: URL, deps: AdminDeps): Promise<Response> {
  const { store, ctx, modules, security } = deps;
  if (request.method === "GET") return html(200, loginPage(), security);
  if (request.method !== "POST") return html(405, loginPage("Método no permitido."), security);
  const form = await request.formData();
  const token = String(form.get("token") ?? "");
  if (!await deps.isOwner(token)) return html(401, loginPage("Token del propietario incorrecto."), security);

  const view = async (extra: Partial<PanelData> = {}, status = 200) => {
    const now = Date.now();
    const data: PanelData = { token, origin: url.origin, modules, stoneLinked: !!await savedLink(ctx),
      agents: await store.agentsList(), day: await store.metricsSummary(now - DAY), week: await store.metricsSummary(now - 7 * DAY), ...extra };
    return html(status, panelPage(data), security);
  };
  const fail = (text: string) => view({ notice: { text, error: true } }, 400);
  const field = (name: string) => String(form.get(name) ?? "").trim();

  switch (field("action")) {
    case "login": return view();
    case "link_stone": {
      let link;
      try { link = parseStoneLink(field("link"), ctx.env.STONE_RELAY_HOST); } catch (error) { return fail((error as Error).message); }
      let text = "Stone vinculado y en línea.";
      try { await stoneRpc(ctx, link, "tools/list", {}, 8_000); } catch (error) {
        if ((error as ToolError).code === "STONE_TOKEN") return fail((error as Error).message); // never save a rejected token
        text = "Stone vinculado. Ahora no responde: abre Stone con el Modo API activado.";
      }
      await store.setConfig("music.link", JSON.stringify(link)); await store.setConfig("music.tools", null);
      return view({ notice: { text } });
    }
    case "unlink_stone":
      await store.setConfig("music.link", null); await store.setConfig("music.tools", null);
      return view({ notice: { text: "Stone desvinculado." } });
    case "agent_create": {
      const name = field("name").slice(0, 40);
      if (!name) return fail("Ponle un nombre al agente.");
      if ((await store.agentsList()).length >= MAX_AGENTS) return fail(`Máximo ${MAX_AGENTS} agentes.`);
      const known = new Set(modules.map((m) => m.prefix));
      const apps = form.getAll("apps").map(String).filter((p) => known.has(p));
      if (!apps.length) return fail("Elige al menos una app.");
      const days = Math.min(365, Math.max(0, Math.floor(Number(field("days")) || 0)));
      const secret = newAgentToken(), now = Date.now();
      const agent: Agent = { id: newId(), name, tokenHash: await sha256Hex(secret), modules: apps.length === known.size ? ["*"] : apps,
        readOnly: form.get("readonly") === "1", paused: false, expiresAt: days ? now + days * DAY : 0, created: now, lastUsed: 0 };
      await store.agentsCreate(agent);
      return view({ newLink: { name, url: `${url.origin}/mcp?token=${secret}`, token: secret } });
    }
    case "agent_pause": case "agent_resume":
      await store.agentsSet(field("id"), { paused: field("action") === "agent_pause" });
      return view({ notice: { text: field("action") === "agent_pause" ? "Agente pausado." : "Agente reanudado." } });
    case "agent_revoke":
      return (await store.agentsDelete(field("id"))) ? view({ notice: { text: "Enlace revocado: deja de funcionar ya." } }) : fail("Ese agente no existe.");
  }
  return fail("Acción desconocida.");
}
