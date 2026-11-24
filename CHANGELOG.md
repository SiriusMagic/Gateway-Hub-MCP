# Gateway Hub 0.0.33: usar un modelo .gguf que ya tienes, sin descargarlo otra vez

- **Lucía → Avanzado → «Usar un modelo que ya tengo (.gguf)»**: eliges el archivo (Descargas, tarjeta de memoria…) y se
  usa donde está, sin copiarlo: cada modelo ocupa espacio una sola vez. Se puede «Usar» o «Quitar» (el archivo no se
  borra).
- Formato de conversación según el modelo: Qwen (ChatML, Qwen3 sin «pensar»), Gemma, Llama 3, Phi‑3/4 y Mistral; los
  demás, ChatML.
- Los modelos dentro de otra app (por ejemplo PocketPal, en su carpeta privada) no se pueden leer: Android no lo permite.

# Gateway Hub 0.0.32: intérprete del modelo (sentimientos, MCP o automático)

- **Instala `GatewayHub-0.0.32.apk`**: actualiza tu Gateway Hub y trae todo (Lucía, modelos locales, MCP). Tiene la misma
  firma que Stone 1.7.28 y el resto de la familia. «Lucía Prueba» 0.0.28 y Stone Prueba 1.7.27 están firmadas con otra
  clave: pueden quedarse instaladas aparte o desinstalarse. Ya no se publica una «Lucía Prueba» desde aquí, porque no se
  instalaría encima de la de 0.0.28.

- **Ajustes de Lucía → «Intérprete del modelo»** (con el modo Modelo o Ambos):
  - *Sentimientos* (por defecto, recomendado): el modelo traduce a órdenes de Lucía y sus reglas las hacen y preguntan «¿…?».
  - *MCP*: el modelo elige una herramienta de las apps de la familia en el teléfono (por su enlace con el Hub, sin
    internet) y sus datos. Lo que solo lee se hace al momento; lo que cambia algo se muestra en una tarjeta para confirmar.
  - *Automático*: primero órdenes de Lucía; si ninguna encaja, una herramienta MCP.
- Las herramientas siguen siendo las que cada app permite: ninguna hace fotos, graba, envía SMS ni llama.

# Gateway Hub 0.0.31: elige quién entiende lo que dices

- **Ajustes de Lucía → «Quién entiende lo que dices»**: *Asistente* (solo reglas: al instante, sin modelo), *Modelo* (el
  modelo lee todo y lo convierte en órdenes de Lucía o conversa) o *Ambos* (reglas primero, el modelo para lo que no
  entienden; por defecto). Sin un modelo elegido responde siempre el asistente.
- **«Pantalla apagada o bloqueada: solo asistente»** (activado): al oír «Oye Lucía» con el teléfono bloqueado responden
  las reglas, rápido y con poca batería.
- En modo *Modelo*, las órdenes que cambian algo siguen preguntando «¿…?» y la respuesta «sí / no» la recogen las reglas.

# Gateway Hub 0.0.30: varias órdenes en una frase, y el modelo traduce a órdenes de Lucía

- **Habla natural**: «Hey Luna, llama a Luis por favor y ayúdame con la hora» hace las dos cosas (abre el marcador con
  Luis y dice la hora) y contesta una sola vez. Sirve con «y», «luego», «después», «también» o comas. Se quitan «hey
  Luna», «por favor», «gracias», «ayúdame con», «dime»; «la hora», «la fecha», «el clima» se entienden como pregunta.
  Si un trozo no es una orden («a las 7 y media», «que compre pan y leche»), la frase queda entera.
- **Antes de cambiar algo, sigue preguntando**: si una de las órdenes pide «¿…?», las siguientes esperan a tu «sí».
- **Con un modelo descargado**: lo que las reglas no entienden, el modelo lo traduce a órdenes de Lucía (copia sus
  ejemplos: hay uno por cada herramienta, 115, y 25 de Lucía). Cada orden pasa después por las mismas reglas y
  confirmaciones; el modelo nunca llama a una herramienta por su cuenta. Si no hay ninguna orden, conversa.
- **Por voz**: si el reconocedor libre oyó varias órdenes, se usa esa frase entera.

# Gateway Hub 0.0.29: Stone Video con herramientas y órdenes, sobre la versión con modelos locales

