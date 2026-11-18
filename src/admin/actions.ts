// /admin (and the old /setup): owner-only HTML panel. Every POST carries the owner token; there are
// no cookies, so a foreign page cannot act on the owner's behalf (no CSRF).
import type { HubContext, HubModule } from "../modules/types.ts";
import { ADMIN_HEADERS } from "./layout.ts";
import { type PanelData, loginPage, panelPage } from "./views.ts";
import { type AdminStore, DAY, createAgent, linkStone, setPaused, unlinkStone } from "./service.ts";
import { savedLink } from "../modules/music/index.ts";

const html = (status: number, body: string, security: Record<string, string>) =>
  new Response(body, { status, headers: { ...security, ...ADMIN_HEADERS } });

export interface AdminDeps {
  store: AdminStore; ctx: HubContext; modules: HubModule[]; security: Record<string, string>;
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

  try {
    switch (field("action")) {
      case "login": return view();
      case "link_stone": return view({ notice: { text: await linkStone(store, ctx, field("link")) } });
      case "unlink_stone": await unlinkStone(store); return view({ notice: { text: "Stone desvinculado." } });
      case "agent_create": {
        const made = await createAgent(store, modules, url.origin,
          { name: field("name"), apps: form.getAll("apps").map(String), readOnly: form.get("readonly") === "1", days: Number(field("days")) });
        return view({ newLink: { name: made.agent.name, url: made.url, token: made.token } });
      }
      case "agent_pause": case "agent_resume": {
        const pause = field("action") === "agent_pause";
        if (!await setPaused(store, field("id"), pause)) return fail("Ese agente no existe.");
        return view({ notice: { text: pause ? "Agente pausado." : "Agente reanudado." } });
      }
      case "agent_revoke":
        return (await store.agentsDelete(field("id"))) ? view({ notice: { text: "Enlace revocado: deja de funcionar ya." } }) : fail("Ese agente no existe.");
    }
  } catch (error) {
    return fail((error as Error).message);
  }
  return fail("Acción desconocida.");
}
