import { DynamicWorkerExecutor } from "@cloudflare/codemode";
import type { RunStoneCode } from "./stone.ts";
import program from "./stone-plan.codemode.js";
import { ToolError } from "../modules/types.ts";

export function stoneExecutor(loader: WorkerLoader): RunStoneCode {
  const executor = new DynamicWorkerExecutor({ loader, timeout: 22_000, globalOutbound: null,
    modules: { "stone-plan.js": program } });
  return async (methods) => {
    const execution = await executor.execute('async () => (await import("stone-plan.js")).default(codemode)',
      [{ name: "codemode", fns: methods }]);
    if (execution.error || !execution.result) {
      throw new ToolError("CODE_MODE_FAILED", "El entorno aislado de Code Mode no devolvió una confirmación.");
    }
    return execution.result;
  };
}