- Parte de la rama con modelos locales (Lucía Prueba 0.0.28), ya unida a `main`. Dos APK: `GatewayHub-0.0.29.apk`
  (Gateway Hub) y `GatewayHub-0.0.29-lucia-preview.apk` (Lucía Prueba, se instala aparte).
- **MCP → Apps de este teléfono**: cada app de la familia (Stone Video, Reloj, Notas…) muestra sus herramientas, que pide
  por su enlace con el Hub en el teléfono. Si su «Redireccionar a Hub» está apagado, lo dice. El modelo local sigue
  usando solo Stone.
- **Lucía y Stone Video**: «pon el video de …», «busca el video de …», «busca … en stone video», «qué video es este»,
  «cuántos videos tengo en stone video», «pon los videos en aleatorio». «Pantalla completa», «pon la pantalla
  horizontal» y «brillo al 40 por ciento» van ahora a Stone Video.
- **«Llama a …»**: sigue lo de 0.0.26 (contactos vía Teléfono, nombres en cualquier orden y por cómo suenan).
- Lint sin avisos (contador de pulso y progreso, espacio libre para modelos, plurales).
- Compilar sin acceso a codeload.github.com: `-PllamaSourceDir=<carpeta con llama.cpp en el mismo commit>`.

# Lucía Prueba 0.0.28

- El chat abre directamente y ofrece Modelos, MCP y Ajustes desde el menú lateral.
- Un modelo descargado puede consultar la biblioteca y lo que suena en Stone Prueba con MCP local.
- Esta compilación incorpora las mejoras de contactos, Grabadora y Redes de Gateway Hub 0.0.26.
- Los APK de Lucía Prueba y Stone Prueba están juntos en [android/](android/README.md).

# Gateway Hub 0.0.26: Lucía encuentra a tus contactos y llega Grabadora

- **«Llama a …» sin permiso propio**: si Lucía no tiene el permiso de contactos, los busca a través de Teléfono (que ya lo
  tiene). Si dices que no para siempre, abre la página de la app para permitirlo.
- **Nombres parecidos**: el orden da igual («jefe Disled» = «Disled Jefe») y entiende lo que suena igual («Vox» → «Box»,
  «Jessica» → «Yesica», «Dizled» → «Disled»). Si dos contactos se parecen igual, no adivina.
- **Por voz**: los nombres de tus contactos entran en el vocabulario de Lucía (también vía Teléfono). Si el nombre no lo
  conoce, usa lo que oyó el reconocedor libre y lo compara por cómo suena.
- **Grabadora** en la familia: «mis grabaciones», «busca la grabación de la reunión», «abre la grabadora». Lucía nunca
  graba ni borra: solo abre la app; grabar es tu toque.
- **Redes**: «qué tal mi señal», «cuál es mi ping».
- Servidor: módulo `rec_` (Grabadora) para el token `grb_`.

# Gateway Hub 0.0.25: Lucía sabe usar casi todas las herramientas de la familia

- **Pregunta antes de cambiar algo**: lo que borra, edita, mueve o hace una foto se confirma con «sí» o «no» (espera 60 s;
  otra orden la cancela). Ej.: «borra la lista Gym» → «¿Borro la lista Gym? Las canciones no se borran.» → «sí».
- **Busca primero de qué hablas**: la lista, la alarma, la nota, el evento, la canción o el archivo por su nombre; «esta
  canción» es la que suena.
- **Stone**: «pon la canción despacito», «pon X a continuación», «qué sigue», «vacía la cola», «ve al minuto 2», «más
  rápido / más lento / velocidad normal», «dame 15 minutos más», «quita esta canción de favoritos», «mis canciones
  favoritas», «mis artistas / álbumes / géneros / carpetas», «busca música nueva», «ordena mis canciones por artista»,
  «muéstrame la letra», «busca la letra de esta canción», «crea una lista llamada gym», «agrega esta canción a la lista
  gym», «quita esta canción de la lista gym», «qué canciones tiene la lista gym», «renombra la lista gym a correr»,
  «borra la lista gym», «cambia el artista de esta canción a …», «deshaz el último cambio», «activa el ecualizador»,
  «modo cine / dormilón / ahorro», «activa el fundido entre canciones», «pon la pantalla horizontal», «pantalla
  completa», «brillo al 40 por ciento», «cambia el tema a negro», «cambia el color a rosa».
- **Calendario**: «pospón la reunión con Ana para el viernes a las 4», «mueve el dentista al 30», «cancela la cita del
  dentista» (Calendario 0.3.2 o posterior para posponer).
