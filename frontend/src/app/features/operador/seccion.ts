export type SeccionOperador = 'cola' | 'historial' | 'avisos';

function empiezaEn(ruta: string, prefijo: string): boolean {
  return ruta === prefijo || ruta.startsWith(`${prefijo}/`);
}

/**
 * Qué parte del menú lateral se marca para la dirección dada. Atender una solicitud (`/operador/solicitudes/:id`)
 * sigue contando como la cola, porque desde ahí se llega y a ella se vuelve.
 */
export function seccionDelOperador(url: string): SeccionOperador {
  const ruta = url.split(/[?#]/)[0];
  if (empiezaEn(ruta, '/operador/historial')) {
    return 'historial';
  }
  if (empiezaEn(ruta, '/operador/avisos')) {
    return 'avisos';
  }
  return 'cola';
}
