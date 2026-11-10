import { SolicitudCola } from '../../core/api/modelos-nucleo';
import { plazoRestante } from '../turista/solicitudes/plazo';

/**
 * Qué tan pronto hay que atender una solicitud de la cola (RF-36): `urgente` con 6 horas o menos para atenderla, `vencida`
 * si el plazo ya terminó (el núcleo la cierra enseguida y ya no se puede atender), `en-plazo` en cualquier otro caso.
 */
export type NivelUrgencia = 'urgente' | 'en-plazo' | 'vencida';

type ParaUrgencia = Pick<SolicitudCola, 'estado' | 'venceEn' | 'proximaAVencer'>;

/**
 * La urgencia de una solicitud en este momento. Solo las pendientes tienen plazo para el operador; las demás (el
 * historial) no son urgentes. Además de `proximaAVencer` (que la API calcula al responder) se mira el reloj, para que una
 * solicitud que se vuelve urgente con la pantalla abierta se resalte sin recargar.
 */
export function nivelDeUrgencia(solicitud: ParaUrgencia, ahora: number): NivelUrgencia {
  if (solicitud.estado !== 'PENDIENTE') {
    return 'en-plazo';
  }
  const plazo = plazoRestante(solicitud.venceEn, ahora);
  if (plazo.nivel === 'vencido') {
    return 'vencida';
  }
  return solicitud.proximaAVencer || plazo.nivel === 'urgente' ? 'urgente' : 'en-plazo';
}

const RANGO: Readonly<Record<NivelUrgencia, number>> = { urgente: 0, 'en-plazo': 1, vencida: 2 };

const vencimiento = (solicitud: SolicitudCola): number =>
  solicitud.venceEn ? Date.parse(solicitud.venceEn) : Number.POSITIVE_INFINITY;

/**
 * La cola ordenada por urgencia: primero las urgentes, luego las que están en plazo y al final las vencidas. Dentro de
 * cada grupo, la que vence antes va antes; si vencen a la vez, la que llegó antes. No modifica la lista original.
 */
export function ordenarPorUrgencia(
  solicitudes: readonly SolicitudCola[],
  ahora: number,
): SolicitudCola[] {
  return [...solicitudes].sort(
    (a, b) =>
      RANGO[nivelDeUrgencia(a, ahora)] - RANGO[nivelDeUrgencia(b, ahora)] ||
      vencimiento(a) - vencimiento(b) ||
      Date.parse(a.creadaEn) - Date.parse(b.creadaEn),
  );
}

/** Un código corto para citar la solicitud en una llamada o un mensaje (las primeras letras de su identificador). */
export function codigoCorto(id: string): string {
  return id.slice(0, 8).toUpperCase();
}
