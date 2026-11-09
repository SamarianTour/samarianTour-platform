import { APP } from './app.i18n';
import { COMPARTIDO } from './compartido.i18n';
import { COMUN } from './comun.i18n';
import type { Diccionario, Texto } from './diccionario';
import { ERRORES } from './errores.i18n';
import { ETIQUETAS } from './etiquetas.i18n';
import type { Idioma } from './idioma';

/**
 * Las áreas con textos. Al traducir un área nueva se agrega su diccionario aquí, en `ClaveTexto` y en `TEXTOS`.
 * Las claves se escriben `area.pantalla.texto` (por ejemplo `cuentas.login.titulo`) y no pueden repetirse entre áreas.
 */
export const AREAS: readonly Diccionario<string>[] = [COMUN, APP, COMPARTIDO, ERRORES, ETIQUETAS];

/** Toda clave de texto válida: el pipe `t` y `traducir` solo aceptan estas. */
export type ClaveTexto =
  | keyof typeof COMUN.es
  | keyof typeof APP.es
  | keyof typeof COMPARTIDO.es
  | keyof typeof ERRORES.es
  | keyof typeof ETIQUETAS.es;

export const TEXTOS: Readonly<Record<Idioma, Readonly<Record<ClaveTexto, Texto>>>> = {
  es: { ...COMUN.es, ...APP.es, ...COMPARTIDO.es, ...ERRORES.es, ...ETIQUETAS.es },
  en: { ...COMUN.en, ...APP.en, ...COMPARTIDO.en, ...ERRORES.en, ...ETIQUETAS.en },
};
