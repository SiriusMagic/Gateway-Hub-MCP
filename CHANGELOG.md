# Hub 0.1.0 · Gateway 0.0.9 (hub compartido)

- **Un hub para todos, cada uno en su espacio.** Gateway se une solo al abrirse: sin Cloudflare, sin tokens, sin
  registro. Cada llamada de una IA llega solo al teléfono de su dueño.
- **Enlace público + código:** se pega `https://<hub>/mcp` en Claude o ChatGPT y, cuando lo pide, se escribe el código
  de 8 cifras de Gateway (OAuth 2.1 con PKCE; el código sirve una vez y cambia cada 10 minutos).
- Stone se vincula solo con el paquete completo (todo, un año, se renueva solo). Requiere Stone 1.7.19.
- Gateway funciona en segundo plano por defecto, para que la IA llegue a Stone sin abrir la app.
- Corregido: Gateway no guardaba la credencial del teléfono y creaba un teléfono nuevo cada vez que se abría.
- Para publicarlo: `npx wrangler deploy` (mismo Worker `stone-hub`; tus datos actuales siguen en «main»).

# Sin publicar (fase 7) · hub 0.0.5

## App más simple para gente sin conocimientos técnicos

- Al entrar se ve **una sola pantalla**: un estado grande («Todo listo» / «Te falta un paso» / «Está apagado») y dos
  pasos: **1. Conecta Stone** (un botón) y **2. Conecta tu IA** (botones «Conectar Claude» / «Conectar ChatGPT», con
  valores por defecto: todas las apps, lectura y escritura, sin caducidad). Cada IA conectada se puede «Quitar».
- Las pestañas Agentes, Escáner, Apps, Métricas y Ajustes pasan a **Opciones avanzadas** (con botón Volver).
- Inicio de sesión: primero «Empieza aquí (gratis)» con pasos numerados; el campo de pegar acceso queda debajo, en
  «¿Ya tienes un acceso?». Textos sin jerga (sin «token hub_», «Workers Scripts», «Android Keystore»).
- El enlace nuevo explica en 3 pasos dónde pegarlo en Claude o ChatGPT; «Copiar solo el token» solo en avanzadas.

## Fase 7: adiós al relay

- El hub ya no tiene el camino por el relay: `music_` llega a Stone solo por el canal del teléfono (Gateway). Sin
  teléfono conectado responde `PROVIDER_OFFLINE`. Se quitan `STONE_RELAY_HOST`, `/api/admin/stone` y «Vincular Stone»
  por enlace.
- Gateway: fuera la vinculación por HubLink (Sincronización automática, «Vincular sola», pegar enlace de Stone) y el
  envío del modo por `ContentProvider`. El modo y el entorno viajan solo por el puente (`gatewayhub/state`).
- Stone: se borra la carpeta `relay/`. El Worker `stone-relay` se puede borrar en Cloudflare.
- Para actualizar: Gateway › Ajustes › Cloudflare › «Actualizar mi hub» (o `npx wrangler deploy`).

# Gateway Hub 0.0.8 (app) · hub 0.0.4

Publica las fases 2 a 6. Necesita Stone 1.7.18.

## Fase 6: Stone solo proveedor

- Gateway envía el estado del entorno y el modo (seguro / segundo plano) a cada app vinculada por el puente
  (`gatewayhub/state`), al vincular, al cambiar el modo y al conectar, pausar o desconectar. Stone 1.7.18 ya no tiene
  HubLink: aplica el modo seguro sola (solo responde con Stone en pantalla).
- El hub conserva el camino por el relay solo para Stone anteriores; se retiró en la fase 7.

## Fase 5: varios teléfonos

- **Añadir otro teléfono:** Ajustes › Teléfonos › «Añadir otro teléfono» crea un código de un solo uso (10 caracteres,
  5 minutos; el hub guarda solo su hash y los códigos erróneos cuentan como tokens erróneos). En el otro teléfono, la
  misma APK: entrada › «Unir este teléfono a un hub». Ese teléfono no tiene el token del propietario: solo su credencial
  del canal.
- **Teléfono satélite:** pantalla propia con el estado del canal, sus apps compatibles (Vincular, Paquetes, Probar) y
  «Salir del hub». Cada teléfono firma los paquetes de sus apps con su propia clave del Keystore.
- **Enrutado sin programar nada:** cada app registrada por un teléfono cuyo prefijo no usa un módulo integrado se sirve
  sola por su canal. La misma app en otro teléfono recibe otro prefijo (`music_` → `music2_` … `music9_`); los agentes
  pueden limitarse a ella como a cualquier app.
- Ajustes › Teléfonos lista los teléfonos, si están conectados y qué apps sirven, y permite revocarlos (el canal se corta
  al momento).

## Fase 4: cloudflare por usuario

