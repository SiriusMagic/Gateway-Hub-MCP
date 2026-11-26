# Migración a MCP local

Luna traduce las órdenes reconocidas a argumentos estructurados con Reconocimiento de intenciones. El contrato de ejecución de cada app se publica en su proveedor local MCP. La pantalla y la herramienta deben reutilizar el mismo servicio, caso de uso o método del controlador. Un permiso de Android, una confirmación en la app y un fallo de transporte se conservan como resultados distintos. Se puede abrir la pantalla que inicializa un servicio, pero la llamada MCP ejecuta la orden.

Estado de las filas: **Migrada** indica que la ruta de Luna y el contrato se comprobaron en código y pruebas automatizadas. **Disponible** indica herramienta declarada con revisión individual pendiente. **Pendiente** indica que falta un contrato, trazar un control o verificar cuál es la función compartida. **Visual** identifica un elemento de renderizado. Las pruebas en un teléfono con ambas APK de la familia siguen pendientes.

## Stone

| Control | Función compartida | Herramienta local | Estado |
| --- | --- | --- | --- |
| Pausa y reanudación en reproductor, horizontal, mini y letras | `PlaybackViewModel.toggle` a través de `AutomationBridge`; el MCP consulta el estado antes de alternar | `player_pause` / `player_play` | Migrada |
| Siguiente en reproductor, horizontal, panel de biblioteca y gesto de pista | `PlaybackViewModel.next` a través de `AutomationBridge` | `player_next` | Migrada |
| Anterior en los mismos controles y el widget | `PlaybackViewModel.previous` y la operación Media3 `seekToPreviousMediaItem` en el widget | `player_previous` | Migrada |
| Barra de progreso y salto de letras, para ir al inicio | `PlaybackViewModel.seek(0)` | `player_seek(positionSeconds=0)` | Migrada |
| Salto por segundos en el reproductor de video | `PlaybackViewModel.seek` con posición relativa limitada a la duración | Contrato relativo por completar | Pendiente |
| Seleccionar canción en biblioteca y ajustes de volumen y ecualizador | `PlaybackViewModel.play`, `setEqualizerEnabled` y el ajuste de volumen del servicio | `player_play_by_title(titulo, volumen?, ecualizador?, segundoPlano?, idPista?)` | Migrada |
| Reproducción de biblioteca y mezcla diaria de Stone | `VoicePlay.shuffled` / `VoicePlay.daily` y `PlaybackViewModel.play` | `player_play_collection(coleccion, volumen?, ecualizador?, segundoPlano?)` | Migrada |
| Degradado, avatar renderizado, animación de portada e indicadores pasivos | Composición de UI | Sin operación | Visual |

Los enlaces a pantallas, controles, gestos y ajustes, así como las operaciones de biblioteca, letras, cola, ecualizador y apariencia, están en el [mapa de Stone](stone.md) y el [índice por archivo](controles-stone.md). El índice conserva las referencias por cruzar. Navegación, selectores y cambios de imágenes permanecen pendientes de un contrato propio con intervención del usuario donde corresponda.

## Familia

El [inventario por app](familia.md) y el [catálogo versionado](../../android/app/src/main/resources/intent-actions/familia.v1.json) contienen las revisiones, referencias de los controles y declaraciones/handlers MCP. La siguiente tabla identifica el siguiente cruce que queda por completar en cada app.

| App | Control → función compartida o destino que se debe comprobar → herramienta | Estado |
| --- | --- | --- |
| Redes | Prueba de velocidad → motor de prueba → `network_speed_test`; verificar que la confirmación y el consumo sean los mismos | Disponible |
| Brújula | Pantalla de rumbo → lectura del sensor y permiso → herramientas de brújula | Disponible |
| Cámara | Disparador y selfie → captura con cuenta atrás y permiso → falta una herramienta de captura; `camera_shot(id)` consulta una foto existente | Pendiente |
| Calculadora | Igual → `Calculator.evaluate` → `calc_evaluate(expression)` | Migrada para expresiones; quedan cobertura del teclado, conversiones e historial |
| Calendario | Agenda y editor → repositorio de eventos → herramientas de calendario; verificar fecha y confirmaciones | Disponible |
| Clima | Resumen y ciudad → consulta del proveedor → herramientas de clima; quedan rutas de resumen y ubicación | Disponible |
| Filem | Buscar, abrir, mover y borrar → operaciones sobre documentos → herramientas de archivos; comprobar permisos SAF | Disponible |
| Luma | Abrir imagen y administrar álbumes → catálogo y operaciones multimedia → herramientas de galería; comprobar selectores | Disponible |
| Grabadora | Iniciar y detener grabación → servicio de grabación y permiso de micrófono → falta herramienta de control; `rec_list` y `rec_get` consultan grabaciones | Pendiente |
| Inicio | Abrir app → resolución de actividad → falta herramienta de apertura; `launcher_apps` consulta el catálogo | Pendiente |
| Nivel | Herramientas del sensor → lectura y calibración → herramientas de nivel | Disponible |
| Notas | Guardar, buscar y administrar carpetas → repositorio de notas → herramientas de notas; quedan atajos de conteo | Disponible |
| QR | Botón Escanear → cámara y resultado del lector → contrato de escaneo; comprobar permiso y retorno | Pendiente |
| Reloj | Guardar alarma → `AlarmStore.save` y `AlarmScheduler.sync` → `clock_alarm_create(time)` | Migrada para crear; temporizador, cronómetro y ajustes pendientes |
| SMS | Redactar y enviar → flujo de mensajería → herramientas de SMS; verificar destinatario, consentimiento y resultado | Disponible |
| Stone Video | Controles del reproductor de video → su propio controlador → herramientas de video; portar y verificar a partir de Stone | Pendiente |
| Teléfono y Contactos | Buscar contacto, marcar, contestar y colgar → repositorio y servicio de llamadas → herramientas de teléfono; conservar permisos y confirmación | Pendiente |
| Tienda | Instalar, actualizar y abrir → instalación y consentimiento del sistema → proveedor MCP propio por implementar | Pendiente |
| Terceros, por ejemplo Spotify | Lanzar y controlar reproducción → API o proveedor autorizado del desarrollador → contrato MCP disponible de la app | Pendiente |

Para marcar una fila como migrada, seguir el control hasta la función que produce el cambio, ejecutar la herramienta con los mismos argumentos y comprobar el mismo estado observable. Probar también falta de permiso, cancelación y errores. Retirar la ruta de Luna anterior después de comprobar el reemplazo. Los componentes que solo muestran estado se anotan en el índice de cada app y no requieren herramienta.
