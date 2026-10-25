/**
 * Posición de la foto que se ve después de avanzar (1) o retroceder (-1) en el visor. La galería es circular: tras
 * la última viene la primera, y antes de la primera está la última.
 */
export function indiceCircular(actual: number, desplazamiento: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return (((actual + desplazamiento) % total) + total) % total;
}
