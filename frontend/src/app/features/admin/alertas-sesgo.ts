import { AlertaSesgo, PrestadorAdmin } from '../../core/api/modelos-nucleo';

/** El índice a partir del cual la recomendación abre una alerta de sesgo (RET-02). Solo informa; lo calcula y aplica el núcleo. */
export const UMBRAL_INDICE_SESGO = 1.5;

/** Las alertas de la de mayor índice a la de menor; a igual índice, la actualizada más recientemente primero. No modifica la lista. */
export function ordenarPorIndice(alertas: readonly AlertaSesgo[]): AlertaSesgo[] {
  return [...alertas].sort(
    (a, b) => b.indice - a.indice || Date.parse(b.actualizadaEn) - Date.parse(a.actualizadaEn),
  );
}

/**
 * El nombre del prestador de una alerta. La alerta trae solo su identificador, así que el nombre sale de la lista de prestadores
 * [SUPUESTO]: la razón social del RNT, que viene vacía mientras no se encuentre. Devuelve null si no se puede saber.
 */
export function nombreDelPrestador(
  prestadorId: string,
  prestadores: readonly PrestadorAdmin[],
): string | null {
  const razonSocial = prestadores.find((prestador) => prestador.id === prestadorId)?.razonSocial?.trim();
  return razonSocial ? razonSocial : null;
}

/** Un código corto para nombrar al prestador cuando no se conoce su razón social (las primeras letras de su identificador). */
export function codigoDelPrestador(prestadorId: string): string {
  return prestadorId.slice(0, 8).toUpperCase();
}
