import type { Texto } from './diccionario';
import { Idioma, idiomaActual } from './idioma';
import { ClaveTexto, TEXTOS } from './textos';

/** Valores para los parámetros `{nombre}` de un texto. `n` es además la cantidad que elige la variante singular o plural. */
export type Parametros = Readonly<Record<string, string | number>>;

const reglasDePlural = new Map<Idioma, Intl.PluralRules>();

function esSingular(idioma: Idioma, cantidad: unknown): boolean {
  if (typeof cantidad !== 'number') {
    return false;
  }
  let reglas = reglasDePlural.get(idioma);
  if (!reglas) {
    reglas = new Intl.PluralRules(idioma);
    reglasDePlural.set(idioma, reglas);
  }
  return reglas.select(cantidad) === 'one';
}

function interpolar(plantilla: string, parametros: Parametros | undefined): string {
  if (!parametros) {
    return plantilla;
  }
  // Un parámetro que no se entregó queda a la vista (`{nombre}`) en lugar de desaparecer: así se nota el olvido.
  return plantilla.replace(/\{(\w+)\}/g, (marca, nombre: string) =>
    nombre in parametros ? String(parametros[nombre]) : marca,
  );
}

/**
 * Busca un texto en un conjunto de diccionarios y lo arma. Si la clave no está en el idioma pedido usa el español, y si
 * tampoco está devuelve la clave, para que el hueco sea visible. Es la función pura que usan `traducir` y las pruebas.
 */
export function traducirDe(
  textos: Readonly<Record<Idioma, Readonly<Record<string, Texto>>>>,
  idioma: Idioma,
  clave: string,
  parametros?: Parametros,
): string {
  const texto: Texto | undefined = textos[idioma][clave] ?? textos.es[clave];
  if (texto === undefined) {
    return clave;
  }
  const plantilla =
    typeof texto === 'string'
      ? texto
      : esSingular(idioma, parametros?.['n'])
        ? texto.uno
        : texto.otros;
  return interpolar(plantilla, parametros);
}

/**
 * El texto de una clave en el idioma vigente. Leer el idioma dentro de una plantilla, de un `computed` o de un `effect`
 * hace que se vuelva a evaluar cuando el idioma cambia. En las plantillas se usa el pipe `t`.
 */
export function traducir(clave: ClaveTexto, parametros?: Parametros): string {
  return traducirDe(TEXTOS, idiomaActual(), clave, parametros);
}