- **Cámara**: «toma una foto», «tómame una selfie» (después de tu «sí», cuenta atrás de 3 s que un toque cancela; Cámara
  0.2.2), «pon el flash de la cámara en automático», «enciende la linterna de la cámara», «temporizador de la cámara a
  10 segundos», «mis últimas fotos de la cámara».
- **Más**: Reloj («borra la alarma de las 7»), Notas («lee la nota receta», «agrega a la nota compras leche», «fija /
  borra la nota…», «crea una carpeta trabajo», «borra la tarea…»), Clima («qué temperatura hace», «quita Quito del
  clima»), SMS («lee los mensajes de Ana»), Teléfono («mis últimas llamadas»), Calculadora («convierte 5 kilómetros a
  millas», «convierte 20 dólares a euros», «borra el historial de la calculadora»), Luma («mis álbumes», «fotos de
  ayer»), Filem («borra / renombra el archivo…»), Redes («mide la velocidad de internet», «redes wifi cerca»), Brújula
  («dónde estoy»), Nivel («mis mediciones»), Inicio («qué apps tengo», «qué hay en mi dock»).
- **Mapa**: «Hora» cuelga de Reloj y «Fecha» de Calendario (Lucía las sigue diciendo aunque no estén instaladas).

# Gateway Hub 0.0.24: Música completa en Lucía, y más Calendario, SMS y otras apps

- **Música** (con las herramientas de Stone, sin internet; Stone 1.7.26 o posterior): «pon aleatorio», «quita el
  aleatorio», «repite esta canción siempre», «repite toda la lista», «no repitas», «pon la lista Rock de los 80», «¿qué
  listas tengo?», «¿qué está sonando?», «¿quién canta esta canción?», «me gusta esta canción», «apaga la música en 30
  minutos», «más graves / más agudos / ecualizador normal», «¿cuántas canciones tengo?».
- **Reloj**: «apaga la alarma de las 7», «activa la alarma de las 6 y media».
- **Calendario**: «¿qué tengo esta semana / la próxima semana / este mes?», «¿cuándo es la reunión con Ana?».
- **SMS**: «busca mensajes de banco», «mis conversaciones», «¿quién me escribió?».
- **Contactos**: «mis contactos favoritos». **Luma**: «busca fotos de la playa». **Filem**: «busca el archivo factura».
  **Cámara**: «activa / quita la marca de agua», «activa la cuadrícula».
- Todo aparece en el mapa bajo su app.

# Gateway Hub 0.0.23: «reproduce música» suena de verdad

- Antes, con un Stone anterior a 1.7.24, Lucía/Luna abría Stone y mandaba «play» demasiado pronto: se perdía y no sonaba.
  Ahora comprueba que el teléfono **está sonando**: si a los 2,5 s no suena, vuelve a dar «play» (Stone ya está listo);
  si a los 6 s sigue en silencio, lo dice («Abrí Stone, pero no empezó a sonar…») y el mapa lo marca como **Falló**,
  en vez de afirmar que está reproduciendo. Igual con «recomendaciones» y «dale play».

# Gateway Hub 0.0.22: «Vincular con token» conoce toda la familia

- Pegar el enlace o token de cualquier app ya dice cuál es: Stone Video (`stv_`), Teléfono, SMS, Reloj, Calendario,
  Clima, Cámara, QR, Redes, Brújula, Nivel, Inicio y **Notas** (que ahora sí se vincula, como `notas_`).

# Gateway Hub 0.0.21: «Conectar todas las apps» y Lucía usa las herramientas de cada app

- **Conectar todas las apps** (Hub › Escáner): un toque activa «Redireccionar a Hub» en cada app tuya instalada y la
  vincula. Dice cuántas se conectaron y cuál no pudo y por qué. Las apps deben tener su versión nueva (con «connect»);
  una anterior se vincula igual si ya tenía el interruptor encendido.
- **El hub en Cloudflare conoce toda la familia**: Stone Video (`video_`), Teléfono y Contactos (`phone_`), SMS, Reloj,
  Calendario, Notas (`notas_`, porque `notes_` son las notas del hub), Clima, Cámara, QR, Redes, Brújula, Nivel e Inicio.
  Hay que publicarlo: `npx wrangler deploy`.
