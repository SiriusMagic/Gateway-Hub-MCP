# Cómo añadir una app al hub (guía para Codex, Claude o una persona)

Cada app es un **módulo**: una carpeta en `src/modules/<app>/` con un `index.ts` que exporta un
objeto `HubModule` (contrato en `src/modules/types.ts`). El enrutador (`src/router.ts`) lee el
**prefijo** de la herramienta que pide la IA y la envía al módulo dueño de ese prefijo. No hay
`if/else` gigantes.

## Pasos

1. Elige un prefijo único: letras minúsculas o cifras terminadas en `_` (`gallery_`, `weather_`).
   No puede empezar por otro prefijo ya usado (`music_` y `musicx_` chocan, y el hub no arranca).
2. Copia la **plantilla mínima** de abajo en `src/modules/<app>/index.ts` (o mira `src/modules/music/` para una app remota).
3. Añade el módulo a la lista de `src/modules/index.ts`. Nada más cambia.
4. Escribe una prueba en `test/` y ejecuta `npm test`.

## Reglas (el router y las pruebas las hacen cumplir)

- **Nombres:** todas las herramientas empiezan por el prefijo del módulo: `^[a-z][a-z0-9_]{1,63}$`.
  Una herramienta mal nombrada no se muestra a la IA.
- **Esquema estricto:** cada `inputSchema` declara `type`, `required`, `maxLength`, `minimum` y
  `maximum`, y lleva `additionalProperties: false`. El hub valida los argumentos en el servidor
  antes de llamar al módulo.
- **Errores:** los errores esperables se lanzan como `new ToolError("CODIGO", "mensaje")`, que la
  IA lee y puede corregir. Cualquier otro error se registra y se responde sin detalles internos.
- **Nunca** uses `eval`, `Function()`, `new Function` ni plantillas con código.
- **Datos:** se guardan solo con `ctx.store`. Las consultas a SQL son siempre parametrizadas
  (`sql.exec("… WHERE id = ?", id)`) y nunca se concatena texto en ellas.
- **Red:** `ctx.fetch` se usa solo hacia hosts fijos y conocidos, nunca hacia una URL que mande la
  IA. Si una app necesita una dirección configurable, valídala como hace `music/stone.ts`.
- **Secretos:** nunca en el código. Van con `npx wrangler secret put NOMBRE` o en `/admin`.
- **Anotaciones:** pon `readOnlyHint: true` en lo que solo lee y `destructiveHint: true` en lo que
  borra.
- **Tamaño:** un archivo cerca de 100 líneas; divídelo antes de pasar de 150.

## App Android compatible (para el escáner)

Si la app vive en el teléfono, como Stone, la app Gateway Hub la encuentra con su escáner cuando la app
declara esto en una Activity exportada:

```xml
<intent-filter>
    <action android:name="com.zirakusa.gatewayhub.action.APP" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
<meta-data android:name="com.zirakusa.gatewayhub.module" android:value="weather_" />
```

- Cuando el usuario toca «Abrir y vincular», la app recibe esa acción. **Solo debe mostrar su pantalla de
  conexión**: no puede leer extras ni activar nada por sí sola (ver `HubDiscovery.kt` en Stone).
- En el hub, el módulo declara `androidPackage` y un `status(ctx)` de solo lectura. El escáner lo llama
  sin cambiar nada.
- **Vinculación automática (opcional):**
  - Declara `<meta-data android:name="com.zirakusa.gatewayhub.link" android:value="${applicationId}.hublink"/>`
    y un `ContentProvider` exportado con `android:permission="com.zirakusa.gatewayhub.permission.LINK"`.
    Ese permiso se declara también en la app, con `protectionLevel="signature"`.
  - Su método `call("link")` devuelve `link`, o `error` + `message`.
  - Antes de responder, la app comprueba la firma de quien llama **y** un interruptor del usuario. Ver
    `HubLink.kt` en Stone.
  - Opcional: `call("mode", "safe"|"background")` aplica el modo de Gateway (en `safe`, la app solo atiende MCP
    mientras está en pantalla) y responde `mode`. Ver `HubLink.answerMode` en Stone.
- **Puente local MCP (fase 1, opcional):** un `Service` exportado con la acción `com.zirakusa.gatewayhub.action.MCP`,
  protegido con `com.zirakusa.gatewayhub.permission.LINK` (`signature`), que responde por `Messenger`:
  - Mensaje `what = 1`, `arg1` = id de petición, `Bundle` con `message` = un objeto JSON-RPC MCP (sin lotes); la
    respuesta va a `replyTo` con el mismo `arg1`. Máximo 200 000 caracteres por mensaje.
  - La app identifica a Gateway con `Message.sendingUid` → paquete `com.zirakusa.gatewayhub` + misma firma; nunca con
    datos del mensaje.
  - En esta fase, solo lectura: `tools/list` y `tools/call` solo con herramientas que la app clasifica como de lectura.
  - Para «Probar puente local», la app puede marcar **una** herramienta de lectura sin efectos con
    `_meta["com.zirakusa.gatewayhub/probe"] = true`. Si no marca ninguna, Gateway solo prueba `initialize` y `tools/list`.
  - Errores con el código al principio de `error.message` (`REDIRECT_OFF`, `NOT_SAME_DEVELOPER`, `BUSY`,
    `RESPONSE_TOO_LARGE`…). Implementación de referencia: `HubBridge.kt` y `HubBridgeService.kt` en Stone.
- La app del hub solo marca «Mismo desarrollador» si la firma coincide (`checkSignatures`). Una app de
  otro desarrollador aparece con «Firma desconocida».

## Plantilla mínima

```ts
import { type HubModule, ToolError, text } from "../types.ts";

export const weatherModule: HubModule = {
  prefix: "weather_",
  title: "Tiempo",
  summary: "Forecast for a city.",
  async tools() {
    return [{ name: "weather_today", description: "Today's forecast for a city.",
      inputSchema: { type: "object", additionalProperties: false, required: ["city"],
        properties: { city: { type: "string", minLength: 1, maxLength: 80 } } },
      annotations: { title: "Tiempo de hoy", readOnlyHint: true } }];
  },
  async call(name, args, ctx) {
    if (name === "weather_today") return text(`Sunny in ${args.city}`);
    throw new ToolError("UNKNOWN_TOOL", `Unknown weather tool '${name}'.`);
  },
};
```
