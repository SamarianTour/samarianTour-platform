import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_PRESTADOR } from '../api/api.config';
import { IdentidadService } from './identidad.service';

export const HEADER_USUARIO = 'X-Usuario-Id';
export const HEADER_ROL = 'X-Rol';
export const HEADER_TIPO = 'X-Tipo-Prestador';

/**
 * Agrega la identidad provisional del prestador (usuario, rol PRESTADOR y tipo), pero solo a las rutas
 * /prestador/**: el catálogo público nunca la lleva. Son los mismos datos que traerá el JWT del núcleo; el backend
 * acepta estos headers únicamente con su perfil demo.
 */
export const identidadInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  if (!esDelPrestador(peticion.url)) {
    return siguiente(peticion);
  }
  const prestador = inject(IdentidadService).prestador();
  return siguiente(
    peticion.clone({
      setHeaders: { [HEADER_USUARIO]: prestador.id, [HEADER_ROL]: 'PRESTADOR', [HEADER_TIPO]: prestador.tipo },
    }),
  );
};

function esDelPrestador(url: string): boolean {
  const ruta = url.startsWith('http') ? new URL(url).pathname : url;
  return ruta.startsWith(API_PRESTADOR);
}
