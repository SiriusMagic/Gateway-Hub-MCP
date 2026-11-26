# Arquitectura de Gateway Hub MCP

```text
gateway-hub-mcp/
├── src/
│   ├── index.ts            # Worker: solo HTTPS → Durable Object único ("main")
│   ├── store.ts            # Durable Object: SQLite (notas, config, agentes, métricas) + límites
│   ├── serve.ts            # Límites, tamaño, quién llama → /mcp o /admin
│   ├── agents.ts           # Propietario y agentes: tokens, apps permitidas, solo lectura
│   ├── auth.ts             # Comparación en tiempo constante, lectura del token, limitador, token fuerte
│   ├── mcp.ts              # MCP / JSON-RPC 2.0: filtra herramientas por agente y registra métricas
│   ├── router.ts           # Discriminador: prefijo → módulo; aísla fallos de cada módulo
│   ├── validate.ts         # Validación de argumentos con el esquema de la herramienta
│   ├── admin/              # Lo que puede hacer el propietario
│   │   ├── service.ts      # Lógica compartida (validación una sola vez)
│   │   ├── layout.ts · views.ts · actions.ts   # Panel HTML /admin (sin JavaScript, sin cookies)
│   │   └── api.ts          # API JSON /api/admin/* para la app Android (token solo en cabecera, sin CORS)
│   └── modules/            # UNA CARPETA POR APP
│       ├── types.ts        # Contrato HubModule (lo único que un módulo necesita conocer)
│       ├── index.ts        # Lista de apps: añadir una = una línea
│       ├── music/          # Stone a través de su relay
│       ├── notes/ · calculator/ · utils/
├── test/                   # node --test (sin dependencias)
├── android/                # App Android del propietario (Kotlin + Compose) → android/README.md
│   └── app/src/main/java/com/zirakusa/gatewayhub/
│       ├── data/           # HubApi (HTTPS), modelos, SessionStore cifrado con el Keystore
│       ├── ui/             # HubViewModel (único dueño del estado) y pantallas
│       ├── assistant/local/PhoneMcp.kt  # El Modo MCP de la familia: canal con firma de cada app (link, connect, disconnect, status, tool)
│       │                   # y PhoneMcpControls (Activar MCP, Permitir MCP local, Crear token)
│       ├── assistant/Meaning.kt  # Capa 2: la frase dicha de otra manera → palabras de Lucía; ejemplos por parecido
│       ├── assistant/local/Planner.kt  # Capa 3: el modelo encadena hasta 3 herramientas (lee al momento, cambia con «sí»)
│       ├── assistant/local/ToolGrammar.kt  # Gramáticas GBNF: el modelo solo escribe JSON con herramientas reales
│       └── assistant/      # Asistente: Words y Parser (español → orden, puros), Catalog (apps de la familia),
│                           # Brain (decide y actúa por Hands), Graph (árbol entrada → app → acción), BrainPage (mapa en Ajustes), Skills, StoneSkills, FamilySkills y MoreSkills (herramientas MCP de cada app como frases: las reglas de reconocimiento de intenciones) y Speech (sus respuestas en español), AndroidHands (intents),
│                           # Vosk/VoskEars (voz sin internet, modelo en assets/model-es), Listening (palabra
│                           # «Oye Lucía», EnergyGate, vocabulario), WakeService (escucha en primer plano),
│                           # Lucia* (asistente predeterminado: gesto de esquina y botón), Voice (TTS), BrainCanvas,
│                           # Tally (modo conteo, reglas puras), CountEars (escucha continua), Counting (pantalla) y
│                           # Volume (el volumen dicho dentro de cualquier orden)
├── MODULES.md              # Cómo crear un módulo
└── .github/workflows/      # CI: npm ci, pruebas y npm audit
```

## Reglas

1. **Dependencias en una sola dirección:** `serve` → `mcp` → `router` → `modules/*`. Un módulo solo
   importa `modules/types.ts` y sus propios archivos; nunca otro módulo ni `serve`.
2. **Apps remotas** (como Stone) se conectan por HTTPS a un host fijo y validado. La app sigue
   comprobando su token y sus permisos.
3. **Nada de código oculto:** cada función se usa desde el panel o desde una herramienta.
4. **Sin secretos en el código:** van en `wrangler secret` o en el panel.
5. Archivos cerca de 100 líneas; divídelos antes de 150.

## Reconocimiento de intenciones y feedback

Arka conserva la frase y valida el plan antes de ejecutar las órdenes conocidas. Para las acciones migradas llama
directamente a MCP local. Calculadora recibe la expresión mediante `calc_evaluate`; Reloj crea la alarma mediante
`clock_alarm_create`. Stone ejecuta `player_play_by_title` para buscar, aplicar ajustes y confirmar la pista en una
sola llamada y `player_play_collection` para sus mezclas. Estas órdenes requieren la versión de Stone con las
herramientas, MCP local permitido y la firma de la familia. El
[inventario de migración](docs/intenciones/migracion-mcp-local.md) enumera operaciones y controles pendientes.

Los votos y correcciones quedan en el teléfono en almacenamiento cifrado. Para sincronizarlos, se activa
**Compartir feedback con mi Hub** en los ajustes del asistente y se configura la sesión de propietario. Esa sesión
sirve para obtener una credencial de instalación. Los envíos posteriores usan la credencial limitada a feedback.
El propietario puede consultar los eventos recientes en `GET /api/feedback/private` y revocar una instalación con
`DELETE /api/feedback/credentials` y el JSON `{"installation":"<ID de instalación>"}`. Ambas rutas requieren su
token en la cabecera Authorization. Los alias se guardan en el teléfono y se resuelven de nuevo al usar el contacto.

### Activar la revisión por lotes

1. Desplegar el Worker y guardar una clave aleatoria independiente como secreto `FEEDBACK_REVIEW_TOKEN` en
   Cloudflare y en GitHub Actions. Configurar otro secreto de Actions, `LLM_API_KEY`, para el proveedor.
2. Configurar en GitHub Actions `FEEDBACK_API_URL` con el origen HTTPS del Hub, `LLM_API_URL` con la base HTTPS
   compatible con OpenAI que termina en `/v1`, y `LLM_MODEL` con el identificador del modelo. Habilitar la creación
   de PR por GitHub Actions en los ajustes del repositorio.
3. El workflow **Revisar feedback de intenciones** toma casos a las 05:23 UTC o cuando se ejecuta manualmente.
   GitHub toma la definición programada de la rama predeterminada del repositorio. El workflow toma el código de
   `main` y los PR de revisión apuntan a esa rama.

La cola agrupa correcciones por patrones de intención. En esta primera versión, el modelo revisa variantes de
«pon música» tomadas de un vocabulario cerrado. El modelo recibe una frase sintética y solo puede proponer una
variante de ese vocabulario. El workflow genera el caso de prueba y verifica la regresión antes de abrir un PR.
Las correcciones que no se pueden proyectar quedan disponibles en la cola privada para revisión manual.

El [inventario de acciones](INVENTARIO-INTENCIONES.md) enlaza el mapa de Stone y la familia, el catálogo versionado y el contrato de reproducción por título del canal MCP local.
