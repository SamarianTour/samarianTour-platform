import { HttpErrorResponse } from '@angular/common/http';
import { OperatorFunction, TimeoutError, catchError, map, of, timeout } from 'rxjs';
import { ErrorApi } from '../errores/error-api';
import { aErrorApi } from '../errores/errores.interceptor';

/**
 * Lo que tarda como máximo cada consulta de la recomendación antes de darla por no disponible. El servicio de recomendación es
 * opcional (ADR-04): si no responde a tiempo, su parte de la pantalla se oculta o avisa, y el resto de la plataforma sigue.
 */
/** `POST /recomendaciones` puede tardar hasta unos 9 s: el catálogo (3 s) y la IA (6 s) antes de pasar al motor por reglas (DR-05, DR-06). */
export const LIMITE_RECOMENDAR_MS = 10_000;
/** La página de alertas es la única razón de entrar a ella: se le da más tiempo que a una sección de otra página. */
export const LIMITE_ALERTAS_PAGINA_MS = 8_000;
/** Una sección de la ficha de la oferta (alertas, alternativas) no debe demorar la página: a los 3 s se oculta. */
export const LIMITE_SECCION_MS = 3_000;

/** Por qué una consulta no está disponible: tardó demasiado, el servicio no responde o no hay conexión. */
export type MotivoNoDisponible = 'tiempo' | 'servicio' | 'conexion';

/**
 * El resultado de una consulta a la recomendación. Nunca es un error que rompa la pantalla: o hay datos, o el servicio no está
 * disponible (se oculta o se avisa), o la consulta tuvo un error propio —un dato inválido, una oferta que no existe— que la pantalla
 * explica.
 */
export type Resultado<T> =
  | { readonly estado: 'ok'; readonly datos: T }
  | { readonly estado: 'no-disponible'; readonly motivo: MotivoNoDisponible }
  | { readonly estado: 'error'; readonly error: ErrorApi };

/** Los estados HTTP con que el servidor o Nginx dicen que el servicio no está: 502, 503 y 504 (la pasarela se cansó de esperar). */
const ESTADOS_NO_DISPONIBLE: readonly number[] = [502, 503, 504];

/**
 * Si un fallo significa que el servicio no está disponible, y por qué; `null` si es un error propio de la consulta. Cuentan como
 * no disponible: pasar del límite de tiempo, no tener conexión y un 502, 503 o 504.
 * [SUPUESTO] El 504 (el Nginx se cansó de esperar al servicio) también cuenta: es el mismo caso que el 502 y el 503.
 */
export function motivoNoDisponible(fallo: unknown): MotivoNoDisponible | null {
  if (fallo instanceof TimeoutError) {
    return 'tiempo';
  }
  if (!(fallo instanceof ErrorApi) && !(fallo instanceof HttpErrorResponse)) {
    // Un fallo del propio código no es "el servicio no está": se deja pasar como error.
    return null;
  }
  const error = aErrorApi(fallo);
  if (error.estado === 0) {
    return 'conexion';
  }
  return ESTADOS_NO_DISPONIBLE.includes(error.estado) ? 'servicio' : null;
}

/**
 * Convierte una consulta HTTP en un `Resultado` y le pone un límite de tiempo: al pasarse, la petición se cancela y el resultado es
 * "no disponible". Así la pantalla nunca queda esperando ni recibe un error que no sabe tratar.
 */
export function conDegradacion<T>(limiteMs: number): OperatorFunction<T, Resultado<T>> {
  return (origen) =>
    origen.pipe(
      timeout({ first: limiteMs }),
      map((datos): Resultado<T> => ({ estado: 'ok', datos })),
      catchError((fallo: unknown) => {
        const motivo = motivoNoDisponible(fallo);
        return of<Resultado<T>>(
          motivo ? { estado: 'no-disponible', motivo } : { estado: 'error', error: aErrorApi(fallo) },
        );
      }),
    );
}
