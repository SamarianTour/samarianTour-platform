import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_PRESTADOR, API_TURISTA } from '../api/api.config';
import { IdentidadService } from './identidad.service';

export const HEADER_USUARIO = 'X-Usuario-Id';
export const HEADER_ROL = 'X-Rol';
export const HEADER_TIPO = 'X-Tipo-Prestador';

/**
 * Agrega la identidad provisional del usuario demo, pero solo a las rutas que la exigen y solo si el usuario tiene
 * ese rol: a /prestador/** la del prestador (usuario, rol PRESTADOR y tipo) y a /turista/** la del turista (usuario y
 * rol TURISTA). El catálogo público nunca la lleva. Son los mismos datos que traerá el JWT del núcleo; el backend
 * acepta estos headers únicamente con su perfil demo.
 */
export const identidadInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const ruta = rutaDe(peticion.url);
  if (ruta.startsWith(API_PRESTADOR)) {
    const prestador = inject(IdentidadService).prestador();
    if (prestador) {
      return siguiente(peticion.clone({
        setHeaders: { [HEADER_USUARIO]: prestador.id, [HEADER_ROL]: 'PRESTADOR', [HEADER_TIPO]: prestador.tipo },
      }));
    }
  }
  if (ruta.startsWith(API_TURISTA)) {
    const turista = inject(IdentidadService).turista();
    if (turista) {
      return siguiente(peticion.clone({ setHeaders: { [HEADER_USUARIO]: turista.id, [HEADER_ROL]: 'TURISTA' } }));
    }
  }
  return siguiente(peticion);
};

function rutaDe(url: string): string {
  return url.startsWith('http') ? new URL(url).pathname : url;
}
