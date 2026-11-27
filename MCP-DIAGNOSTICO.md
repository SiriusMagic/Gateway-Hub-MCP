# Reconocimiento de intenciones y diagnóstico MCP

## Luna y Stone

La entrada escrita y la transcripción final usan el mismo intérprete. «Pon New Americana al 50 %» se convierte en
una sola llamada local a Stone. El volumen viaja como número y Stone lo aplica antes de reproducir. La petición
no requiere un token de Cloudflare ni consulta al modelo de conversación.

```json
{
  "jsonrpc": "2.0",
  "id": "7ff7419e-069c-4450-be51-305779a066f4",
  "method": "tools/call",
  "params": {
    "name": "player_play_by_title",
    "arguments": { "titulo": "New Americana", "volumen": 50 }
  }
}
```

Stone admite `titulo` de hasta 200 caracteres, `volumen` entre 0 y 100, `ecualizador` y `segundoPlano` como
booleanos e `idPista` para elegir una coincidencia. El resultado incluye el estado, los pasos, la pista confirmada
o las coincidencias disponibles. Luna ofrece «opción 1», «opción 2», etc. cuando el título es ambiguo. Si Stone
no responde en cinco segundos o no confirma el inicio, Luna muestra «Resultado sin confirmar». La reproducción
puede haberse iniciado. Luna no repite esa llamada automáticamente.

## Comprobar una conexión

1. En la app de destino, abre **Ajustes › Modo MCP**. Para una prueba local, activa **Permitir MCP local** en
   la app y en **Arka › MCP › General**. Android comprueba que ambas APK estén firmadas por el mismo desarrollador.
2. Abre **Arka › MCP › Diagnóstico MCP**, elige la app y pulsa **MCP local**. La prueba llama a
   `player_state` en Stone y a `ping` en las otras apps.
3. Para probar por internet, deja **Redireccionar a Hub** encendido, vincula la app al Hub desde el escáner
   y pulsa **Por Hub**. El Hub usa el enlace guardado y reenvía el ID JSON-RPC de la prueba a la app.
4. Compara el ID en ambos teléfonos o pantallas. Stone y Stone Video muestran la última petición rastreada
   en **Modo MCP**. Las otras apps muestran el ID de la última prueba recibida en ese mismo panel.

**Redireccionar a Hub** viene activo cuando la preferencia falta. Un valor guardado se conserva. El relay admite
la redirección con **Activar MCP** apagado. La app sirve la credencial de Arka solo con la redirección encendida.
**Permitir MCP local** tiene su propia preferencia y autorización por firma. Los tokens de agentes se siguen
administrando en Arka. Algunas versiones intermedias ya eliminaron preferencias antiguas y sus valores
no se pueden recuperar a partir de esas instalaciones.

El diagnóstico de Luna muestra por separado la última transcripción, la última intención con sus argumentos cuando usa la herramienta de Stone,
el resultado de MCP y los fallos recientes. El detalle con texto está disponible en la sesión. Se conserva
el último fallo con metadatos y se le puede adjuntar un comentario privado en el teléfono.

Los informes automáticos empiezan desactivados. Al habilitarlos, una cola privada y el programador de Android
reintentan únicamente el envío del informe cuando vuelve la conexión. Envían ID, app, herramienta, etapa,
código, versión y duración. El Hub exige el token del propietario, valida los campos y agrupa los fallos de
los últimos 30 días por app, herramienta, etapa y código. La transcripción, el título, los argumentos,
los tokens y los comentarios quedan fuera del informe. La pestaña **Analítica** muestra esos grupos.

## Dictado

El transcriptor recibe audio PCM a través de `UtteranceTranscriber`. Los modelos se bajan desde Ajustes de
voz, con revisión fijada y verificación SHA-256. Se conserva la opción de Vosk y la selección guardada.

| Opción | Variante multilingüe | Tamaño |
| --- | --- | ---: |
| Whisper Base (predeterminada entre Whisper) | Base Q5_1 | 59 707 625 bytes |
| Whisper Small (opcional) | Small Q3_K | 113 846 889 bytes |