- **Lucía usa las herramientas MCP de cada app, sin internet** (por el canal firmado de cada app, con sus mismas reglas):
  - Reloj: «¿cuál es mi próxima alarma?», «¿qué alarmas tengo?», «¿qué hora es en Madrid?».
  - Notas: «¿qué tareas tengo?», «agrega tarea comprar pan», «marca la tarea comprar pan como hecha», «busca en mis
    notas receta», «¿qué notas tengo?».
  - Calendario: «¿qué tengo mañana / el viernes / el 3?», «crea un evento dentista mañana a las 3 de la tarde»,
    «próximos feriados».
  - Clima: «¿va a llover mañana?», «pronóstico». Teléfono: «¿tengo llamadas perdidas?», «¿quién me llamó?», «el número de
    Ana», «¿quién cumple años esta semana?». SMS: «¿tengo mensajes sin leer?».
  - Calculadora: «¿cuánto es 25 por 4?», «calcula raíz de 81». Luma: «¿cuántas fotos tengo?». Filem: «¿cuánto espacio me
    queda?», «archivos recientes». QR: «últimos QR». Redes: «¿cómo está mi conexión?». Brújula: «¿hacia dónde estoy
    mirando?». Nivel: «¿está nivelado?».
  - Si una app no está conectada, Lucía lo dice («conecta Reloj al Hub…»).
- En el **mapa de Lucía** cada herramienta cuelga de su app (Reloj → Próxima alarma, Notas → Mis tareas…).
- El reconocimiento sin internet conoce las palabras nuevas; lo que se dice libre (una tarea, una ciudad) se toma tal cual.

# Gateway Hub (app Android) 0.0.20: Contactos dentro de Teléfono

- «Abre contactos» abre la pantalla de Contactos que ahora vive dentro de Teléfono (0.3.0). Si todavía tienes la app
  Contactos aparte y un Teléfono anterior, abre esa.

# Gateway Hub (app Android) 0.0.19: Lucía contesta llamadas, notas rápidas y linterna

- **Llamadas**: «Oye Lucía, contesta», «atiende la llamada», «cuelga», «rechaza la llamada». La primera vez pide el
  permiso de llamadas. Solo con tu voz: ninguna IA conectada por MCP puede hacerlo.
- **Nota rápida**: «anota que mañana compro pan», «apunta llamar a Ana», «crea una nota comprar leche» → nota nueva en
  Notas (carpeta Lucía).
- **Linterna**: «enciende la linterna», «apaga la linterna», «prende la luz».
- En el mapa: Teléfono → Contestar · Colgar, Notas → Conteo · Nota rápida, Lucía → Linterna.

# Gateway Hub (app Android) 0.0.18: mapa de Lucía en Ajustes y más formas de controlar la música

- El **mapa de Lucía** (el cerebro completo) pasa a **Ajustes de Lucía › Mapa de Lucía**. La pantalla de hablar queda
  limpia: conversación, caja de texto y micrófono.
- El mapa es ahora un **árbol**: Voz/Texto → app → lo que hace esa app. Cada acción cuelga de su app (Notas → Conteo,
  Stone → Música, Reloj → Alarma…); lo que Lucía responde sola (Mi día, Hora, Fecha, Volumen) cuelga de «Lucía». Están
  todas las apps de la familia.
- **Hecho / Falló** ya no son nodos unidos al camino: es una marca arriba (● Hecho en verde, ● Falló en rojo), y la
  última rama se pinta del mismo color.
- Música: «pon pausa», «pon pausa a la música», «pausa la canción», «quita / apaga la música», «reinicia la música»,
  «repite la canción», «vuelve a empezar», «otra canción», «cambia la canción», «quita la pausa», «dale play».

# Gateway Hub (app Android) 0.0.17: órdenes con volumen

- Una orden puede llevar el **volumen**: «pon música al 30 por ciento», «reproduce música de Bad Bunny al 40 %», «pon mis
  recomendaciones a todo volumen», «sigue la música con el volumen a la mitad». Lucía pone el volumen **antes** de que
  empiece a sonar, y en el cerebro se encienden los dos nodos (Música → Volumen → Stone).
- Solo el volumen: «volumen al 30», «volumen al máximo», «sube el volumen», «bájale un poco», «más bajo» (de 15 en 15).
- «Sigue la música» y «reanuda la música» ahora continúan la reproducción.
- 45 pruebas en la app.

# Gateway Hub (app Android) 0.0.16: modo conteo de Lucía

