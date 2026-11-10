import { Resultado, MotivoNoDisponible } from '../../core/api/degradacion';
import { ConsultaRecomendacion, Recomendaciones } from '../../core/api/modelos-recomendacion';
import { ErrorApi } from '../../core/errores/error-api';

/** Lo que se envió, sin el idioma: sirve para mostrarlo en la burbuja del turista y para volver a intentarlo. */
export type ConsultaEnviada = Omit<ConsultaRecomendacion, 'idioma'>;

/** Qué dice el asistente en un mensaje. */
export type Contenido =
  | { readonly tipo: 'bienvenida' }
  | { readonly tipo: 'buscando'; readonly consulta: ConsultaEnviada }
  | { readonly tipo: 'resultado'; readonly consulta: ConsultaEnviada; readonly resultado: Recomendaciones }
  | { readonly tipo: 'no-disponible'; readonly consulta: ConsultaEnviada; readonly motivo: MotivoNoDisponible }
  | { readonly tipo: 'error'; readonly consulta: ConsultaEnviada; readonly error: ErrorApi };

/**
 * Un mensaje de la conversación. La conversación vive solo en memoria mientras la página está abierta: lo que escribe el turista
 * puede incluir datos personales, así que nunca se guarda en el navegador ni viaja en la dirección (DR-09).
 */
export type Mensaje =
  | { readonly id: number; readonly autor: 'turista'; readonly consulta: ConsultaEnviada }
  | { readonly id: number; readonly autor: 'asistente'; readonly contenido: Contenido };

export const BIENVENIDA: Mensaje = { id: 0, autor: 'asistente', contenido: { tipo: 'bienvenida' } };

/** Lo que el asistente muestra para el resultado de una consulta: las recomendaciones, el aviso de que no está disponible, o el error. */
export function contenidoDeResultado(consulta: ConsultaEnviada, resultado: Resultado<Recomendaciones>): Contenido {
  switch (resultado.estado) {
    case 'ok':
      return { tipo: 'resultado', consulta, resultado: resultado.datos };
    case 'no-disponible':
      return { tipo: 'no-disponible', consulta, motivo: resultado.motivo };
    case 'error':
      return { tipo: 'error', consulta, error: resultado.error };
  }
}

/** Cambia lo que dice el mensaje del asistente con ese `id`: de "buscando" a la respuesta, o de un aviso a "buscando" al reintentar. */
export function reemplazar(mensajes: readonly Mensaje[], id: number, contenido: Contenido): Mensaje[] {
  return mensajes.map((mensaje) => (mensaje.id === id && mensaje.autor === 'asistente' ? { ...mensaje, contenido } : mensaje));
}

/** Si hay una búsqueda en curso: mientras la hay no se envía otra. */
export function hayBusqueda(mensajes: readonly Mensaje[]): boolean {
  return mensajes.some((mensaje) => mensaje.autor === 'asistente' && mensaje.contenido.tipo === 'buscando');
}

/** Qué muestra una respuesta con recomendaciones: ninguna oferta, pocas (con el aviso del servicio) o las de siempre. */
export type VistaDeRespuesta = 'ninguna' | 'pocas' | 'completa';

export function vistaDeRespuesta(respuesta: Recomendaciones): VistaDeRespuesta {
  if (respuesta.recomendaciones.length === 0) {
    return 'ninguna';
  }
  return respuesta.aviso === 'POCAS_COINCIDENCIAS' ? 'pocas' : 'completa';
}
