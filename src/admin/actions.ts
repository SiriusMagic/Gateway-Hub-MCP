// /admin (and the old /setup): owner-only HTML panel. Every POST carries the owner token; there are
// no cookies, so a foreign page cannot act on the owner's behalf (no CSRF).
import type { HubContext, HubModule } from "../modules/types.ts";
import { ADMIN_HEADERS } from "./layout.ts";
import { type PanelData, loginPage, panelPage } from "./views.ts";
import { type AdminStore, DAY, createAgent, linkStone, setPaused, unlinkStone } from "./service.ts";
import { musicModule, savedLink } from "../modules/music/index.ts";
import { REMOTE_APPS, linkRemote, remoteLink, unlinkRemote } from "../modules/remote/index.ts";

/** Accepts the access as people paste it: "https://hub#hub_…", "…?token=hub_…" or only the token. */
export function ownerTokenFrom(pasted: string): string {
  return /hub_[A-Za-z0-9_-]{20,}/.exec(pasted)?.[0] ?? pasted.trim();
}

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
  const pasted = String(form.get("token") ?? "");
  // A Stone link/token opens Stone, not the hub: say so instead of counting a wrong token.
  if (/(stn|lum)_[A-Za-z0-9_-]{10,}|\/d\/[a-z0-9]{12,40}\/mcp/.test(pasted))
    return html(400, loginPage("Eso es un enlace de una app (Stone, Luma…). Aquí va el token del propietario (hub_…); el enlace de la app se pega luego en Apps."), security);
  const token = ownerTokenFrom(pasted);
  if (!await deps.isOwner(token)) return html(401, loginPage("Token del propietario incorrecto."), security);

  const view = async (extra: Partial<PanelData> = {}, status = 200) => {
    const now = Date.now();
    const data: PanelData = { token, origin: url.origin, modules, stoneLinked: !!await savedLink(ctx),
      stoneDetail: await musicModule.status?.(ctx).then((st) => st.detail).catch(() => undefined),
      remote: await Promise.all(REMOTE_APPS.map(async (app) => ({ app, linked: !!await remoteLink(ctx, app.prefix),
        detail: await modules.find((m) => m.prefix === app.prefix)?.status?.(ctx).then((st) => st.detail).catch(() => undefined) }))),
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
      case "link_app": case "unlink_app": {
        const app = REMOTE_APPS.find((a) => a.prefix === field("prefix"));
        if (!app) return fail("Esa app no está en este hub.");
        if (field("action") === "unlink_app") { await unlinkRemote(ctx, app.prefix); return view({ notice: { text: `${app.label} desvinculada.` } }); }
        return view({ notice: { text: await linkRemote(ctx, app, field("link")) } });
      }
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
