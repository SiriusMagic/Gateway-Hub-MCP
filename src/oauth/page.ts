// The sign-in page the AI app opens: one field for the code Gateway shows. No script, no cookie; everything
// the page prints goes through esc().
import { esc, page } from "../admin/layout.ts";

export const PAGE_HEADERS = {
  "Content-Type": "text/html; charset=utf-8",
  // No form-action: after the code, the answer redirects back to the AI app (another site).
  "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'",
};

export interface AuthorizeView { client: string; host: string; fields: Record<string, string>; error?: string }

export function authorizePage(v: AuthorizeView): string {
  const hidden = Object.entries(v.fields).map(([k, x]) => `<input type="hidden" name="${esc(k)}" value="${esc(x)}">`).join("");
  return page(`<h1>Conectar con tu teléfono</h1>
<div class="card"><h2>«${esc(v.client)}» quiere usar tus apps</h2>
<p class="muted">Abre <b>Gateway Hub</b> en tu teléfono y escribe aquí el código de 8 números que ves.</p>
<form method="post" action="/oauth/authorize">${hidden}
<label for="code">Código</label>
<input type="text" id="code" name="code" inputmode="numeric" autocomplete="one-time-code" maxlength="12" placeholder="1234 5678" autofocus required>
${v.error ? `<p class="bad">${esc(v.error)}</p>` : ""}
<button type="submit">Conectar</button></form>
<p class="muted"><small>Después vuelves a ${esc(v.host)}. Si no pediste esto, cierra la página.</small></p></div>`);
}

export function messagePage(title: string, text: string): string {
  return page(`<h1>${esc(title)}</h1><div class="card"><p>${esc(text)}</p></div>`);
}
