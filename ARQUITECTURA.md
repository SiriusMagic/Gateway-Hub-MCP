# Arquitectura de Gateway Hub MCP

```text
gateway-hub-mcp/
├── src/
│   ├── index.ts            # Worker: solo HTTPS → Durable Object único ("main")
│   ├── store.ts            # Durable Object: SQLite (notas, config, agentes, métricas) + límites
│   ├── serve.ts            # Límites, tamaño, quién llama → /mcp o /admin
│   ├── agents.ts           # Propietario y agentes: tokens, apps permitidas, solo lectura
│   ├── auth.ts             # Comparación en tiempo constante, lectura del token, limitador
│   ├── mcp.ts              # MCP / JSON-RPC 2.0: filtra herramientas por agente y registra métricas
│   ├── router.ts           # Discriminador: prefijo → módulo; aísla fallos de cada módulo
│   ├── validate.ts         # Validación de argumentos con el esquema de la herramienta
│   ├── admin/              # Panel del propietario (sin JavaScript, sin cookies)
│   │   ├── layout.ts · views.ts · actions.ts
│   └── modules/            # UNA CARPETA POR APP
│       ├── types.ts        # Contrato HubModule (lo único que un módulo necesita conocer)
│       ├── index.ts        # Lista de apps: añadir una = una línea
│       ├── music/          # Stone a través de su relay
│       ├── notes/ · calculator/ · utils/
├── test/                   # node --test (sin dependencias)
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
