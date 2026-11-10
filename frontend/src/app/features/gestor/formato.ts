import { LOCALE, idiomaActual } from '../../core/idioma/idioma';
import { traducir } from '../../core/idioma/traducir';

/**
 * Una cantidad de solicitudes tal como se muestra. El núcleo no entrega los conteos de 1 o 2 (RET-03, DN-41): llegan sin valor y se
 * escriben "menos de 3". Un cero sí es un cero. Los miles se separan según el idioma: 1.234 en español y 1,234 en inglés.
 */
export function formatoCantidad(cantidad: number | null | undefined): string {
  if (cantidad === null || cantidad === undefined) {
    return traducir('gestor.menosDeTres');
  }
  return cantidad.toLocaleString(LOCALE[idiomaActual()]);
}