Small tiene 1,91 veces el tamaño del archivo de Base. Se verificaron su integridad, carga y transcripción
con whisper.cpp en Linux. Para comparar en Android, instala las dos variantes, elige **Comparar ambos** y
dicta el mismo conjunto de títulos. Cada muestra se entrega a ambos motores y Luna ejecuta una vez con
Base, con respaldo de Vosk. El panel muestra el texto de cada motor y la latencia estimada desde el fin
de voz detectado. La estimación suma la espera del detector y el tiempo de ese motor, y excluye la espera
causada por ejecutar el otro modelo. Si se elige cada variante por separado, el panel muestra su tiempo directo.

Para comprobar títulos en tu teléfono, prueba «Pon New Americana al cincuenta por ciento», «Reproduce Alone»
y «Reproduce Esta cobardía». Compara el título transcrito y el que llegó en `titulo`, y revisa el tiempo de
cada variante en **Diagnóstico MCP**.

## Ramas de la familia

Todas las ramas de revisión se llaman `codex/mcp-diagnostico` y parten del `main` correspondiente.

| Aplicación | Base de la rama | Referencia de los controles |
| --- | --- | --- |
| [Gateway Hub MCP](https://github.com/SiriusMagic/Gateway-Hub-MCP/tree/codex/mcp-diagnostico) | `9a46dcf` | Dictado de prueba en `86562ea` |
| [Stone](https://github.com/SiriusMagic/Stone/tree/codex/mcp-diagnostico) | `cf2f095` | `081b5c5` |
| [Brújula](https://github.com/SiriusMagic/Br-jula-mcp.../tree/codex/mcp-diagnostico) | `c5c92abf` | `c7f1e0e1` |
| [Cámara](https://github.com/SiriusMagic/C-mara-mcp/tree/codex/mcp-diagnostico) | `68ece458` | `9375b7ea` |
| [Calculadora](https://github.com/SiriusMagic/Calculadora.mcp/tree/codex/mcp-diagnostico) | `f060de6b` | `e563cd0d` |
| [Calendario](https://github.com/SiriusMagic/Calendario-mcp/tree/codex/mcp-diagnostico) | `1d9a5ade` | `1d75c326` |
| [Clima](https://github.com/SiriusMagic/Clima-mcp/tree/codex/mcp-diagnostico) | `d1842b5b` | `a57f844f` |
| [Filem](https://github.com/SiriusMagic/Filem.mcp/tree/codex/mcp-diagnostico) | `bfab6f8b` | `1b93c084` |
| [Luma](https://github.com/SiriusMagic/Galer-a-mcp/tree/codex/mcp-diagnostico) | `8dae3042` | `41732689` |
| [Grabadora](https://github.com/SiriusMagic/Grabadora-mcp/tree/codex/mcp-diagnostico) | `f267c9e3` | `4a32b779` |
| [Inicio](https://github.com/SiriusMagic/Laucher-mcp/tree/codex/mcp-diagnostico) | `60282aae` | `5c3af546` |
| [Nivel](https://github.com/SiriusMagic/Nivel-burbuja-mcp/tree/codex/mcp-diagnostico) | `e2ed6a1f` | `0f498c82` |
| [Notas](https://github.com/SiriusMagic/Notas.mcp/tree/codex/mcp-diagnostico) | `f3f8ae5c` | `3c771469` |
| [QR](https://github.com/SiriusMagic/Qr-mcp/tree/codex/mcp-diagnostico) | `af824d6e` | `37c7f361` |
| [Redes](https://github.com/SiriusMagic/Analizador-de-redes-mcp/tree/codex/mcp-diagnostico) | `7ec887a2` | `d5ee509d` |
| [Reloj](https://github.com/SiriusMagic/Reloj-mcp/tree/codex/mcp-diagnostico) | `f507ef22` | `991b6112` |
| [SMS](https://github.com/SiriusMagic/SMS/tree/codex/mcp-diagnostico) | `943b5611` | `0bb599a7` |
| [Stone Video](https://github.com/SiriusMagic/Stone-v-deo-mcp/tree/codex/mcp-diagnostico) | `1d8ba94a` | `7c55f614` |
| [Teléfono y Contactos](https://github.com/SiriusMagic/Tel-fono.mcp/tree/codex/mcp-diagnostico) | `5a7e4d6f` | `d41132d9` |

Tienda distribuye las APK. Su repositorio carece de proveedor MCP propio. Contactos usa el servicio de Teléfono.
