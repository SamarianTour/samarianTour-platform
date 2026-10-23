/**
 * Base de la API del catálogo. Es relativa: en desarrollo la resuelve el proxy de ng serve (proxy.conf.json)
 * y en el piloto, el mismo Nginx que sirve la aplicación.
 */
export const API_CATALOGO = '/api/catalogo';

/** Rutas que exigen la identidad del prestador. */
export const API_PRESTADOR = `${API_CATALOGO}/prestador/`;

/** Rutas que exigen la identidad del turista. */
export const API_TURISTA = `${API_CATALOGO}/turista/`;
