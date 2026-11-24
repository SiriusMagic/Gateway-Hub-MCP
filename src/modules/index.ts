// The list of apps the hub offers. To add one: create src/modules/<app>/index.ts following
// ./types.ts (see ../../MODULES.md) and add it here. Nothing else changes.
import { type HubModule } from "./types.ts";
import { musicModule } from "./music/index.ts";
import { notesModule } from "./notes/index.ts";
import { utilsModule } from "./utils/index.ts";
import { calculatorAppModule, familyModules, filesAppModule, galleryModule } from "./remote/index.ts";

// calc_ is the Calculadora app on the phone (it replaced the hub's own calculator, so an AI sees one calculator).
// The rest of the family follows (Teléfono, SMS, Reloj, Calendario, Notas, Clima, Cámara, QR, Redes, Brújula, Nivel, Inicio).
export const modules: HubModule[] = [musicModule, galleryModule, calculatorAppModule, filesAppModule, ...familyModules, notesModule, utilsModule];