- **La app publica o actualiza tu hub** con un token de Cloudflare que se usa una vez y se olvida:
  - Entrada › «¿No tienes el token hub_?»: si la cuenta no tiene hub, se publica (`gateway-hub`, con su Durable Object,
    su subdominio workers.dev y un `hub_` nuevo); si ya lo tiene (`stone-hub`…), se actualiza y se entra con un `hub_` nuevo.
  - Ajustes › Cloudflare: **Actualizar mi hub** (sube la versión que trae la app; conserva el token, los agentes y los
    datos con `keep_bindings`, sin migraciones) y **Generar nuevo token del propietario**.
- El Worker va dentro del APK (`assets/hub-worker.js`, `npm run bundle:android`; una prueba comprueba que es el de esta
  versión y que no lleva secretos). APK universal: nada del desarrollador dentro.
- **Se elimina** el token de Cloudflare guardado (0.0.6), su aviso de caducidad y el intercambio con Stone. El que estaba
  guardado se borra al abrir esta versión.
- Hub 0.0.4.

## Fase 3: canal central

- **Hub (Worker `stone-hub`):** nuevo canal `wss://<hub>/channel`. Cada teléfono entra con su propia credencial
  (`POST /api/admin/devices`, se muestra una vez y el hub guarda solo su hash; `DELETE` la revoca y corta el canal al
  momento). El WebSocket lo mantiene el Durable Object `HubStore` que ya existe: no hay clase nueva ni migración.
  - Gateway registra por el canal sus apps proveedoras (prefijo) y los paquetes emitidos. El hub nunca firma paquetes:
    solo elige hasta 8 candidatos por llamada, y la app los verifica.
  - `music_`: si Stone está conectada por el canal, las llamadas van por ahí y **necesitan un paquete activo**
    (`NO_PACKAGE` si no lo hay; Gateway da a Stone uno de solo lectura al vincular). Si no, todo sigue por el relay
    como hasta ahora. Un tiempo agotado avisa al teléfono para cancelar la petición.
  - `/api/admin/state` y `/api/admin/devices` muestran los teléfonos, si están conectados y qué apps sirven.
- **App:** `channel/` (OkHttp 5.3.2, como Stone): conecta el canal con la credencial del teléfono, reenvía cada petición
  a la app por el puente local con un id propio (`ch-<id>`, para cancelar la correcta) y vuelve a registrarse al cambiar
  los paquetes. Funciona con la app abierta y, en «MCP en segundo plano», desde el servicio. En modo seguro se corta al
  salir de Gateway. Ajustes › Entorno MCP muestra el estado del canal.
- Sin desplegar el Worker ni generar APK.

## Fase 2: proveedores y paquetes de ejecución

- Escáner › Stone: **Vincular** (entrega la clave pública del entorno y recibe el id de la instalación; Stone recibe al
  momento un paquete de solo lectura) y **Paquetes**: ofrecer un paquete (para todos los agentes o uno, con leer,
  revisar, escribir, modificar o revertir, 7/30/90 días), ver si está activo o pendiente de aprobar en Stone, y revocar.
- Clave del entorno ECDSA P-256 en el Android Keystore, sin exportar; los paquetes se firman en el teléfono.
- «Probar puente local» usa el paquete de lectura cuando existe.
- Sin cambios en el hub (Worker), el relay ni Cloudflare. Sin APK todavía.

# Gateway Hub 0.0.7 (app)

- **Puente local con Stone** (fase 1 de la migración a un solo endpoint MCP por usuario): el Escáner detecta si una
  app del mismo desarrollador ofrece el puente (Stone 1.7.16+) y muestra **Probar puente local**.
  - Hace `initialize` y `tools/list` por IPC de Android (Messenger) y solo ejecuta la herramienta que la app marca como
    prueba, si además es de solo lectura. Nunca ejecuta otra.
  - Mensajes de 200 000 caracteres como máximo, 75 s por petición, y cada fallo del canal con su motivo
    (sin puente, rechazado, conexión perdida, sin respuesta, demasiado grande, «Redirección hub» apagada…).
- Sin cambios en el hub (`stone-hub`), el relay ni Cloudflare. Con una Stone anterior el botón no aparece.

# Gateway Hub 0.0.6 (app)

- **La app crea el token del propietario (`hub_`)**: en la pantalla de entrada, «¿No tienes el token hub_?».
  1. *Crear token en Cloudflare* abre Cloudflare con el permiso Workers Scripts › Editar ya elegido.
  2. Pegas ese token y pulsas *Generar mi acceso*. La app crea un `hub_` nuevo, lo guarda como secreto `HUB_TOKEN` del
     Worker de tu hub (`stone-hub`) con la API de Cloudflare y entra sola. Ya no hace falta `wrangler secret put`.
- **Ajustes › Cloudflare:** actualizar el token de Cloudflare (p. ej. al caducar a los 7 días), ver los días que le
  quedan, generar un nuevo token del propietario u olvidarlo. Con 2 días o menos aparece un aviso en todas las pestañas.
- El token de Cloudflare se guarda cifrado y se **comparte con Stone 1.7.15** (misma firma + «Redirección hub»): si lo
  actualizas en una app, vale para las dos.