- **Modo conteo**: di «vamos a contar», «a contar» o «nuevo conteo casa Pérez». Lucía escucha sin parar (pantalla
  encendida) y cuenta lo que vas diciendo: «tomacorriente», «tomacorriente», «interruptor simple»… Un pitido confirma
  cada conteo.
- Entiende cantidades («cinco focos», «dos interruptores y un tomacorriente»), «otro» / «tres más» (lo último contado),
  palabras repetidas sin pausa («toma corriente toma corriente» = 2), singular y plural juntos, y correcciones: «quita
  uno», «quita dos focos», «borra el último» / «me equivoqué». «¿Cuántos van?» o «cuántos focos» lo dice en voz alta (sin
  contarse a sí misma). Cada línea tiene botones − y +.
- «Termina» o «Guardar en Notas» crea una nota nueva en Notas (carpeta **Conteos**) con la tabla y el total; «Abrir en
  Notas» la muestra. Si Notas no está o es anterior a 0.6.0, se puede compartir. «Cancela» solo pausa; descartar pregunta.
- Vocabulario de electricidad incluido (tomacorriente, interruptores, foco, luminaria, breaker, cajetín…); lo nuevo que
  digas se añade a la lista y se aprende para las siguientes veces.
- 42 pruebas en la app.

# Gateway Hub (app Android) 0.0.15: Lucía, voz sin internet y «Oye Lucía»

- El asistente se llama **Lucía** y tiene su icono en el cajón de apps; también se abre con el micrófono de la pantalla
  principal del hub y desde la pantalla de inicio de sesión (funciona sin conectar el hub).
- **Voz sin internet y sin Google**: reconocimiento en el teléfono con Vosk (modelo pequeño en español, Apache 2.0) en
  lugar del de Android, que en muchos teléfonos (Tecno, Infinix) no existe o necesita Google. Primero prueba con las
  palabras que conoce (órdenes, apps y nombres de tus contactos) y, si no, con texto libre.
- **«Oye Lucía»** (o «Hola Lucía»; también Luna, Nova o Kira): opcional, en ajustes. Escucha con una notificación fija,
  solo se despierta con la frase entera y gasta poco (solo analiza cuando hay sonido). Opción «solo cargando».
  Tras reiniciar el teléfono hay que abrir Lucía una vez (Android no deja encender el micrófono al arrancar).
- **Asistente del teléfono**: si eliges Lucía como asistente digital, se abre con el **gesto de la esquina inferior** o
  manteniendo el **botón de encendido**, como el de Google. Ajustes trae el botón para elegirla.
- «Mostrar sobre otras apps» (opcional) para que «Oye Lucía» abra la pantalla aunque estés en otra app.
- Entiende más formas: «tiempo», «va a llover», «pon algo de…», «qué tengo hoy».
- El APK pesa unos 60 MB porque lleva el modelo de voz. 33 pruebas en la app.

# Gateway Hub (app Android) 0.0.14: Asistente

- **Asistente** con su propio icono dentro de la app del hub: hablas o escribes y abre apps y hace cosas en la familia,
  **todo en el teléfono** (reconocimiento de voz de Android en el dispositivo, reglas en español, sin IA en la nube y sin
  escuchar en segundo plano). Responde en voz alta (se puede silenciar).
- Entiende: reproducir música (aleatoria o «de X») y **recomendaciones del día** (Stone), pausa / siguiente / anterior /
  sigue, **alarmas** («a las 7 y media», «6 de la tarde»), **temporizadores** («media hora»), **llamar** y **mandar
  mensajes** (por nombre o número; solo abre la pantalla, tú confirmas), el **clima**, tu **agenda**, «buenos días»
  (resumen del día), la hora, la fecha, foto, QR, prueba de velocidad y abrir cualquier app de la familia por su nombre.
- **El cerebro en nodos**: entrada → acción → app → resultado; cada orden enciende su camino (verde si se hizo, rojo si
  falló), los nodos más usados brillan más, y arriba dice cuántas órdenes se entendieron y se hicieron y cuánto tardan.
- Usa las órdenes estándar de Android (las mismas de cualquier asistente) y las de la familia (`FAMILIA.md` §10, permiso
  de firma `com.zirakusa.family.permission.ACT`). Se puede elegir como asistente del teléfono (botón de encendido).
- Nunca elige entre dos contactos al azar: si «Ana» puede ser varias personas y ninguna coincide, dice que no la encontró.
- 29 pruebas en la app.

