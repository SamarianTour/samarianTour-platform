import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { Problema } from '../api/modelos';
import { ErrorApi, ExtrasError, MENSAJES, MENSAJE_GENERICO, MENSAJE_SIN_CONEXION } from './error-api';

/**
 * Convierte los errores HTTP en ErrorApi. Lee el application/problem+json de la API y traduce su `codigo`
 * a un mensaje para el usuario; el código no sale de aquí hacia la interfaz.
 */
export const erroresInterceptor: HttpInterceptorFn = (peticion, siguiente) =>
  siguiente(peticion).pipe(catchError((error: unknown) => throwError(() => aErrorApi(error))));

export function aErrorApi(error: unknown): ErrorApi {
  if (error instanceof ErrorApi) {
    return error;
  }
  if (!(error instanceof HttpErrorResponse)) {
    return new ErrorApi(0, undefined, MENSAJE_GENERICO);
  }
  if (error.status === 0) {
    return new ErrorApi(0, undefined, MENSAJE_SIN_CONEXION);
  }
  const problema = aProblema(error.error);
  const codigo = problema?.codigo;
  const mensaje = codigo ? (MENSAJES[codigo] ?? MENSAJE_GENERICO) : MENSAJE_GENERICO;
  const extras = extrasDe(problema);
  if (codigo === 'DATO_INVALIDO') {
    const { campo, detalle } = campoDe(problema?.detail);
    return new ErrorApi(error.status, codigo, mensaje, campo, detalle, extras);
  }
  return new ErrorApi(error.status, codigo, mensaje, undefined, undefined, extras);
}

/** Lee las propiedades extra del núcleo; descarta las que lleguen con otro tipo para que la pantalla no se rompa. */
function extrasDe(problema: object | undefined): ExtrasError | undefined {
  const { bloqueadaHasta, intentosRestantes, compromisos } = (problema ?? {}) as Record<string, unknown>;
  const extras: { -readonly [K in keyof ExtrasError]: ExtrasError[K] } = {};
  if (typeof bloqueadaHasta === 'string') {
    extras.bloqueadaHasta = bloqueadaHasta;
  }
  if (typeof intentosRestantes === 'number') {
    extras.intentosRestantes = intentosRestantes;
  }
  if (Array.isArray(compromisos) && compromisos.every((compromiso) => typeof compromiso === 'string')) {
    extras.compromisos = compromisos;
  }
  return Object.keys(extras).length > 0 ? extras : undefined;
}

function aProblema(cuerpo: unknown): Partial<Problema> | undefined {
  if (typeof cuerpo === 'string') {
    try {
      return aProblema(JSON.parse(cuerpo));
    } catch {
      return undefined;
    }
  }
  return cuerpo !== null && typeof cuerpo === 'object' ? (cuerpo as Partial<Problema>) : undefined;
}

/** La API describe un dato inválido como "campo: detalle", por ejemplo "capacidadMaxima: debe ser mayor que cero". */
function campoDe(detalle: string | undefined): { campo?: string; detalle?: string } {
  const partes = /^([A-Za-z][A-Za-z0-9]*): (.+)$/.exec(detalle ?? '');
  return partes ? { campo: partes[1], detalle: partes[2] } : {};
}
