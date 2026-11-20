# Gateway Hub MCP

**Un solo enlace MCP para que una IA (Claude, ChatGPT, Codex…) use varias apps**, en lugar de
conectarlas una por una. Cada herramienta lleva el prefijo de su app, y el enrutador
(`src/router.ts`) envía cada llamada a la app que le corresponde.

| Prefijo | App | Qué hace |
|---|---|---|
| `music_` | [Stone](https://github.com/SiriusMagic/Stone) (en el teléfono) | Todas las herramientas de Stone, a través de su relay |
| `notes_` | Notas | Crear, buscar, leer, editar y borrar |
| `calc_` | Calculadora | Expresiones y porcentajes, sin ejecutar código |
| `utils_` | Utilidades | Hora en cualquier zona horaria y números al azar |

```text
Claude / ChatGPT ──https──▶ Gateway Hub (Cloudflare Worker, TypeScript)
   (un enlace por agente)     │ enlace del agente → apps permitidas · lectura/escritura · caducidad
                              │ 120/min · 1 MiB · validación de argumentos · métricas
                              ▼
                          router.ts ── prefijo ──▶ notes_ · calc_ · utils_   (dentro del hub)
                              └── music_ ──https──▶ relay de Stone ──wss──▶ Stone (teléfono)
```

## Quién hace qué

| | Gateway Hub (este repo) | Cada app (p. ej. Stone) |
|---|---|---|
| Enlaces para las IA | ✅ Genera uno por agente, con apps permitidas, solo lectura y caducidad | Un token solo para el hub |
| Métricas | ✅ Llamadas, errores y latencia por agente, app y herramienta | Las suyas, locales |
| Activar o apagar | Pausar o revocar agentes | ✅ **Interruptor en la propia app**: la app decide siempre |
| Permisos finales | Filtro previo por app y por solo lectura | ✅ **Siempre comprueba su token** y pide confirmación para lo crítico |

La seguridad final vive en cada app: aunque el hub fallara o lo atacaran, Stone seguiría
comprobando su propio token y pidiendo confirmación en el teléfono.

## App Android

[`android/`](android/README.md) contiene **Gateway Hub para Android 0.0.11**, la app nativa del propietario:
agentes, **escáner de apps compatibles**, apps y métricas. El APK está en `android/GatewayHub-0.0.11.apk`. Usa la API JSON
`/api/admin/*`, que solo acepta el token del propietario en la cabecera y no responde a navegadores
(sin CORS).

## Uso (desde el teléfono, sin pegar tokens en ningún chat)

1. Abre **`https://<hub>/admin`** (o la app) y pega tu **acceso**: `https://<hub>#hub_…`, o solo el token del
   propietario. Los tokens de Stone (`stn_…`) y de agente (`gw_…`) no sirven para entrar.
2. **Vincula Stone:** en Stone › Ajustes › Modo MCP, toca **Copiar**. Eso copia un
   enlace solo para el hub. Pégalo en *Apps*.
3. **Crea un enlace por IA:** pon un nombre, elige las apps, si es solo lectura y cuántos días
   dura. El panel muestra el enlace **una sola vez**: pégalo en Claude o ChatGPT como conector
   personalizado.
4. Desde el panel puedes **pausar o revocar** cada enlace y ver sus **métricas** de 24 h y 7 días.

## Seguridad

- **Transporte:** solo HTTPS (una petición por HTTP se rechaza, no se redirige). Cabeceras HSTS,
  nosniff, no-store, DENY y CSP sin scripts en `/admin`.
- **Token del propietario:** secreto de Cloudflare (`wrangler secret put HUB_TOKEN`). Solo él abre
  el panel; los enlaces de agente nunca lo abren.
- **Enlaces de agente:** `gw_` + 256 bits aleatorios. Se muestran una vez y solo se guarda su
  SHA-256.
- **Permisos:** un agente solo **ve y llama** las herramientas de sus apps. En solo lectura, solo las
  que leen (`readOnlyHint`).
- **Límites:** 120 peticiones por minuto por cliente; tras 20 tokens erróneos, bloqueo de un minuto;
  cuerpo de 1 MiB como máximo; lotes de 20.
- **Datos:** argumentos validados en el servidor con el esquema de cada herramienta, SQL siempre
  parametrizado, HTML siempre escapado y sin cookies (sin riesgo de CSRF).
- **Red:** el módulo de música solo acepta enlaces del relay configurado (`STONE_RELAY_HOST`), así que
  el hub no puede usarse para llamar a otras webs. El token de Stone viaja en la cabecera.
- **Aislamiento:** un módulo que falla no tumba a los demás.
- **Sin dependencias en ejecución:** MCP, validación y calculadora están escritos a mano; `wrangler`
  solo sirve para publicar.

## Desarrollo

```sh
npm ci
npm test                 # 27 pruebas en Node (enrutador, módulos, agentes, métricas, panel)
npm run dev              # http://127.0.0.1:8790 (secretos locales en .dev.vars, ignorado por Git)
npx wrangler secret put HUB_TOKEN
npx wrangler deploy
```

- Añadir una app: [`MODULES.md`](MODULES.md).
- Arquitectura: [`ARQUITECTURA.md`](ARQUITECTURA.md).

> El Worker se publica como `stone-hub` para conservar la dirección y los datos actuales. Para
> cambiarle el nombre, edita `name` en `wrangler.toml`; tendrá otra dirección y otro almacenamiento.
