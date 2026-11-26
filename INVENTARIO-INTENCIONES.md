# Reconocimiento de intenciones — inventario de acciones

Este mapa sirve para revisar qué puede hacer Luna desde Arka en Android. El nombre del asistente sigue el ajuste existente. La interfaz muestra **Reconocimiento de intenciones** y conserva la clave interna `FEELINGS` para interpretar configuraciones anteriores.

| App | Mapa | Catálogo que puede leer Arka |
| --- | --- | --- |
| Stone música | [Acciones y contratos](docs/intenciones/stone.md) · [Controles por archivo y línea](docs/intenciones/controles-stone.md) | [Stone 1.0.0](android/app/src/main/resources/intent-actions/stone.v1.json) |
| Arka | [Entradas y ejecución](#arka) | El intérprete local conserva `Action`, `IntentPlan` y `Skills`. |
| Las otras 18 apps de la familia | [Mapa inicial por app](docs/intenciones/familia.md) | [Familia 1.0.0](android/app/src/main/resources/intent-actions/familia.v1.json) |

El `schemaVersion` del catálogo describe su formato y `catalogVersion` identifica sus datos. Cada ID permanece estable al mover o rediseñar un control. `controles` y `fuente` apuntan a las líneas de una revisión concreta del repositorio. Los archivos JSON se empaquetan como recursos de Arka y se pueden leer por classpath. El inventario es evidencia para el intérprete. Las invocaciones genéricas usan la herramienta descubierta en `tools/list` y validan su esquema. El piloto conoce el contrato y hace una llamada directa. Una fila marcada `adaptacion` o `pendiente` requiere trabajo o verificación.

## Arka

- `arka.intent.map.open` · navegación. Menú lateral → pantalla `RsScreen`. Consulta las frases conocidas, permite buscar y abrir la app asociada. Conserva el modo seleccionado. [AssistantWorkspace.kt](android/app/src/main/java/com/zirakusa/gatewayhub/assistant/AssistantWorkspace.kt) · [RsScreen.kt](android/app/src/main/java/com/zirakusa/gatewayhub/assistant/RsScreen.kt)
- `arka.intent.execute` · consulta o cambio, según la acción. Chat y micrófono → `AssistantState.send` → `Intents`, `Parser`, `IntentPlan` → `Brain` y `AndroidHands`. Frases compuestas conservan resultados y dependencia por acción. Ejemplo «Calcula dos más dos y pon música». [AssistantScreen.kt](android/app/src/main/java/com/zirakusa/gatewayhub/assistant/AssistantScreen.kt) · [AssistantState.kt](android/app/src/main/java/com/zirakusa/gatewayhub/assistant/AssistantState.kt)
- `arka.intent.feedback` · cambio. Botones de voto por resultado, panel de corrección y alias de contacto. Guarda evidencia privada; compartir es una preferencia separada. Ejemplo «Corrige “Kuri”» abre una selección explícita desde el panel. [FeedbackPanel.kt](android/app/src/main/java/com/zirakusa/gatewayhub/assistant/FeedbackPanel.kt)
- `arka.mcp.local` · configuración. Ajustes de MCP y estado de apps del teléfono. «Permitir MCP local» habilita ejecución a través de proveedores firmados; el enlace para clientes externos se gestiona aparte. [PhoneMcpScreen.kt](android/app/src/main/java/com/zirakusa/gatewayhub/assistant/PhoneMcpScreen.kt) · [PhoneMcp.kt](android/app/src/main/java/com/zirakusa/gatewayhub/assistant/local/PhoneMcp.kt)

## Piloto de Stone en una llamada

El [estado de migración a MCP local](docs/intenciones/migracion-mcp-local.md) cruza los controles con la función
compartida y la herramienta, incluida la parte pendiente de cada app.

La intención de Arka `stone.player.play_by_title` invoca `player_play_by_title` en el proveedor firmado `content://com.zirakusa.stone.kotlin.preview.hublink`, método `tool`, JSON-RPC `tools/call`.

```kotlin
reproducirPorTitulo(
    titulo = "New Americana",
    volumen = 50,
    ecualizador = true,
    segundoPlano = true
)
```

| Parámetro | Contrato del piloto |
| --- | --- |
| `titulo` | Obligatorio. Título o fragmento de título de una pista de audio, de 1 a 200 caracteres normalizados. |
| `volumen` | Entero opcional de 0 a 100. Stone aplica su propio control y devuelve el porcentaje logrado según los pasos físicos de Android. |
| `ecualizador` | Booleano opcional. Stone ajusta el efecto antes de iniciar. Si el modo batería o la ruta impide activarlo, puede reproducir y devolver un fallo parcial. |
| `segundoPlano` | Booleano opcional, por defecto `false`. Arka intenta volver al asistente cuando Stone confirma la pista. Stone mantiene su servicio de reproducción. |
| `idPista` | Opcional. Solo para confirmar una opción devuelta en una búsqueda ambigua del mismo título. |

Stone comprueba primero el reproductor y busca solo en títulos de audio. Prioriza el título exacto, permite una coincidencia parcial única y devuelve hasta 20 opciones con `total` si hay varias. Espera a que el motor informe el ID de la pista y reproducción activa. Puede devolver `playing` o `partial` con la pista confirmada, `ambiguous` con coincidencias, o `failed` con los códigos de `steps`. Un fallo de volumen detiene el inicio y permite preguntar si se acepta el volumen actual. Ejemplos de códigos: `TRACK_NOT_FOUND`, `INVALID_SELECTION`, `VOLUME_REJECTED`, `EQ_BATTERY_MODE`, `EQ_NOT_ACTIVE`, `PLAYBACK_UNCONFIRMED`.

La rama base **Stone 1.7.30** (`versionCode 64`) admite `library_search`, `equalizer_set_enabled`, `player_play_songs` y `player_state`. El piloto requiere las herramientas locales `player_play_by_title` y `player_play_collection` para ejecutar esas órdenes completas. `tools/list` permite comprobar si la versión instalada las ofrece. Ante un fallo de transporte, Luna informa que la ejecución queda incierta. Cuando Stone indica que está cerrado o que el reproductor aún no está listo, Luna puede abrirlo y reintentar esa respuesta previa al cambio.

La biblioteca y el MCP reutilizan el catálogo de canciones. Los botones y el servicio local pasan por `PlaybackViewModel.play`, `setEqualizerEnabled` y el mismo ajuste de volumen de `StonePlaybackService`. [Implementación en Stone](https://github.com/SiriusMagic/Stone/blob/codex/inventario-intenciones/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/automation/actions/PlayByTitle.kt) · [Cliente Arka](android/app/src/main/java/com/zirakusa/gatewayhub/assistant/StoneTitleAction.kt)

## Órdenes compuestas frecuentes

| Frase | Acciones y decisiones |
| --- | --- |
| «Calcula dos más dos y pon música en segundo plano New Americana con el volumen a 50 y el ecualizador activo» | `calc_evaluate(expression="2+2")` → `player_play_by_title` por MCP local. Cada app devuelve su resultado. |
| «Reanuda la música al 30 y para en 25 minutos» | Ajustar volumen y reanudar pista actual → programar temporizador de Stone. Preguntar qué reproducir si la cola está vacía. |
| «Retrocede diez segundos y pausa» | Leer ID y posición de la pista → buscar posición relativa acotada → pausar. Revalidar ID antes de cambiar posición. Orden por adaptar. |
| «Vuelve al inicio de esta canción y pon repetición individual» | Buscar posición cero conservando el ID → activar repetición de una pista. Enlace del reinicio estricto pendiente. |
| «Añade New Americana a Gimnasio y reproduce la lista» | Resolver IDs → añadir la pista → reproducir la lista si se completó la modificación. Pedir selección si hay canciones o listas homónimas. Orden por adaptar. |
| «Busca la letra de esta canción y ábrela» | Consultar letra local → ofrecer búsqueda en línea si falta → mostrar hoja si se selecciona. Abrir hoja y guardar letra son decisiones separadas. |

## Cómo continuar el inventario por repo

1. Lee la guía de la familia, `FAMILIA.md` y las instrucciones de la revisión que se está inventariando. Anota el commit, el paquete y el proveedor del manifiesto.
2. Parte de cada pantalla, diálogo, menú, widget, notificación y gesto. Anota archivo, línea y callback, sigue el estado hasta su handler y busca entradas equivalentes del servicio local. Enumera también las capacidades sin control visible.
3. Reutiliza un ID al llegar a la misma operación. Registra parámetros, resultado real, permisos, confirmación, estado previo y disponibilidad para Luna. Para consultas ambiguas pide el dato que falta. Para cambios, presenta selección y revisión antes de efectos sensibles.
4. Clasifica las vistas puramente visuales como tales. Para identidad y aspecto diferencia nombre de paquete, etiqueta del launcher, icono, acceso directo, colores, tema y archivos de imagen.
5. Compara con `tools/list` en un teléfono con ambas APK de la familia instaladas. Escribe en el mapa qué verificaste con código o pruebas y qué queda pendiente en el dispositivo.

## Cobertura

Stone tiene un mapa de **88 acciones cruzadas** más **24 declaraciones MCP** por completar con controles o frases. El índice de controles localiza **539 referencias estáticas** de Stone, incluidas filas marcadas «Por cruzar». Para las otras 18 apps se consultaron 710 archivos de código y se localizaron 1.211 referencias de UI y 196 declaraciones del servicio. El mapa de cada app conserva sus enlaces y las frases de Arka documentadas. El seguimiento individual de todos sus controles, los gestos de sistema y el comportamiento real de las APK quedan pendientes. La rama piloto no incluye cambios de nombre de paquete o lanzamiento de una versión.
