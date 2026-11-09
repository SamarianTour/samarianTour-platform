import { EstadoSolicitud, TransicionSolicitud } from '../../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../../core/idioma/tabla-de-textos';
import type { PasoSolicitud } from '../../../shared/pasos-solicitud/pasos-solicitud';

/** Los nueve estados de una solicitud (DN-09), en el orden del ciclo de vida. */
export const ESTADOS_SOLICITUD: readonly EstadoSolicitud[] = [
  'PENDIENTE',
  'ATENDIDA',
  'CONFIRMADA',
  'NO_DISPONIBLE',
  'SIN_RESPUESTA',
  'DECLINADA',
  'PAGO_FALLIDO',
  'PAGADA',
  'VENCIDA',
];

/** El nombre de cada estado en el idioma vigente. Cada tabla exige los nueve: si el contrato agrega uno, el build falla. */
export const ETIQUETA_ESTADO = tablaDeTextos<EstadoSolicitud>({
  PENDIENTE: 'solicitudes.estado.PENDIENTE',
  ATENDIDA: 'solicitudes.estado.ATENDIDA',
  CONFIRMADA: 'solicitudes.estado.CONFIRMADA',
  NO_DISPONIBLE: 'solicitudes.estado.NO_DISPONIBLE',
  SIN_RESPUESTA: 'solicitudes.estado.SIN_RESPUESTA',
  DECLINADA: 'solicitudes.estado.DECLINADA',
  PAGO_FALLIDO: 'solicitudes.estado.PAGO_FALLIDO',
  PAGADA: 'solicitudes.estado.PAGADA',
  VENCIDA: 'solicitudes.estado.VENCIDA',
});

/** Qué significa cada estado para quien envió la solicitud. */
export const DESCRIPCION_ESTADO = tablaDeTextos<EstadoSolicitud>({
  PENDIENTE: 'solicitudes.estadoDesc.PENDIENTE',
  ATENDIDA: 'solicitudes.estadoDesc.ATENDIDA',
  CONFIRMADA: 'solicitudes.estadoDesc.CONFIRMADA',
  NO_DISPONIBLE: 'solicitudes.estadoDesc.NO_DISPONIBLE',
  SIN_RESPUESTA: 'solicitudes.estadoDesc.SIN_RESPUESTA',
  DECLINADA: 'solicitudes.estadoDesc.DECLINADA',
  PAGO_FALLIDO: 'solicitudes.estadoDesc.PAGO_FALLIDO',
  PAGADA: 'solicitudes.estadoDesc.PAGADA',
  VENCIDA: 'solicitudes.estadoDesc.VENCIDA',
});

/** Quién hizo cada cambio del historial, visto por el turista. */
export const ETIQUETA_ACTOR = tablaDeTextos<TransicionSolicitud['actor']>({
  TURISTA: 'solicitudes.actor.TURISTA',
  OPERADOR: 'solicitudes.actor.OPERADOR',
  SISTEMA: 'solicitudes.actor.SISTEMA',
  PAGOS: 'solicitudes.actor.PAGOS',
});

/** El color de la etiqueta de cada estado: una clase de las etiquetas globales (siempre con texto, no solo color). */
export const CLASE_DE_ESTADO_SOLICITUD: Readonly<Record<EstadoSolicitud, string>> = {
  PENDIENTE: 'etiqueta--aviso',
  ATENDIDA: 'etiqueta--exito',
  CONFIRMADA: 'etiqueta--info',
  NO_DISPONIBLE: 'etiqueta--neutra',
  SIN_RESPUESTA: 'etiqueta--neutra',
  DECLINADA: 'etiqueta--neutra',
  PAGO_FALLIDO: 'etiqueta--peligro',
  PAGADA: 'etiqueta--exito',
  VENCIDA: 'etiqueta--aviso',
};

/** Las solicitudes que siguen su curso; las demás son los cinco estados terminales del contrato. */
const ACTIVAS: ReadonlySet<EstadoSolicitud> = new Set<EstadoSolicitud>([
  'PENDIENTE',
  'ATENDIDA',
  'CONFIRMADA',
  'PAGO_FALLIDO',
]);

export type FiltroSolicitudes = 'todas' | 'activas' | 'finalizadas';
export const FILTROS_SOLICITUDES: readonly FiltroSolicitudes[] = [
  'todas',
  'activas',
  'finalizadas',
];

export function estaActiva(estado: EstadoSolicitud): boolean {
  return ACTIVAS.has(estado);
}

/** Si la solicitud entra en el filtro de la lista. */
export function coincideConFiltro(estado: EstadoSolicitud, filtro: FiltroSolicitudes): boolean {
  return filtro === 'todas' || (filtro === 'activas') === estaActiva(estado);
}

/** Lo que el turista puede hacer a continuación con una solicitud. */
export type AccionSolicitud =
  /** Confirmar o declinar las condiciones (solo atendida, dentro del plazo). */
  | 'responder'
  /** La oferta no se logró: puede volver a pedirla para otra fecha. */
  | 'volver-a-solicitar'
  /** Ya la pagó: puede dejar su opinión en la ficha de la oferta. */
  | 'opinar'
  /** Sigue su curso: solo puede ver la oferta. */
  | 'ver-oferta';

/**
 * La acción principal de cada estado. El pago de una solicitud confirmada o con el pago fallido lo agrega la rama de
 * pagos; hasta entonces esos estados solo ofrecen ver la oferta.
 */
export function accionPrincipal(estado: EstadoSolicitud): AccionSolicitud {
  switch (estado) {
    case 'ATENDIDA':
      return 'responder';
    case 'NO_DISPONIBLE':
    case 'SIN_RESPUESTA':
    case 'VENCIDA':
      return 'volver-a-solicitar';
    case 'PAGADA':
      return 'opinar';
    default:
      return 'ver-oferta';
  }
}

/** El historial de la más antigua a la más reciente, sin modificar el original. */
export function ordenarHistorial(historial: readonly TransicionSolicitud[]): TransicionSolicitud[] {
  return [...historial].sort((a, b) => Date.parse(a.en) - Date.parse(b.en));
}

/**
 * En qué paso del flujo (solicitar → confirmar → pagar → listo) está una solicitud. Las que terminaron sin pagarse
 * (no disponible, sin respuesta, declinada, vencida) ya no avanzan en el flujo y no tienen paso.
 */
export function pasoDe(estado: EstadoSolicitud): PasoSolicitud | null {
  switch (estado) {
    case 'PENDIENTE':
      return 'solicitar';
    case 'ATENDIDA':
      return 'confirmar';
    case 'CONFIRMADA':
    case 'PAGO_FALLIDO':
      return 'pagar';
    case 'PAGADA':
      return 'listo';
    default:
      return null;
  }
}
