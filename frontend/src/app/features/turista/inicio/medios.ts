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

/** [SUPUESTO] Las fotos de Experiencias aún no existen: `null` = degradado de respaldo. Ejemplo: `${BASE}exp-buceo.webp`. */
export const FOTO_DE_EXPERIENCIA: Readonly<Record<ClaveExperiencia, string | null>> = {
  buceo: null,
  trekking: null,
  aves: null,
  playas: null,
  cafe: null,
};

/**
 * [SUPUESTO, avisar a backend] El contrato de zonas no trae imagen. Mientras tanto, la foto se busca aquí por id de zona.
 * Una zona que no esté en el mapa (por ejemplo, una que cree el administrador) o con `null` conserva su degradado.
 */
export const FOTO_DE_ZONA: Readonly<Record<string, string | null>> = {
  a00000000000000000000001: null, // Centro Histórico → zona-centro.webp
  a00000000000000000000002: null, // El Rodadero y Gaira → zona-rodadero.webp
  a00000000000000000000003: null, // Taganga → zona-taganga.webp
  a00000000000000000000004: null, // Zona sur hotelera → zona-sur.webp
  a00000000000000000000005: null, // Parque Tayrona → zona-tayrona.webp
  a00000000000000000000006: null, // Troncal del Caribe → zona-troncal.webp
  a00000000000000000000007: null, // Minca y Sierra Nevada → zona-minca.webp
};
