import { CambioComision, CuentaInterna } from '../../core/api/modelos-nucleo';

/** El porcentaje de comisión va de 0 a menos de 100, con hasta dos decimales (RF-82, DN-23). */
export const COMISION_MAXIMA_EXCLUSIVA = 100;

export type ErrorDePorcentaje = 'requerido' | 'rango' | 'decimales' | 'igual';

/**
 * Valida el porcentaje nuevo como lo hace el núcleo, para avisar antes de enviar: es obligatorio, está entre 0 y menos de 100 y tiene
 * hasta dos decimales. Además se rechaza uno igual al vigente: no cambiaría nada. Devuelve el problema, o null si se puede enviar.
 */
export function validarPorcentaje(
  valor: number | null | undefined,
  vigente: number | null | undefined,
): ErrorDePorcentaje | null {
  if (valor === null || valor === undefined || Number.isNaN(valor)) {
    return 'requerido';
  }
  if (!Number.isFinite(valor) || valor < 0 || valor >= COMISION_MAXIMA_EXCLUSIVA) {
    return 'rango';
  }
  // Se compara en centésimas con una tolerancia, porque 5.55 * 100 no da exactamente 555 en coma flotante.
  if (Math.abs(valor * 100 - Math.round(valor * 100)) > 1e-6) {
    return 'decimales';
  }
  if (vigente !== null && vigente !== undefined && Math.round(valor * 100) === Math.round(vigente * 100)) {
    return 'igual';
  }
  return null;
}

/** El porcentaje que se envía: con exactamente las centésimas escritas, sin los restos de la coma flotante. */
export function porcentajeParaEnviar(valor: number): number {
  return Math.round(valor * 100) / 100;
}

/** Cómo quedaría un pago de ejemplo con una comisión: lo que se descuenta al prestador y lo que recibe. */
export interface EjemploDeComision {
  readonly precioBase: number;
  readonly comision: number;
  readonly prestador: number;
}

/** El precio de ejemplo con el que se explica el efecto de la comisión. */
export const PRECIO_DE_EJEMPLO = 100_000;

/**
 * Un ejemplo aproximado: la comisión es el porcentaje del precio base y se descuenta al prestador, no se le suma al turista (RF-53,
 * RF-49). [SUPUESTO] Se redondea al peso; el redondeo exacto lo hace el núcleo al dispersar el pago. Null si el porcentaje no es válido.
 */
export function ejemploDeComision(
  porcentaje: number | null | undefined,
  precioBase = PRECIO_DE_EJEMPLO,
): EjemploDeComision | null {
  if (porcentaje === null || porcentaje === undefined || !(porcentaje >= 0 && porcentaje < COMISION_MAXIMA_EXCLUSIVA)) {
    return null;
  }
  const comision = Math.round((precioBase * porcentaje) / 100);
  return { precioBase, comision, prestador: precioBase - comision };
}

/** El historial del cambio más reciente al más antiguo, sin modificar el original. */
export function ordenarHistorial(historial: readonly CambioComision[]): CambioComision[] {
  return [...historial].sort((a, b) => Date.parse(b.vigenteDesde) - Date.parse(a.vigenteDesde));
}

/**
 * Quién hizo un cambio de comisión. El cambio trae solo el identificador del administrador y el inicial no trae ninguno; el nombre sale
 * de las cuentas internas [SUPUESTO]. Devuelve null si no se puede saber (el llamador decide qué decir).
 */
export function nombreDelAdministrador(
  administradorId: string | null | undefined,
  cuentas: readonly CuentaInterna[],
): string | null {
  if (!administradorId) {
    return null;
  }
  return cuentas.find((cuenta) => cuenta.id === administradorId)?.nombre ?? null;
}