- Límite: el hub tiene que estar publicado una vez en tu cuenta. Publicarlo desde la app sigue en la Fase 2.

# Gateway Hub 0.0.5 (app)

- **Modo seguro aplicado en Stone:** Gateway envía el modo elegido a Stone 1.7.14 o posterior (al abrirse y en cada
  cambio), por el mismo canal `signature` de «Redirección hub». En Modo seguro, Stone atiende MCP solo mientras está
  en pantalla; en MCP en segundo plano, sigue funcionando con Stone cerrada.

# Gateway Hub 0.0.4 (app) · hub 0.0.3

- **Ajustes** (nueva pestaña):
  - **Entorno MCP:** *Conectar*, *Pausar* y *Desconectar del entorno*. Los aplica el hub (`POST /api/admin/environment`
    y el panel `/admin`). Pausar conserva vinculación y ajustes, pero rechaza toda llamada (`PAUSED`). Desconectar
    además invalida la sesión de Stone (la desvincula) y bloquea las llamadas (`DISCONNECTED`). Al volver a conectar,
    Stone se vincula sola si tiene «Redirección hub».
  - **Modo seguro** (por defecto: nada en segundo plano) o **MCP en segundo plano**: servicio con la notificación
    persistente «MCP activo — Gateway Hub» y botones *Pausar* y *Desconectar*. Solo funciona con la sesión iniciada
    y el entorno conectado.
  - **Notificaciones de actividad:** resumen cada 15 min, 30 min, 1 h o 3 h (herramientas, errores y agentes), solo si
    hubo actividad. Alerta inmediata si hay llamadas con error o rechazadas por permisos. Nuevo `GET /api/admin/activity?since=`.
  - **Capturas permitidas por defecto**; se bloquean solo a mano en Ajustes › Privacidad.
  - **Temas:** Noche dorada, Océano, Bosque, Rubí y Claro.
  - **Idioma:** español o inglés, elegido en la primera pantalla de la primera instalación y modificable en Ajustes.
  - **Permisos:** estado de Internet, Notificaciones y Archivos (exportar un informe de actividad sin tokens, con el
    selector de Android), y acceso a los ajustes de Android.
  - **Ayuda:** conectar una IA, conectar una app de un tercero, hacer una app compatible y qué hace cada estado.
- **Hub:** se quitan Calculadora, Notas y Utilidades. Solo queda Stone (`music_`). Los datos antiguos de notas siguen
  guardados en el Durable Object, pero ya no se usan ni se borran.

# Gateway Hub app 0.0.3

- **Sincronización automática** en el Escáner (activada por defecto, se puede apagar):
  - Al escanear, vincula solas las apps del mismo desarrollador que tengan **Redirección hub** activada
    (Stone 1.7.13).
  - También vuelve a vincular una app si su token fue revocado.
  - Botón **Vincular automáticamente** en cada app.
- Seguridad:
  - Permiso `com.zirakusa.gatewayhub.permission.LINK` de nivel `signature`.
  - Solo se pregunta a apps con la misma firma.
  - La dirección del canal (`authority`) debe pertenecer al mismo paquete que la declara.
  - Si la app responde que no, se muestra su motivo y no se envía nada al hub.

# Gateway Hub MCP 0.0.2

- **Acceso en un solo pegado:** la app y el panel web aceptan `https://<hub>#hub_…` (o solo el token). Si
  pegas un enlace o token de Stone, o el enlace de un agente, te dicen qué es y dónde va.
- **Escáner de apps compatibles** (pestaña Escáner de la app Android):
  - Encuentra las apps del teléfono que declaran `com.zirakusa.gatewayhub.action.APP`, sin el permiso de
    ver todas las apps.
  - Marca «Mismo desarrollador» solo si la firma coincide.
  - Pregunta al hub el estado de cada una (`GET /api/admin/scan`, que solo hace `tools/list`): sin vincular,
    vinculada y apagada, o en línea. **No activa nada.**
- App Android 0.0.2 (versionCode 2).

# Gateway Hub MCP 0.0.1

Primera versión, separada de Stone.

- Un solo enlace MCP para varias apps, enrutado por prefijo: `music_` (Stone), `notes_`, `calc_`, `utils_`.
- Panel `/admin` del propietario: vincular Stone, crear un enlace por IA (apps permitidas, solo lectura,
  caducidad), pausar o revocar, y métricas de 24 h y 7 días.
- **App Android 0.0.1** (`android/`): crear, copiar, pausar y revocar enlaces de agente, vincular Stone y
  ver métricas. Token del propietario cifrado con el Android Keystore, solo HTTPS, sin copias de seguridad,
  sin capturas de pantalla.
- API JSON `/api/admin/*` para la app: solo el token del propietario y solo en la cabecera, sin CORS.
- Seguridad: solo HTTPS, token del propietario como secreto de Cloudflare, enlaces de agente guardados
  como SHA-256, 120 peticiones/min, bloqueo tras 20 tokens erróneos, validación de argumentos, SQL
  parametrizado, panel sin JavaScript ni cookies.
