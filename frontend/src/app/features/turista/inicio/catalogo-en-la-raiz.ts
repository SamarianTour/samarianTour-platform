import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Los query params del catálogo (filtros, texto y página). Antes el catálogo vivía en la raíz, así que los enlaces viejos
 * y los de la demostración (`/?tipo=ACTIVIDAD&zonaId=...`) traen alguno de ellos.
 */
export const PARAMETROS_DEL_CATALOGO: readonly string[] = [
  'texto',
  'tipo',
  'zonaId',
  'tipoActividad',
  'precioMin',
  'precioMax',
  'nivelEsfuerzoMaximo',
  'accesibleSillaRuedas',
  'page',
];

/** Si una dirección de la raíz trae algún parámetro del catálogo: entonces es un enlace viejo y no el Inicio. */
export function esEnlaceViejoDelCatalogo(params: { has(nombre: string): boolean }): boolean {
  return PARAMETROS_DEL_CATALOGO.some((nombre) => params.has(nombre));
}

/**
 * Guard del Inicio: si la raíz trae filtros del catálogo es un enlace viejo, y se redirige a `/catalogo` con los mismos
 * parámetros; sin ellos, `/` es el Inicio. (Una ruta no puede combinar `redirectTo` con `canMatch`, por eso es un guard.)
 */
export const catalogoEnLaRaiz: CanActivateFn = (ruta) =>
  esEnlaceViejoDelCatalogo(ruta.queryParamMap)
    ? inject(Router).createUrlTree(['/catalogo'], { queryParams: ruta.queryParams })
    : true;
