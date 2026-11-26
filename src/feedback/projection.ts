// Only vocabulary from this list can leave the private queue. No substring survives projection.
export const variants = ["arranca", "arranca la", "empieza", "empieza la", "inicia", "inicia la"] as const;
type Variant = typeof variants[number];
export type Sample = { variant: Variant; compound: boolean; background: boolean; titled: boolean; volume: boolean; equalizer: boolean };
export const sampleKey = (s: Sample) =>
  [variants.indexOf(s.variant), +s.compound, +s.background, +s.titled, +s.volume, +s.equalizer].join("-");

type JsonObject = Record<string, unknown>;
export function object(value: unknown): JsonObject | null {
  return value && typeof value === "object" && !Array.isArray(value) ? value as JsonObject : null;
}
const normalized = (value: string) => value.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().trim();
const candidate = /(?:^|\by\s+)(arranca(?: la)?|empieza(?: la)?|inicia(?: la)?)\s+musica\b(.*)$/;
export function project(record: JsonObject): Sample | null {
  if (!["corregido", "falló"].includes(String(record.status)) || typeof record.original !== "string") return null;
  if (record.status === "corregido" && (typeof record.expected !== "string" ||
      !/\b(?:pon(?:er|ga)?|reproduc\w*|sonar|suene)\b.*\b(?:musica|cancion|tema)\b/.test(normalized(record.expected)))) return null;
  const original = normalized(record.original);
  const match = candidate.exec(original);
  const step = Array.isArray(record.plan) ? object(record.plan[Number(record.step)]) : null;
  if (!match || !step || !["UNKNOWN", "OPEN", "PLAY_MUSIC", "PHONE_MCP", "CHAT"].includes(String(step.action))) return null;
  const tail = match[2].trim();
  if (/\by\s+(?:llama|abre|calcula|reproduce|pon)\b/.test(tail)) return null;
  const name = tail.split(/\b(?:con (?:el )?volumen|(?:el )?ecualizador|a\s+\d+[ %]|al\s+\d+[ %])/)[0]
    .replace(/\b(?:en segundo plano|de fondo)\b/g, "").trim();
  const compound = match.index > 0;
  if (compound && !/^calcula\s/.test(original)) return null;
  return { variant: match[1] as Variant, compound, background: /\b(?:en segundo plano|de fondo)\b/.test(tail),
    titled: /[a-z0-9]/.test(name), volume: /\bvolumen\b|\d+\s*%/.test(tail),
    equalizer: /\becualizador (?:activo|activado|encendido)\b/.test(tail) };
}

export function checkedSample(value: unknown): Sample | null {
  const data = object(value);
  if (!data || !variants.includes(data.variant as Variant)) return null;
  const { variant, compound, background, titled, volume, equalizer } = data;
  if (typeof compound !== "boolean" || typeof background !== "boolean" || typeof titled !== "boolean" ||
      typeof volume !== "boolean" || typeof equalizer !== "boolean") return null;
  return { variant: variant as Variant, compound, background, titled, volume, equalizer };
}
