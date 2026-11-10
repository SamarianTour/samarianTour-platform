import { EstadoModeracion, OfertaPrestador, VersionOferta } from '../../core/api/modelos';
import { tablaDeTextos } from '../../core/idioma/tabla-de-textos';

/**
 * Lo único que hace falta saber de una oferta para calcular su estado: si está retirada y qué versiones tiene. Lo cumplen tanto el
 * resumen de la lista (`OfertaPrestador`) como la oferta con el contenido completo (`OfertaPropia`, DC-50).
 */
export interface OfertaConVersiones {
  readonly retirada: boolean;
  readonly publicada?: object;
  readonly pendiente?: { readonly estadoModeracion: EstadoModeracion; readonly motivoRechazo?: string };
}

/** Estado de una oferta tal como lo ve su prestador. Se calcula: la API no lo envía. */
export type EstadoOferta = 'PUBLICADA' | 'EN_MODERACION' | 'RECHAZADA' | 'PUBLICADA_CON_CAMBIOS' | 'RETIRADA';

/** El nombre de cada estado en el idioma vigente (ver `core/idioma/etiquetas.i18n.ts`). */
export const ESTADO_OFERTA = tablaDeTextos<EstadoOferta>({
  PUBLICADA: 'etiquetas.estadoOferta.PUBLICADA',
  EN_MODERACION: 'etiquetas.estadoOferta.EN_MODERACION',
  RECHAZADA: 'etiquetas.estadoOferta.RECHAZADA',
  PUBLICADA_CON_CAMBIOS: 'etiquetas.estadoOferta.PUBLICADA_CON_CAMBIOS',
  RETIRADA: 'etiquetas.estadoOferta.RETIRADA',
});

/** Color de la etiqueta de cada estado: una clase de las etiquetas globales. */
export const CLASE_DE_ESTADO: Record<EstadoOferta, string> = {
  PUBLICADA: 'etiqueta--exito',
  EN_MODERACION: 'etiqueta--aviso',
  RECHAZADA: 'etiqueta--peligro',
  PUBLICADA_CON_CAMBIOS: 'etiqueta--info',
  RETIRADA: 'etiqueta--neutra',
};

/**
 * Calcula el estado con `retirada`, `publicada` y `pendiente`.
 * [SUPUESTO] Cuando una oferta cumple más de una definición, manda este orden: retirada; versión pendiente
 * rechazada; publicada con una edición en moderación; primera versión en moderación; publicada.
 */
export function estadoDe(oferta: OfertaConVersiones): EstadoOferta {
  if (oferta.retirada) {
    return 'RETIRADA';
  }
  if (oferta.pendiente?.estadoModeracion === 'RECHAZADA') {
    return 'RECHAZADA';
  }
  if (oferta.pendiente) {
    return oferta.publicada ? 'PUBLICADA_CON_CAMBIOS' : 'EN_MODERACION';
  }
  return oferta.publicada ? 'PUBLICADA' : 'EN_MODERACION';
}

/** La versión que se muestra en la lista: la pendiente si existe; si no, la publicada. */
export function versionVigente(oferta: OfertaPrestador): VersionOferta | undefined {
  return oferta.pendiente ?? oferta.publicada;
}

/** El motivo del rechazo de la versión pendiente, si fue rechazada. */
export function motivoDeRechazo(oferta: OfertaConVersiones): string | undefined {
  return oferta.pendiente?.estadoModeracion === 'RECHAZADA' ? oferta.pendiente.motivoRechazo : undefined;
}

/**
 * Una oferta retirada no se edita: el retiro es definitivo (DC-09). Cualquier otra sí, tenga o no versión publicada: con el
 * contenido completo de sus versiones (DC-50) también se corrige la primera versión mientras espera moderación o una que fue
 * rechazada.
 */
export function sePuedeEditar(oferta: OfertaConVersiones): boolean {
  return !oferta.retirada && !!(oferta.publicada ?? oferta.pendiente);
}
