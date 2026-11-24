# Gateway Hub MCP

**Un solo enlace MCP para que una IA (Claude, ChatGPT, Codex…) use varias apps**, en lugar de
conectarlas una por una. Cada herramienta lleva el prefijo de su app, y el enrutador
(`src/router.ts`) envía cada llamada a la app que le corresponde.

| Prefijo | App | Qué hace |
|---|---|---|
| `music_` | [Stone](https://github.com/SiriusMagic/Stone) (en el teléfono) | Todas las herramientas de Stone, a través de su relay |
| `notes_` | Notas | Crear, buscar, leer, editar y borrar |
| `gallery_` | [Luma](https://github.com/SiriusMagic/Galer-a-mcp) (en el teléfono) | Buscar fotos sin verlas, álbumes, fechas, favoritas |
| `calc_` | [Calculadora](https://github.com/SiriusMagic/Calculadora.mcp) (en el teléfono) | Cálculo exacto y científico, historial, divisas y unidades |
| `utils_` | Utilidades | Hora en cualquier zona horaria y números al azar |

```text
Claude / ChatGPT ──https──▶ Gateway Hub (Cloudflare Worker, TypeScript)
   (un enlace por agente)     │ enlace del agente → apps permitidas · lectura/escritura · caducidad
                              │ 120/min · 1 MiB · validación de argumentos · métricas
                              ▼
                          router.ts ── prefijo ──▶ notes_ · utils_   (dentro del hub)
                              ├── gallery_ · calc_ ──https──▶ relay ──wss──▶ Luma · Calculadora (teléfono)
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

[Descarga Gateway Hub y Lucía 0.0.26 de prueba](android/GatewayHub-0.0.26-lucia-preview.apk).
Esta variante se instala junto a la app habitual. La versión 0.0.27 incorpora MCP local con Stone
y requiere las actualizaciones firmadas con la clave habitual de ambas apps.
Consulta las instrucciones en [Gateway Hub para Android](android/README.md).

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
- **Token del propietario:** secreto de Cloudflare (`wrangler secret put HUB_TOKEN`), de 32 caracteres aleatorios o
  más: con uno débil el hub no abre. Solo él abre
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
npm test                 # 30 pruebas en Node (enrutador, módulos, agentes, métricas, panel)
npm run dev              # http://127.0.0.1:8790 (secretos locales en .dev.vars, ignorado por Git)
openssl rand -base64 32 | npx wrangler secret put HUB_TOKEN   # 32+ caracteres aleatorios
npx wrangler deploy
```

- Añadir una app: [`MODULES.md`](MODULES.md).
- Arquitectura: [`ARQUITECTURA.md`](ARQUITECTURA.md).

> El Worker se publica como `stone-hub` para conservar la dirección y los datos actuales. Para
> cambiarle el nombre, edita `name` en `wrangler.toml`; tendrá otra dirección y otro almacenamiento.
