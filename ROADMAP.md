# Hoja de ruta: Gateway Hub + Stone

Objetivo: que **cualquiera**, programe o no, conecte su IA con sus apps **sin buscar tokens, sin copiar y
pegar y sin depender de la cuenta de Cloudflare de otra persona**.

## Aclaraciones

- **Ningún túnel caduca.** El relay y el hub son Workers permanentes de Cloudflare. Lo que caduca es el
  enlace de un agente, si al crearlo eliges una duración (7, 30 o 90 días…).
- Hoy todo usa la cuenta de Cloudflare del autor. Eso está bien para él, pero no para otras personas: cada
  uno debe usar **la suya** (plan gratuito).

## Fase 1: Redirección hub ✅ (Stone 1.7.13 · Hub app 0.0.3)

- Stone › Modo API › Gateway Hub › **Redirección hub**. Con el interruptor activado, el **Escáner** de la
  app del hub vincula Stone solo.
- Protocolo seguro en el mismo teléfono, con tres cerrojos:
  1. un permiso de Android de nivel `signature` (solo apps firmadas con la misma clave);
  2. Stone vuelve a comprobar la firma de quien llama;
  3. el interruptor del usuario.
- El enlace viaja de Stone a la app del hub y de ahí al hub por HTTPS. No pasa por el portapapeles ni por
  ningún chat.

## Fase 2: «Mi Cloudflare» (instalación en un toque)

1. En la app del hub, el botón **Crear mi clave de Cloudflare** abre `dash.cloudflare.com` con la plantilla
   de permisos ya rellenada («Editar Workers»). El usuario solo toca *Crear* y *Copiar*.
2. El usuario pega esa clave **una sola vez**. Se guarda cifrada con el Android Keystore.
3. La app publica en **su** cuenta el relay y el hub, con la API de Cloudflare. Los scripts van dentro del
   APK.
4. La app **genera el token del propietario** y lo guarda como secreto del Worker. El usuario nunca lo ve.
5. Con Redirección hub, Stone recibe la dirección de **su** relay. Hace falta que la dirección del relay
   en Stone sea configurable, no fija en la compilación.
6. Sección **MCP › Cloudflare** en Stone, para quien no instale el hub: la misma instalación, pero solo
   del relay.

## Fase 3: El hub manda

- **Permisos por herramienta** de cada agente, desde el hub. Stone aplica lo que el hub le pide dentro del
  token reservado.
- **Modo «Solo hub»** en Stone: se ocultan los tokens directos y la Analítica local. Todo se gestiona en el
  hub, y Stone solo recibe y cumple las peticiones.
- **Tokens de Stone para terceros:** tokens especiales para conectar Stone a otro hub.
- **Apps secundarias:** añadir al hub apps que no están en este teléfono, mediante un enlace (como hoy) o
  un código QR.

## Fuera de alcance por ahora

- Monetización y cuentas centralizadas. Cada persona usa su propia cuenta gratuita.