# Hub 0.0.3: token del propietario fuerte

- El hub **no abre** (ni `/mcp` ni `/admin`) mientras `HUB_TOKEN` sea débil: menos de 32 caracteres, pocos caracteres
  distintos o una secuencia («abcdef…», «0123…»). Responde 503 con cómo crear uno fuerte y registra el motivo, nunca el
  token. El token del propietario es el único secreto que elige una persona, y abre el hub y todos los teléfonos que el
  hub alcanza.
- Crear uno fuerte: `openssl rand -base64 32 | npx wrangler secret put HUB_TOKEN`.
- 30 pruebas.

# Hub y Gateway 0.0.13: «Vincular con token», Escáner primero y Filem (files_)

- La app abre en **Escáner**, que ahora es la primera pestaña. Se quita la pestaña **Apps**.
- Arriba del Escáner, **Vincular con token**: pega el enlace que copia cualquier app (Modo MCP › token tipo Hub ›
  Copiar). El token ya dice de qué app es (stn_ Stone, lum_ Luma, cal_ Calculadora, fil_ Filem) y el aviso es simple:
  «Stone: token vinculado».
- Hub: **Filem** es `files_` (app remota con Modo MCP, como Luma y la Calculadora). Hay que publicar el hub
  (`npx wrangler deploy`) para que exista.
- Notas aún no se puede vincular: el hub todavía tiene su propio `notes_`.

# Hub: la Calculadora del teléfono es calc_

- `calc_` ahora es tu app **Calculadora** (Modo MCP, por el relay), como Luma es `gallery_`. Se quita la calculadora
  interna del hub, para que la IA vea una sola calculadora. Se vincula igual: «Redireccionar a Hub» en la Calculadora o
  su enlace en Apps (`/api/admin/apps/calc_`).
- Las pruebas de agentes usan `utils_` como módulo local. 28 pruebas en Node.
- Para publicarlo: `npx wrangler deploy`.

# Gateway 0.0.12 · hub con Luma (gallery_)

- **Luma (Galería) en el hub:** módulo `gallery_` para cualquier app con Modo MCP como el de Stone. Se vincula igual:
  «Redireccionar a Hub» en Luma (Gateway la vincula sola al escanear) o un token tipo Hub pegado en Apps.
- API: `POST/DELETE /api/admin/apps/<prefijo>`; el panel tiene su formulario para el enlace de Luma.
- Los tokens de las apps pueden ser `stn_` (Stone), `lum_` (Luma) u otro prefijo de 3 letras.
- Para publicarlo: `npx wrangler deploy`. 29 pruebas en Node y 23 en Android.

# Gateway 0.0.11 · hub 0.0.2 + Stone 1.7.22

- **Dos formas de conectar Stone:**
  - **Puente:** en Stone creas un token tipo **Hub**, tocas Copiar y lo pegas en Apps. El hub solo junta los enlaces;
    los permisos, la caducidad y las herramientas los pone Stone.
  - **Administrado por el hub:** en Stone activas **«Redireccionar a Hub»**. Al escanear, Gateway vincula Stone sola
    (solo apps firmadas por el mismo desarrollador) y desde ese momento los permisos se ponen aquí, en el hub.
- El panel, el escáner y `music_status` dicen quién manda en Stone, con qué token (nombre, modo) y su uso. El secreto
  nunca se muestra.
- Vuelve «Sincronización automática» y «Vincular automáticamente» en el Escáner.
- Para publicar el hub: `npx wrangler deploy`.

# Hub 0.0.2 restaurado · Gateway 0.0.10

- **Vuelve el hub de antes:** el hub solo reenvía. Cada llamada de `music_` va a Stone por el relay `stone-relay`, con
  el token `stn_` de Stone, y **todo el control vuelve a estar en Stone** (interruptor, tokens y confirmaciones).
- Para vincular: en Stone › Ajustes › Modo MCP, toca **Copiar** y pega el enlace en la pestaña Apps del hub.
- Se quitan el hub compartido, el código de 8 cifras, el canal de teléfonos, los paquetes firmados y el puente local.
- La app Gateway 0.0.10 es la 0.0.2 con número nuevo, para que se instale encima de la 0.0.9. Necesita Stone 1.7.21.
- Para publicarlo: `npx wrangler deploy` (mismo Worker `stone-hub`; se vuelve a usar `STONE_RELAY_HOST`).

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
