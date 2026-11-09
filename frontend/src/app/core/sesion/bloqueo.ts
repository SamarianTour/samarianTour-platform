const MINUTO_MS = 60_000;

/**
 * El aviso cuando la cuenta se bloquea por cinco intentos fallidos seguidos (RF-05): cuánto falta y a qué hora se
 * puede intentar de nuevo. `bloqueadaHasta` es la fecha ISO que da el núcleo. Si ya pasó o no se entiende, es un
 * mensaje general.
 */
export function mensajeDeBloqueo(bloqueadaHasta: string, ahora: number): string {
  const hasta = Date.parse(bloqueadaHasta);
  const general =
    'Tu cuenta está bloqueada por intentos fallidos. Inténtalo de nuevo en unos minutos.';
  if (Number.isNaN(hasta) || hasta <= ahora) {
    return general;
  }
  const minutos = Math.ceil((hasta - ahora) / MINUTO_MS);
  const espera = minutos === 1 ? 'en 1 minuto' : `en ${minutos} minutos`;
  return `Tu cuenta está bloqueada por intentos fallidos. Podrás intentarlo de nuevo ${espera}, a las ${hora(hasta, ahora)}.`;
}

/** La hora local; si es de otro día, también el día, para que no se confunda con la de hoy. */
function hora(momento: number, ahora: number): string {
  const fecha = new Date(momento);
  const mismoDia = fecha.toDateString() === new Date(ahora).toDateString();
  const formato = new Intl.DateTimeFormat(
    'es-CO',
    mismoDia
      ? { hour: 'numeric', minute: '2-digit' }
      : { day: 'numeric', month: 'long', hour: 'numeric', minute: '2-digit' },
  );
  return formato.format(fecha);
}
