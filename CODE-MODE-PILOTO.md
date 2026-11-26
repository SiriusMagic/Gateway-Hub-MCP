# Cloudflare Code Mode para Stone

El piloto añade una ruta experimental para reproducir canciones por título desde **Arka Prueba**. El Worker ejecuta
un programa fijo en un Dynamic Worker con el SDK `@cloudflare/codemode`. El programa recibe el título y los ajustes
de reproducción mediante funciones limitadas. El Hub conserva las credenciales y hace las llamadas al enlace
remoto autorizado de Stone. Esta ruta requiere el token de propietario del Hub y conexión a internet.

El plan valida las coincidencias de título, conserva el artista para distinguir versiones, aplica los ajustes y
pide reproducir una pista una sola vez. Después consulta el reproductor para comprobar el ID activo. Si hay varias
coincidencias, devuelve las opciones para que Luna pregunte. Los resultados de cada paso quedan disponibles para
el diagnóstico y el Hub devuelve `Server-Timing`. El tiempo en la tarjeta de Luna incluye la ida y vuelta al Hub.

Cloudflare Code Mode cambia cómo se encadenan las operaciones. Stone sigue controlando su biblioteca y sus
permisos. Este piloto se comunica con la versión instalada a través de su relay MCP remoto. La acción local
`player_play_by_title` conserva la protección por firma.

## Hub de prueba

Dynamic Workers requiere un [plan Workers Paid](https://developers.cloudflare.com/dynamic-workers/pricing/).
Desde esta rama, con acceso a la cuenta de Cloudflare, provisiona el secreto y despliega un Worker independiente:

```sh
npx wrangler deploy --env code-preview
npx wrangler secret put HUB_TOKEN --env code-preview
```

La configuración usa `stone-hub-code-preview` y su propio Durable Object. Vincula Stone desde **Hub → Escáner →
Vincular con token** con un enlace vigente del reproductor que autorice buscar, reproducir y consultar el estado. El volumen y
el ecualizador también necesitan permiso en Stone si se piden. Vincular la compilación de prueba mediante los
botones locales protegidos por firma requiere la firma de la familia, por eso el piloto usa el enlace manual.

Desde **Arka Prueba → MCP → Crear token**, entra al Hub de prueba con su URL y token. En la pantalla MCP, activa
**Cloudflare Code Mode · Stone**. Con Stone abierto, prueba **«Reproduce la canción Alone»** por texto y luego por voz.
También puedes probar un título inexistente y dos pistas con el mismo título para comprobar la respuesta. El
interruptor controla las órdenes con título. Whisper, Vosk y el resto de órdenes conservan sus opciones.
