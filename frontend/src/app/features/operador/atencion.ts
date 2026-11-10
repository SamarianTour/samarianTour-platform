import { AtencionRequest } from '../../core/api/modelos-nucleo';

/** Lo que el operador decide tras verificar el cupo con el prestador (RF-37 a RF-40). */
export type ResultadoAtencion = AtencionRequest['resultado'];

/** Los límites del contrato (`AtencionRequest`). */
export const POLITICA_MAXIMA = 1000;
export const NOTA_ATENDIDA_MAXIMA = 1000;
export const NOTA_NO_DISPONIBLE_MAXIMA = 500;

/** Lo que el formulario de atención guarda. El precio es el TOTAL en pesos, no por persona (RF-37). */
export interface ValoresAtencion {
  readonly resultado: ResultadoAtencion;
  readonly precioTotal: number | null;
  readonly politicaCancelacion: string;
  /** El prestador aceptó apartar el cupo durante la ventana de confirmación y pago (RF-38). */
  readonly aparteAcordado: boolean;
  readonly nota: string;
}

export const VALORES_ATENCION_VACIOS: ValoresAtencion = {
  resultado: 'ATENDIDA',
  precioTotal: null,
  politicaCancelacion: '',
  aparteAcordado: false,
  nota: '',
};

export type CampoAtencion = 'precioTotal' | 'politicaCancelacion' | 'aparteAcordado' | 'nota';
export type MotivoAtencion = 'requerido' | 'entero' | 'minimo' | 'largo' | 'sinAparte';

/** El máximo de la nota según el resultado: el motivo de "no disponible" es más corto que las condiciones. */
export function notaMaxima(resultado: ResultadoAtencion): number {
  return resultado === 'ATENDIDA' ? NOTA_ATENDIDA_MAXIMA : NOTA_NO_DISPONIBLE_MAXIMA;
}

/**
 * Valida como lo hace el núcleo para avisar antes de enviar; el núcleo vuelve a validar. Con "hay cupo" (ATENDIDA) exige el
 * precio total (entero, mayor que cero), la política de cancelación y que el prestador haya aceptado apartar el cupo
 * (RF-38, RF-39). Con "no hay cupo" solo revisa el largo del motivo (RF-40). Devuelve el primer motivo de cada campo con
 * error; un objeto vacío significa que se puede enviar.
 */
export function validarAtencion(
  valores: ValoresAtencion,
): Partial<Record<CampoAtencion, MotivoAtencion>> {
  const errores: Partial<Record<CampoAtencion, MotivoAtencion>> = {};

  if (valores.resultado === 'ATENDIDA') {
    const precio = valores.precioTotal;
    if (precio === null || precio === undefined || Number.isNaN(precio)) {
      errores.precioTotal = 'requerido';
    } else if (!Number.isSafeInteger(precio)) {
      errores.precioTotal = 'entero';
    } else if (precio < 1) {
      errores.precioTotal = 'minimo';
    }

    const politica = valores.politicaCancelacion.trim();
    if (politica === '') {
      errores.politicaCancelacion = 'requerido';
    } else if (politica.length > POLITICA_MAXIMA) {
      errores.politicaCancelacion = 'largo';
    }

    if (!valores.aparteAcordado) {
      errores.aparteAcordado = 'sinAparte';
    }
  }

  if (valores.nota.trim().length > notaMaxima(valores.resultado)) {
    errores.nota = 'largo';
  }
  return errores;
}

/**
 * El cuerpo de `POST /operador/solicitudes/{id}/atencion` para los valores del formulario, que deben estar validados.
 * "No hay cupo" no manda precio ni condiciones, aunque el operador las haya escrito antes de cambiar de opción.
 */
export function construirAtencion(valores: ValoresAtencion): AtencionRequest {
  const nota = valores.nota.trim();
  if (valores.resultado === 'NO_DISPONIBLE') {
    return { resultado: 'NO_DISPONIBLE', ...(nota ? { nota } : {}) };
  }
  return {
    resultado: 'ATENDIDA',
    precioBase: valores.precioTotal ?? 0,
    politicaCancelacion: valores.politicaCancelacion.trim(),
    aparteAcordado: true,
    ...(nota ? { nota } : {}),
  };
}

/**
 * El enlace para escribirle al prestador por WhatsApp (RF-71), o null si no tiene número. El número llega como "+573001110101";
 * `wa.me` lo quiere solo con dígitos y con el indicativo del país.
 */
export function enlaceWhatsapp(numero: string | null | undefined): string | null {
  const digitos = (numero ?? '').replace(/\D/g, '');
  return digitos.length >= 8 ? `https://wa.me/${digitos}` : null;
}
