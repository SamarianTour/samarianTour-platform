import { EstadoSolicitud } from '../../../core/api/modelos-nucleo';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { Plazo } from './plazo';

/** "1 d 2 h", "2 h 30 min" o "45 min": solo las dos unidades más grandes, que es lo que importa para decidir. */
export function duracionTexto(plazo: Plazo): string {
  if (plazo.dias > 0) {
    return traducir('solicitudes.plazo.dias', { dias: plazo.dias, horas: plazo.horas });
  }
  if (plazo.horas > 0) {
    return traducir('solicitudes.plazo.horas', { horas: plazo.horas, minutos: plazo.minutos });
  }
  return traducir('solicitudes.plazo.minutos', { minutos: plazo.minutos });
}

/**
 * El aviso de plazo de una solicitud según qué se espera de quién: el operador mientras está pendiente, el turista
 * mientras decide y luego mientras paga. Sin plazo (solicitud cerrada) no hay aviso.
 */
export function avisoDePlazo(estado: EstadoSolicitud, plazo: Plazo): string | null {
  if (plazo.nivel === 'sin-plazo') {
    return null;
  }
  if (plazo.nivel === 'vencido') {
    return traducir('solicitudes.plazo.vencido');
  }
  const clave: ClaveTexto =
    estado === 'PENDIENTE'
      ? 'solicitudes.plazo.pendiente'
      : estado === 'ATENDIDA'
        ? 'solicitudes.plazo.atendida'
        : 'solicitudes.plazo.pagar';
  return traducir(clave, { tiempo: duracionTexto(plazo) });
}
