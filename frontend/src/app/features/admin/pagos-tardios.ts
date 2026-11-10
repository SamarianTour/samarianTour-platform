import { PagoTardio } from '../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../core/idioma/tabla-de-textos';

/** Por qué un pago aprobado no pagó la solicitud (RF-49, RF-50). */
export type MotivoDePagoTardio = PagoTardio['motivo'];

/** El nombre de cada motivo, en el idioma vigente. Exige los dos del contrato. */
export const ETIQUETA_MOTIVO_PAGO = tablaDeTextos<MotivoDePagoTardio>({
  PAGO_TARDIO: 'admin.pagosTardios.motivo.PAGO_TARDIO',
  MONTO_DISTINTO: 'admin.pagosTardios.motivo.MONTO_DISTINTO',
});

/** Qué pasó en cada caso, para saber por qué hay que devolver el dinero. */
export const EXPLICACION_MOTIVO_PAGO = tablaDeTextos<MotivoDePagoTardio>({
  PAGO_TARDIO: 'admin.pagosTardios.explicacion.PAGO_TARDIO',
  MONTO_DISTINTO: 'admin.pagosTardios.explicacion.MONTO_DISTINTO',
});

/** Los pagos del más reciente al más antiguo, sin modificar el original. */
export function ordenarPagosTardios(pagos: readonly PagoTardio[]): PagoTardio[] {
  return [...pagos].sort((a, b) => Date.parse(b.registradoEn) - Date.parse(a.registradoEn));
}

/** El total en pesos que falta devolver: la suma de lo que aprobó la pasarela en cada pago. */
export function totalPorDevolver(pagos: readonly PagoTardio[]): number {
  return pagos.reduce((total, pago) => total + pago.monto, 0);
}

/** Cuántos pagos todavía no se le avisaron al administrador por WhatsApp. */
export function contarSinAvisar(pagos: readonly PagoTardio[]): number {
  return pagos.filter((pago) => !pago.avisado).length;
}

/** Un código corto para citar la solicitud del pago (las primeras letras de su identificador). */
export function codigoDeSolicitud(solicitudId: string): string {
  return solicitudId.slice(0, 8).toUpperCase();
}
