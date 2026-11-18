# Gateway Hub MCP 0.0.1

Primera versión, separada de Stone.

- Un solo enlace MCP para varias apps, enrutado por prefijo: `music_` (Stone), `notes_`, `calc_`, `utils_`.
- Panel `/admin` del propietario: vincular Stone, crear un enlace por IA (apps permitidas, solo lectura,
  caducidad), pausar o revocar, y métricas de 24 h y 7 días.
- Seguridad: solo HTTPS, token del propietario como secreto de Cloudflare, enlaces de agente guardados
  como SHA-256, 120 peticiones/min, bloqueo tras 20 tokens erróneos, validación de argumentos, SQL
  parametrizado, panel sin JavaScript ni cookies.
