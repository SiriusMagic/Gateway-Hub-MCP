import { execFileSync, spawnSync } from "node:child_process";
import { existsSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { pathToFileURL } from "node:url";
import { checkedSample, object, sampleKey, type Sample } from "../src/feedback/projection.ts";

const rulesPath = "android/app/src/main/resources/intent-music-prefixes.txt";
const base = "main";
const childEnv = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
  !["GH_TOKEN", "GITHUB_TOKEN", "FEEDBACK_REVIEW_TOKEN", "LLM_API_KEY"].includes(key)));
const githubEnv = { ...childEnv, GH_TOKEN: process.env.GH_TOKEN };
const testClass = "com.zirakusa.gatewayhub.assistant.IntentReviewFixtureTest";
const testReport = `android/app/build/test-results/testDebugUnitTest/TEST-${testClass}.xml`;

function url(name: string) {
  const value = process.env[name];
  if (!value) throw new Error("missing configuration");
  const parsed = new URL(value);
  if (parsed.protocol !== "https:" || parsed.username || parsed.password || parsed.search || parsed.hash) {
    throw new Error("invalid configured URL");
  }
  return parsed.href.replace(/\/$/, "");
}

async function post(target: string, key: string, body: unknown): Promise<unknown> {
  const response = await fetch(target, { method: "POST", redirect: "error", signal: AbortSignal.timeout(45_000),
    headers: { Authorization: `Bearer ${key}`, "Content-Type": "application/json" }, body: JSON.stringify(body) });
  if (!response.ok) throw new Error("provider unavailable");
  return response.json();
}

export function proposal(value: unknown, key: string, sample: Sample) {
  const data = object(value);
  return data !== null && Object.keys(data).length === 2 && data.key === key && data.addMusicPrefix === sample.variant;
}

export function fixture(sample: Sample) {
  const music = `${sample.variant} música${sample.background ? " en segundo plano" : ""}` +
    (sample.titled ? " Canción del Faro" : "") + (sample.volume ? " con el volumen a 50" : "") +
    (sample.equalizer ? (sample.volume ? " y el ecualizador activo" : " con el ecualizador activo") : "");
  return { phrase: (sample.compound ? "Calcula dos más 2 y " : "") + music,
    query: sample.titled ? "Canción del Faro" : null, expression: sample.compound ? "2+2" : null,
    volume: sample.volume ? "50" : null, equalizer: sample.equalizer, background: sample.background };
}

function git(args: string[], env = childEnv) {
  return execFileSync("git", args, { env, stdio: ["ignore", "pipe", "pipe"] }).toString().trim();
}
function gh(args: string[]) {
  return execFileSync("gh", args, { env: githubEnv, stdio: ["ignore", "pipe", "pipe"] }).toString().trim();
}
function testAndroid(args: string[]) {
  const gradle = process.env.INTENT_GRADLE || "./gradlew";
  return spawnSync(gradle, args, { cwd: "android", env: childEnv, stdio: "inherit" }).status === 0;
}

function publish(key: string, sample: Sample) {
  const branch = `feedback/intent-music-${sample.variant.replace(" ", "-")}`;
  const reviews = JSON.parse(gh(["pr", "list", "--base", base, "--head", branch, "--state", "all", "--json", "state,files"]));
  const changedCase = `android/app/src/test/resources/intent-review/${key}.json`;
  if (existsSync(changedCase)) return "already_fixed";
  const open = reviews.find((pr) => pr.state === "OPEN");
  if (open) return open.files.some((file) => file.path === changedCase) ? "proposed" : "temporary_error";
  if (reviews.some((pr) => pr.state === "CLOSED")) return "rejected";
  const before = readFileSync(rulesPath, "utf8");
  const startingHead = git(["rev-parse", "HEAD"]);
  try {
    writeFileSync(changedCase, JSON.stringify(fixture(sample), null, 2) + "\n");
    if (testAndroid([":app:testDebugUnitTest", "--tests", testClass])) return "already_fixed";
    if (!existsSync(testReport) || !readFileSync(testReport, "utf8").includes(`intent-fixture:${key}`)) return "temporary_error";
    writeFileSync(rulesPath, before.trimEnd() + "\n" + sample.variant + "\n");
    if (!testAndroid([":app:testDebugUnitTest", ":app:lintDebug"]) ||
        spawnSync("npm", ["test"], { env: childEnv, stdio: "inherit" }).status !== 0) return "rejected";
    git(["switch", "-c", branch]);
    git(["add", "--", rulesPath, changedCase]);
    git(["commit", "-m", `Reconocer «${sample.variant} música»`]);
    git(["push", "origin", `HEAD:refs/heads/${branch}`], githubEnv);
    gh(["pr", "create", "--base", base, "--head", branch, "--title", `Reconocer «${sample.variant} música»`,
      "--body", "Caso sintético de feedback. Falló antes de añadir la regla y pasó después. También pasaron las pruebas de Android, lint y npm. Revisión manual requerida."]);
    return "proposed";
  } finally {
    // Revert only this job's generated files, including an index left by a failed commit.
    git(["reset", startingHead, "--", rulesPath, changedCase]);
    if (existsSync(changedCase)) rmSync(changedCase);
    writeFileSync(rulesPath, before);
    if (git(["branch", "--show-current"]) === branch) {
      git(["switch", "--detach", startingHead]);
      git(["branch", "-D", branch]);
    }
  }
}

async function main() {
  const queue = url("FEEDBACK_API_URL");
  const modelApi = url("LLM_API_URL");
  const review = process.env.FEEDBACK_REVIEW_TOKEN;
  const llm = process.env.LLM_API_KEY;
  const model = process.env.LLM_MODEL;
  if (!review || !llm || !model || !process.env.GH_TOKEN) throw new Error("missing configuration");
  if (git(["status", "--porcelain"])) throw new Error("review requires a clean checkout");
  const batch = object(await post(`${queue}/api/feedback/review/lease`, review, {}));
  if (!batch || typeof batch.lease !== "string" || !/^[0-9a-f-]{36}$/.test(batch.lease) ||
      !Array.isArray(batch.cases) || batch.cases.length > 20) throw new Error("invalid queue response");
  for (const item of batch.cases) {
    const row = object(item);
    const sample = checkedSample(row?.sample);
    if (!row || !sample) continue;
    const key = sampleKey(sample);
    if (row.key !== key) continue;
    let status = "temporary_error";
    try {
      const answer = object(await post(`${modelApi}/chat/completions`, llm, {
        model, temperature: 0, response_format: { type: "json_object" },
        messages: [
          { role: "system", content: "Evalúa un caso sintético de voz en español. Si el imperativo pide reproducir música, propone exactamente {key,addMusicPrefix}, copiando la clave y la variante indicada. Devuelve {} si no corresponde. El programa genera la prueba a partir del caso." },
          { role: "user", content: JSON.stringify({ key, variant: sample.variant, ...fixture(sample) }) }
        ]
      }));
      const content = answer && Array.isArray(answer.choices) ? answer.choices[0]?.message?.content : null;
      status = typeof content === "string" && proposal(JSON.parse(content), key, sample) ? publish(key, sample) : "no_proposal";
    } catch { /* Report only status codes. Provider and command errors can contain credentials. */ }
    await post(`${queue}/api/feedback/review/finish`, review, { key, lease: batch.lease, status });
    console.log(`Caso ${key}: ${status}`);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main().catch(() => {
  console.error("Revisión de intenciones interrumpida. Comprueba la configuración y los permisos del workflow.");
  process.exitCode = 1;
});
