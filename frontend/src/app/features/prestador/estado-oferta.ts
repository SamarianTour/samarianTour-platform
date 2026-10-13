import { OfertaPrestador, VersionOferta } from '../../core/api/modelos';

/** Estado de una oferta tal como lo ve su prestador. Se calcula: la API no lo envía. */
export type EstadoOferta = 'PUBLICADA' | 'EN_MODERACION' | 'RECHAZADA' | 'PUBLICADA_CON_CAMBIOS' | 'RETIRADA';

export const ESTADO_OFERTA: Record<EstadoOferta, string> = {
  PUBLICADA: 'Publicada',
  EN_MODERACION: 'En moderación',
  RECHAZADA: 'Rechazada',
  PUBLICADA_CON_CAMBIOS: 'Publicada con cambios en revisión',
  RETIRADA: 'Retirada',
};

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
export function estadoDe(oferta: OfertaPrestador): EstadoOferta {
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
export function motivoDeRechazo(oferta: OfertaPrestador): string | undefined {
  return oferta.pendiente?.estadoModeracion === 'RECHAZADA' ? oferta.pendiente.motivoRechazo : undefined;
}

/**
 * Una oferta retirada no se edita: el retiro es definitivo (DC-09).
 * [SUPUESTO] El formulario de edición se llena con la ficha pública, así que hace falta una versión publicada:
 * una oferta que solo tiene versión pendiente no se puede editar desde aquí.
 */
export function sePuedeEditar(oferta: OfertaPrestador): boolean {
  return !oferta.retirada && !!oferta.publicada;
}
