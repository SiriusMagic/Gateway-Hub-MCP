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
