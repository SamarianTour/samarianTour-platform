/**
 * Un texto de la interfaz. Puede llevar parámetros entre llaves (`{n}`, `{nombre}`) y, si depende de una cantidad,
 * dos variantes según `n`: `{ uno: '{n} minuto', otros: '{n} minutos' }`.
 */
export type Texto = string | { readonly uno: string; readonly otros: string };

/** Los textos de un área en los dos idiomas, con exactamente las mismas claves. */
export interface Diccionario<K extends string> {
  readonly es: Readonly<Record<K, Texto>>;
  readonly en: Readonly<Record<K, Texto>>;
}

/**
 * Arma el diccionario de un área. Las claves las fija el español; el inglés debe tenerlas todas y ninguna más: si falta
 * una traducción o sobra una clave, el build falla aquí, no la pantalla en producción.
 *
 * Está en su propio archivo, aparte del registro (`textos.ts`), para que los archivos `*.i18n.ts` no se importen entre
 * sí con el registro y no se forme un ciclo.
 */
export function diccionario<const K extends string>(
  es: Record<K, Texto>,
  en: Record<NoInfer<K>, Texto>,
): Diccionario<K> {
  return { es, en };
}
