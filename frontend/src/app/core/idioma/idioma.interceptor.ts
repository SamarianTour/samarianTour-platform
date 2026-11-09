import { HttpInterceptorFn } from '@angular/common/http';
import { idiomaActual } from './idioma';

/**
 * Manda `Accept-Language` con el idioma de la interfaz en toda petición a la API, para que el backend pueda responder
 * en ese idioma (por ejemplo los textos de las alertas). Una petición que ya trae su propio `Accept-Language` lo conserva.
 * Va primero en la lista de interceptores: así lo llevan también las peticiones que la sesión repite con un token nuevo.
 */
export const idiomaInterceptor: HttpInterceptorFn = (peticion, siguiente) =>
  peticion.url.startsWith('/api/') && !peticion.headers.has('Accept-Language')
    ? siguiente(peticion.clone({ setHeaders: { 'Accept-Language': idiomaActual() } }))
    : siguiente(peticion);
