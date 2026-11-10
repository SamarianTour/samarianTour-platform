import { EstadoSolicitud } from '../../core/api/modelos-nucleo';
import { ESTADOS_SOLICITUD } from '../turista/solicitudes/estado-solicitud';

/**
 * Los estados que se pueden consultar en el historial del operador. Sin `estado` la API devuelve la cola (las pendientes),
 * así que el historial es cada uno de los demás estados; no existe una vista "todos" en el contrato.
 */
export const ESTADOS_HISTORIAL: readonly EstadoSolicitud[] = ESTADOS_SOLICITUD.filter(
  (estado) => estado !== 'PENDIENTE',
);

/** El estado que se abre al entrar al historial: las que el operador atendió y esperan la respuesta del turista. */
export const ESTADO_INICIAL_HISTORIAL: EstadoSolicitud = 'ATENDIDA';

/**
 * El estado que pide la dirección (`?estado=`). Un valor desconocido, de la cola (`PENDIENTE`) o ausente no llega a la API:
 * se usa el inicial, para que una dirección escrita a mano no provoque un error 400.
 */
export function estadoDelHistorial(valor: string | null): EstadoSolicitud {
  return ESTADOS_HISTORIAL.find((estado) => estado === valor) ?? ESTADO_INICIAL_HISTORIAL;
}
