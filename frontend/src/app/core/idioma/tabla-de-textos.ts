import type { ClaveTexto } from './textos';
import { traducir } from './traducir';

/**
 * Una tabla de etiquetas que se traduce sola. Recibe, para cada valor, la clave de su texto, y devuelve un objeto que se
 * usa igual que una tabla de textos (`ROL[rol]`), pero cuyo valor se lee en el idioma vigente cada vez que se consulta.
 * Por eso las plantillas y los `computed` que la leen se actualizan al cambiar de idioma sin tocarlos.
 */
export function tablaDeTextos<K extends string | number>(
  claves: Readonly<Record<K, ClaveTexto>>,
): Readonly<Record<K, string>> {
  const tabla = {} as Record<K, string>;
  for (const valor of Object.keys(claves) as K[]) {
    Object.defineProperty(tabla, valor, {
      enumerable: true,
      get: () => traducir(claves[valor]),
    });
  }
  return tabla;
}
