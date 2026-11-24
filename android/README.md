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

APK listo para instalar: [`GatewayHub-0.0.17.apk`](GatewayHub-0.0.17.apk) (`com.zirakusa.gatewayhub`).

## Conversación local en Lucía

Al abrir Gateway Hub sin conexión configurada se abre Lucía. Desde **Lucía › Ajustes › Administrar Gateway Hub** puedes entrar al panel del propietario.

1. Abre Lucía › Ajustes › **Modelos locales** en un Android ARM64. Descarga **Qwen3 4B** (2,50 GB, recomendado para la primera prueba en un teléfono de 8 GB de RAM o más) o **Qwen2.5 1.5B** (1,12 GB). Las descargas se comprueban con SHA-256 antes de activar el modelo.
2. Instala una voz de síntesis en español sin conexión si Lucía lo indica. En modo avión, toca el micrófono y prueba «hoy me siento cansado, ¿qué me sugieres?». Puedes elegir un tono en los ajustes. Empieza la frase con «charla» para enviarla directamente al modelo. El historial de la conversación se mantiene en memoria mientras Lucía está abierta.
3. Prueba «abre Stone», «pausa la música» y «reanuda la música». Las órdenes del teléfono y el MCP local de apps compatibles siguen pasando por el asistente de Android.
4. Para usar el Hub por internet, crea un enlace dedicado en **Agentes** y pégalo en Lucía › Ajustes › **Gateway Hub MCP**. Prueba «busca mis notas de viaje en el Hub». Los cambios requieren confirmación en pantalla. El enlace se guarda cifrado y solo se envía al Hub configurado.

La compilación de la rama experimental requiere Android SDK 36, NDK 29.0.13113456 y CMake 3.31.6. CMake descarga una revisión fija de [llama.cpp](https://github.com/ggml-org/llama.cpp) para la variante ARM64.

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
- Único permiso: `INTERNET`. Para el escáner, la visibilidad de paquetes se limita a las apps que
  declaran `com.zirakusa.gatewayhub.action.APP` (`<queries>`), sin `QUERY_ALL_PACKAGES`.

## Compilar y probar

```sh
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assemblePreview
# Prueba real opcional contra un hub: crea un agente de prueba y lo revoca al terminar
GATEWAY_HUB_URL=https://… GATEWAY_HUB_TOKEN=… ./gradlew :app:testDebugUnitTest --tests '*LiveHubTest'
```

- Las capturas de cada pantalla quedan en `app/build/screenshots/`.
- El APK instalable sale en `app/build/outputs/apk/preview/app-preview.apk`. Usa la misma
  configuración que Stone (AGP 8.13.2, Kotlin 2.3.21, para que R8 entienda los metadatos), está
  firmado con la clave de depuración de la máquina y su `versionCode` empieza en 1.
