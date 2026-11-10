import { TipoOferta, Zona } from '../../../core/api/modelos';
import { Tablero } from '../../../core/api/modelos-nucleo';

/** Una fila de la demanda: las solicitudes de una zona y un tipo de oferta. `null` es un conteo de 1 o 2 que el núcleo no entrega. */
export interface FilaDemanda {
  /** Identifica la fila: una zona aparece una vez por cada tipo de oferta. */
  readonly clave: string;
  readonly zonaId: string;
  readonly tipoOferta: TipoOferta;
  readonly solicitudes: number | null;
}

/**
 * Las filas del tablero, en el orden en que llegan: primero las combinaciones con más solicitudes. No se reordenan ni se
 * "completan": un conteo omitido sigue omitido (RET-03, DN-41).
 */
export function filasDeDemanda(tablero: Tablero): FilaDemanda[] {
  return tablero.filas.map((fila) => ({
    clave: `${fila.zonaId}|${fila.tipoOferta}`,
    zonaId: fila.zonaId,
    tipoOferta: fila.tipoOferta,
    solicitudes: fila.solicitudes ?? null,
  }));
}

export interface CifrasDemanda {
  /** Todas las solicitudes del periodo; `null` si son 1 o 2. */
  readonly total: number | null;
  readonly zonas: number;
  readonly tiposDeOferta: number;
}

/**
 * Las cifras de arriba de la pantalla. El total es el que entrega el núcleo: no se suman las filas, porque las que tienen 1 o 2
 * solicitudes no traen su conteo y la suma saldría de menos. Las zonas y los tipos sí se cuentan de las filas: una fila existe
 * aunque su conteo se omita.
 */
export function cifrasDeDemanda(tablero: Tablero): CifrasDemanda {
  return {
    total: tablero.total ?? null,
    zonas: new Set(tablero.filas.map((fila) => fila.zonaId)).size,
    tiposDeOferta: new Set(tablero.filas.map((fila) => fila.tipoOferta)).size,
  };
}

/** La mayor cantidad que sí se conoce, para dar escala a las barras; 0 si ninguna se conoce. */
export function maximoConocido(filas: readonly FilaDemanda[]): number {
  return filas.reduce((mayor, fila) => Math.max(mayor, fila.solicitudes ?? 0), 0);
}

/** El ancho de la barra de un conteo omitido: una marca corta y rayada, para que se vea que hay algo pero no cuánto. */
export const ANCHO_MENOS_DE_TRES = 8;

/** El ancho mínimo de una barra con solicitudes, para que se vea aunque sea mucho menor que la mayor. */
const ANCHO_MINIMO = 3;

/** El ancho de una barra, de 0 a 100, relativo a la mayor. Un conteo omitido lleva la marca corta; un cero, nada. */
export function anchoDeBarra(solicitudes: number | null, maximo: number): number {
  if (solicitudes === null) {
    return ANCHO_MENOS_DE_TRES;
  }
  if (solicitudes <= 0 || maximo <= 0) {
    return 0;
  }
  return Math.min(100, Math.max(ANCHO_MINIMO, Math.round((solicitudes / maximo) * 100)));
}

/** Lo que se sabe de una zona por la lista pública de zonas; sin ella (inactiva, por ejemplo) solo se tiene su id. */
export interface DatosDeZona {
  readonly nombre: string | null;
  readonly areaProtegida: boolean;
  readonly altaAfluencia: boolean;
}

/**
 * El tablero trae solo el `zonaId`; el nombre y las marcas de zona sensible salen del catálogo. La lista pública solo trae las zonas
 * activas: de una inactiva no hay nombre y la pantalla muestra su id.
 */
export function datosDeZona(zonaId: string, zonas: readonly Zona[]): DatosDeZona {
  const zona = zonas.find((una) => una.id === zonaId);
  return {
    nombre: zona?.nombre ?? null,
    areaProtegida: zona?.areaProtegida ?? false,
    altaAfluencia: zona?.altaAfluencia ?? false,
  };
}
