/**
 * Base de la API del catálogo. Es relativa: en desarrollo la resuelve el proxy de ng serve (proxy.conf.json)
 * y en el piloto, el mismo Nginx que sirve la aplicación.
 */
export const API_CATALOGO = '/api/catalogo';

/** Base de la API del núcleo: cuentas, solicitudes, pagos, prestadores, administración e indicadores. */
export const API_NUCLEO = '/api/nucleo';

/** Base de la API de recomendación: chatbot, alertas del destino y alternativas. */
export const API_RECOMENDACION = '/api/recomendacion';
