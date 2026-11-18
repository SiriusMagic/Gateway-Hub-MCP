# Gateway Hub MCP: reglas para agentes

- Lee `ARQUITECTURA.md` y, para crear una app, `MODULES.md`. Abre solo los archivos que necesites.
- Pruebas: `npm test` (debe pasar todo). Nunca desactives ni saltes una prueba.
- Cada módulo nuevo lleva su prueba en `test/`, con al menos: lista de herramientas, una llamada
  correcta, una con argumentos malos y el caso de "no encontrado".
- Prohibido: `eval`, `Function()`, SQL concatenado, HTML sin `esc()`, `fetch` a URLs que mande la
  IA, y secretos en el código o en los registros (logs).
- Publicar: `npx wrangler deploy`. El token del propietario se cambia con
  `npx wrangler secret put HUB_TOKEN`.
- No cambies `name` en `wrangler.toml` sin avisar: cambia la dirección pública y el almacenamiento.
