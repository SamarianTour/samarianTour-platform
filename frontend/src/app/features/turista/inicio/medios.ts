/**
 * Los medios del Inicio (video, fotos de tarjetas, de experiencias y de zonas), todos en un solo lugar. Los archivos viven
 * en `frontend/public/inicio/` y se sirven desde `/inicio/...`.
 *
 * Un medio en `null` todavía no existe: la pantalla muestra el degradado de respaldo. Cuando se agregue el archivo, solo
 * se cambia esa línea (ver "Medios del Inicio" en `frontend/README.md`).
 */
const BASE = '/inicio/';

export const HERO_VIDEO = `${BASE}hero.mp4`;
export const HERO_POSTER = `${BASE}hero-poster.jpg`;

/** Foto de cada tarjeta de "Encuentra tu plan", por tipo de oferta. */
export const FOTO_DE_TIPO = {
  ACTIVIDAD: `${BASE}tipo-actividades.webp`,
  PAQUETE: `${BASE}tipo-paquetes.webp`,
  ALOJAMIENTO: `${BASE}tipo-alojamiento.webp`,
  RESTAURANTE: `${BASE}tipo-restaurantes.webp`,
} as const;

/** Las cinco columnas del acordeón de Experiencias. */
export type ClaveExperiencia = 'buceo' | 'trekking' | 'aves' | 'playas' | 'cafe';

/** Fotos de Experiencias. `null` = todavía no hay foto y se muestra el degradado de respaldo. */
export const FOTO_DE_EXPERIENCIA: Readonly<Record<ClaveExperiencia, string | null>> = {
  buceo: `${BASE}exp-buceo.webp`,
  trekking: `${BASE}exp-trekking.webp`,
  aves: `${BASE}exp-aves.webp`,
  playas: `${BASE}exp-playas.webp`,
  cafe: `${BASE}exp-cafe.webp`,
};

/**
 * [SUPUESTO, avisar a backend] El contrato de zonas no trae imagen. Mientras tanto, la foto se busca aquí por id de zona.
 * Una zona que no esté en el mapa (por ejemplo, una que cree el administrador) o con `null` conserva su degradado.
 */
export const FOTO_DE_ZONA: Readonly<Record<string, string | null>> = {
  a00000000000000000000001: `${BASE}zona-centro.webp`, // Centro Histórico
  a00000000000000000000002: `${BASE}zona-rodadero.webp`, // El Rodadero y Gaira
  a00000000000000000000003: `${BASE}zona-taganga.webp`, // Taganga
  a00000000000000000000004: `${BASE}zona-sur.webp`, // Zona sur hotelera
  a00000000000000000000005: `${BASE}zona-tayrona.webp`, // Parque Tayrona
  a00000000000000000000006: `${BASE}zona-troncal.webp`, // Troncal del Caribe
  a00000000000000000000007: `${BASE}zona-minca.webp`, // Minca y Sierra Nevada
};
