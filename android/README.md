# Gateway Hub para Android

App nativa (Kotlin, Jetpack Compose) del **propietario** del Gateway Hub. Hace lo mismo que el panel
web `/admin`, pero en el teléfono:

- **Agentes:** crear un enlace por IA (apps permitidas, solo lectura, duración), copiarlo una sola
  vez, y pausar, reanudar o revocar.
- **Acceso en un solo pegado:** `https://<hub>#hub_…` o solo el token. Si pegas algo de Stone o de un
  agente, la app explica qué es.
- **Vincular con token:** pega el enlace que copia la app (Modo MCP › token tipo Hub › Copiar). El token ya dice
  de qué app es (Stone, Luma, Calculadora, Filem) y responde «Stone: token vinculado».
- **Escáner (primera pestaña):** encuentra las apps compatibles instaladas, verifica que sean del mismo desarrollador y
  muestra su estado en el hub (sin vincular, apagada, en línea). Solo lee, no activa nada.
- **Lucía (asistente, icono propio):** voz o texto en español, entendido en el teléfono, para abrir apps de la familia,
  reproducir música y recomendaciones en Stone, alarmas y temporizadores, llamar y escribir (con confirmación), clima,
  agenda y «buenos días». Muestra su «cerebro» en nodos que se encienden con cada orden. Ver `assistant/`.
  La voz se reconoce **sin internet** con Vosk (`vosk-model-small-es-0.42`, licencia Apache 2.0, dentro del APK). Se
  abre con «Oye Lucía» (opcional), con el gesto de la esquina o el botón de encendido si es el asistente del teléfono.
  **Modo conteo** («vamos a contar»): cuenta lo que dices en voz alta y lo guarda como tabla en Notas (carpeta Conteos).
- **Métricas:** llamadas, errores y tiempo de respuesta en 24 h y 7 días, por agente, por app y por
  herramienta.

## Lucía

- **Sentimientos** muestra las reglas y el recorrido de la última orden. Las órdenes encadenadas conservan su orden y esperan el «sí» antes de llamar.
- **Modelos → +** permite vincular GGUF y carpetas, buscar GGUF públicos en Hugging Face y descargar Whisper Base Q5 para dictado. El audio se procesa en el teléfono. Vosk atiende «Oye Lucía» y las confirmaciones de llamadas.
- En **Ajustes de Lucía**, activa **Permitir conversación local** y escribe o di «charla» seguido del tema. Al pausar la conversación, Lucía cancela la generación y libera la memoria del modelo. Los GGUF quedan disponibles en Modelos.
- Para clientes externos, abre **MCP → Conectar un LLM externo**, crea un agente con los permisos requeridos y copia su enlace. Las órdenes locales pueden comunicarse con las apps de la familia mediante sus enlaces firmados.

La compilación requiere Android SDK 36, NDK 29.0.13113456 y CMake 3.31.6. En ARM64 CMake usa revisiones fijas de [llama.cpp](https://github.com/ggml-org/llama.cpp) y [whisper.cpp](https://github.com/ggml-org/whisper.cpp).

## Seguridad

- Habla solo con la API del propietario (`/api/admin/*`), **solo por HTTPS** (`network_security_config`
  sin excepciones). Las direcciones `http://`, con usuario o con ruta se rechazan.
- El **token del propietario** se guarda cifrado con AES-GCM y una clave del **Android Keystore**, y
  viaja solo en la cabecera `Authorization`, nunca en la URL. No se guarda hasta que el hub lo acepta.
  Si el hub lo rechaza más tarde, la sesión se cierra sola.
- Sin copias de seguridad (`allowBackup=false` y reglas de extracción vacías): el token no sale del
  teléfono.
- `FLAG_SECURE`: no se pueden hacer capturas y no aparece la vista previa en Recientes.
- Los enlaces nuevos se muestran **una vez** y no se guardan. Al copiarlos se marcan como sensibles,
  así que Android 13+ no los muestra en la vista previa del portapapeles.
- Los permisos de micrófono, contactos y llamadas se solicitan cuando la función los usa. Para el escáner, la visibilidad de paquetes se limita a las apps que
  declaran `com.zirakusa.gatewayhub.action.APP` (`<queries>`), sin `QUERY_ALL_PACKAGES`.

## Compilar y probar

```sh
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
# Prueba real opcional contra un hub: crea un agente de prueba y lo revoca al terminar
GATEWAY_HUB_URL=https://… GATEWAY_HUB_TOKEN=… ./gradlew :app:testDebugUnitTest --tests '*LiveHubTest'
```

- Las capturas de cada pantalla quedan en `app/build/screenshots/`.
- El APK de compilación sale en `app/build/outputs/apk/release/app-release-unsigned.apk`. Para publicarlo, fírmalo con la clave familiar y comprueba con `apksigner verify --print-certs` que la huella sea `34b88716ba31a8ca170624c1c009c11dd23e182148731f0889c98404b34182d4`. Verifica también `com.zirakusa.gatewayhub` y el incremento de versión antes de copiar el APK firmado a `android/GatewayHub-<versión>.apk`. [Guía de firma de la familia](https://github.com/SiriusMagic/Laucher-mcp/blob/main/GUIA-AGENTES.md).
- En un teléfono, compara Vosk y Whisper con los mismos audios de «Kusa», «Adonis Sarango», cifras y órdenes encadenadas, en silencio y con ruido. Anota contactos identificados, acciones correctas, latencia y memoria. Verifica la confirmación, el respaldo a Vosk y la actualización desde la 0.0.37.
