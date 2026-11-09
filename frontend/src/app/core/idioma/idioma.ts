import { signal } from '@angular/core';

/** Los idiomas de la interfaz (RF-18, RF-19). */
export type Idioma = 'es' | 'en';

export const IDIOMAS: readonly Idioma[] = ['es', 'en'];
export const IDIOMA_POR_DEFECTO: Idioma = 'es';

/** Dónde se recuerda el idioma elegido. */
export const CLAVE_IDIOMA = 'samarian-tour.idioma';

/** El formato regional de cada idioma, para fechas, números y precios. */
export const LOCALE: Readonly<Record<Idioma, string>> = { es: 'es-CO', en: 'en-US' };

export function esIdioma(valor: unknown): valor is Idioma {
  return valor === 'es' || valor === 'en';
}

/**
 * El idioma que corresponde a la configuración del navegador: `es` o `en` si su idioma principal lo es, sin importar la
 * región (`es-CO`, `en-GB`); si no, español.
 */
export function idiomaDelNavegador(lenguaje: string | null | undefined): Idioma {
  const base = (lenguaje ?? '').split(/[-_]/)[0].toLowerCase();
  return esIdioma(base) ? base : IDIOMA_POR_DEFECTO;
}

/** Con una elección guardada manda esa; sin ella, la del navegador. Un valor guardado que no sea `es` ni `en` se ignora. */
export function elegirIdiomaInicial(
  guardado: string | null,
  lenguajeDelNavegador: string | null | undefined,
): Idioma {
  return esIdioma(guardado) ? guardado : idiomaDelNavegador(lenguajeDelNavegador);
}

function leerGuardado(): string | null {
  try {
    return localStorage.getItem(CLAVE_IDIOMA);
  } catch {
    return null;
  }
}

function lenguajeDelNavegador(): string | undefined {
  try {
    return navigator.language;
  } catch {
    return undefined;
  }
}

/**
 * El idioma vigente vive aquí, en un signal de módulo, y no dentro de un servicio, porque lo necesita también el código
 * que no tiene inyección de dependencias: las tablas de etiquetas, `ErrorApi` y las funciones puras que arman mensajes.
 * `IdiomaService` es la fachada para los componentes. Cualquier lectura dentro de una plantilla o de un `computed` se
 * actualiza sola cuando el idioma cambia.
 */
const actual = signal<Idioma>(elegirIdiomaInicial(leerGuardado(), lenguajeDelNavegador()));

export const idiomaActual = actual.asReadonly();

/**
 * Cambia el idioma al instante. Por defecto lo recuerda para la próxima visita; con `recordar: false` solo cambia el de
 * esta sesión (lo usan las pruebas).
 */
export function establecerIdioma(
  idioma: Idioma,
  opciones: { readonly recordar?: boolean } = {},
): void {
  actual.set(idioma);
  if (opciones.recordar ?? true) {
    try {
      localStorage.setItem(CLAVE_IDIOMA, idioma);
    } catch {
      // Sin almacenamiento (modo privado), la elección dura lo que dure la página.
    }
  }
}
