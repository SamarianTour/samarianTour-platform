import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { aErrorApi } from '../errores/errores.interceptor';
import { SesionService } from './sesion.service';

/** Rutas que exigen el token según los contratos: `/prestador`, `/turista`, `/operador`, `/gestor`, `/admin` y `/cuenta`. */
const RUTA_PROTEGIDA =
  /^\/api\/(?:catalogo|nucleo|recomendacion)\/(?:prestador|turista|operador|gestor|admin|cuenta)(?:[/?]|$)/;

/** Rutas con las que se obtiene o se pierde la sesión: son públicas y no deben llevar un token que quizá ya venció. */
const RUTA_DE_ACCESO = /^\/api\/nucleo\/(?:cuentas|politica-datos)(?:[/?]|$)/;

export function esRutaProtegida(url: string): boolean {
  return RUTA_PROTEGIDA.test(url);
}

export function esRutaDeAcceso(url: string): boolean {
  return RUTA_DE_ACCESO.test(url);
}

/**
 * Manda `Authorization: Bearer <token>` a la API cuando hay sesión. Si una petición recibe 401 `NO_AUTENTICADO`
 * (el token venció), renueva la sesión y repite la petición UNA sola vez. Si la renovación no es aceptada, la sesión
 * ya no sirve: en una ruta protegida lleva a iniciar sesión; en una pública (por ejemplo el catálogo) repite la
 * petición sin token para no sacar al usuario de lo que estaba viendo.
 *
 * Va antes que el interceptor de errores en la lista, para recibir ya traducidos los errores de la API.
 */
export const sesionInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  const token = sesion.tokenAcceso();
  if (!peticion.url.startsWith('/api/') || esRutaDeAcceso(peticion.url) || !token) {
    return siguiente(peticion);
  }

  return siguiente(conToken(peticion, token)).pipe(
    catchError((error: unknown) => {
      if (!tokenVencido(error)) {
        return throwError(() => error);
      }
      return sesion.renovar().pipe(
        switchMap((nuevo) => siguiente(conToken(peticion, nuevo))),
        catchError((fallo: unknown) => {
          if (sesion.autenticada()) {
            // La renovación sirvió y la petición repetida falló por su cuenta, o no hubo conexión: se cuenta tal cual.
            return throwError(() => fallo);
          }
          if (!esRutaProtegida(peticion.url)) {
            return siguiente(peticion);
          }
          if (!router.url.startsWith('/iniciar-sesion')) {
            void router.navigate(['/iniciar-sesion'], { queryParams: { volver: router.url } });
          }
          return throwError(() => error);
        }),
      );
    }),
  );
};

function conToken(peticion: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

/**
 * Solo `NO_AUTENTICADO` indica un token vencido o inválido. Otros 401 son respuestas normales de una operación, como
 * `CONTRASENA_INCORRECTA` al eliminar la cuenta, y no deben provocar una renovación.
 */
function tokenVencido(error: unknown): boolean {
  if (!(error instanceof HttpErrorResponse) && !(error instanceof Error)) {
    return false;
  }
  const fallo = aErrorApi(error);
  return fallo.estado === 401 && fallo.codigo === 'NO_AUTENTICADO';
}
