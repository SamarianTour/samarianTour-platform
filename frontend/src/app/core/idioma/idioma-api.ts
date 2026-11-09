import { Idioma, idiomaActual } from './idioma';

/**
 * El servicio de recomendación recibe el idioma en la petición además de en `Accept-Language` (contrato
 * `recomendacion.openapi.yaml`): en el cuerpo de `POST /consultas` (`ConsultaRecomendacion.idioma`) y como parámetro
 * `idioma` de las alertas y las alternativas. Lo que el usuario consulta siempre sale en el idioma que tiene la interfaz.
 */

/** La consulta con el idioma vigente; un `idioma` ya presente en la consulta se respeta. */
export function conIdioma<T extends object>(
  consulta: T & { idioma?: Idioma },
): T & { idioma: Idioma } {
  return { ...consulta, idioma: consulta.idioma ?? idiomaActual() };
}

/** Los parámetros de URL con el idioma vigente, para `HttpClient.get(..., { params })`. */
export function parametrosConIdioma(
  otros: Readonly<Record<string, string>> = {},
): Record<string, string> {
  return { ...otros, idioma: idiomaActual() };
}
