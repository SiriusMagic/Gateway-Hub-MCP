// HTML shell of the admin panel: escaping, styles and the strict CSP (no scripts at all).

export const ADMIN_HEADERS = {
  "Content-Type": "text/html; charset=utf-8",
  "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'",
};

export function esc(value: unknown): string {
  return String(value).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
}

/** A form that carries the owner token (hidden) so the panel needs no cookie and no session. */
export function form(token: string, action: string, inner: string, extra = ""): string {
  return `<form method="post" action="/admin" ${extra}><input type="hidden" name="token" value="${esc(token)}">` +
    `<input type="hidden" name="action" value="${esc(action)}">${inner}</form>`;
}

export function page(body: string): string {
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
