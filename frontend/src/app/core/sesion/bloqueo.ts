import { LOCALE, idiomaActual } from '../idioma/idioma';
import { traducir } from '../idioma/traducir';

const MINUTO_MS = 60_000;

/**
 * El aviso cuando la cuenta se bloquea por cinco intentos fallidos seguidos (RF-05): cuánto falta y a qué hora se
 * puede intentar de nuevo, en el idioma vigente. `bloqueadaHasta` es la fecha ISO que da el núcleo. Si ya pasó o no se
 * entiende, es un mensaje general.
 */
export function mensajeDeBloqueo(bloqueadaHasta: string, ahora: number): string {
  const hasta = Date.parse(bloqueadaHasta);
  if (Number.isNaN(hasta) || hasta <= ahora) {
    return traducir('errores.bloqueo.general');
  }
  const minutos = Math.ceil((hasta - ahora) / MINUTO_MS);
  const espera = traducir('errores.bloqueo.espera', { n: minutos });
  const { texto, esDeHoy } = hora(hasta, ahora);
  return traducir(esDeHoy ? 'errores.bloqueo.hoy' : 'errores.bloqueo.otroDia', {
    espera,
    hora: texto,
  });
}

/** La hora local; si es de otro día, también el día, para que no se confunda con la de hoy. */
function hora(momento: number, ahora: number): { texto: string; esDeHoy: boolean } {
  const fecha = new Date(momento);
  const esDeHoy = fecha.toDateString() === new Date(ahora).toDateString();
  const formato = new Intl.DateTimeFormat(
    LOCALE[idiomaActual()],
    esDeHoy
      ? { hour: 'numeric', minute: '2-digit' }
      : { day: 'numeric', month: 'long', hour: 'numeric', minute: '2-digit' },
  );
  return { texto: formato.format(fecha), esDeHoy };
}
