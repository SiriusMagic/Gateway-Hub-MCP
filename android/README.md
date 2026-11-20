# Gateway Hub para Android (0.0.2)

App nativa (Kotlin, Jetpack Compose) del **propietario** del Gateway Hub. Hace lo mismo que el panel
web `/admin`, pero en el teléfono:

- **Agentes:** crear un enlace por IA (apps permitidas, solo lectura, duración), copiarlo una sola
  vez, y pausar, reanudar o revocar.
- **Acceso en un solo pegado:** `https://<hub>#hub_…` o solo el token. Si pegas algo de Stone o de un
  agente, la app explica qué es.
- **Escáner:** encuentra las apps compatibles instaladas, verifica que sean del mismo desarrollador y
  muestra su estado en el hub (sin vincular, apagada, en línea). Solo lee, no activa nada.
- **Apps:** ver el estado de cada app y vincular o desvincular Stone (pegando el enlace de Stone ›
  Modo MCP › Copiar).
- **Métricas:** llamadas, errores y tiempo de respuesta en 24 h y 7 días, por agente, por app y por
  herramienta.

APK listo para instalar: [`GatewayHub-0.0.12.apk`](GatewayHub-0.0.12.apk) (`com.zirakusa.gatewayhub`).

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
