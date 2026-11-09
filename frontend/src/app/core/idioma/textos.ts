import { COMUN } from './comun.i18n';
import type { Diccionario, Texto } from './diccionario';
import type { Idioma } from './idioma';

/**
 * Las áreas con textos. Al traducir un área nueva se agrega su diccionario aquí, en `ClaveTexto` y en `TEXTOS`.
 * Las claves se escriben `area.pantalla.texto` (por ejemplo `cuentas.login.titulo`) y no pueden repetirse entre áreas.
 */
export const AREAS: readonly Diccionario<string>[] = [COMUN];

/** Toda clave de texto válida: el pipe `t` y `traducir` solo aceptan estas. */
export type ClaveTexto = keyof typeof COMUN.es;

export const TEXTOS: Readonly<Record<Idioma, Readonly<Record<ClaveTexto, Texto>>>> = {
  es: { ...COMUN.es },
  en: { ...COMUN.en },
};
