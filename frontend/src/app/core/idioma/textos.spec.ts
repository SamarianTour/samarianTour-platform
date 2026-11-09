import type { Texto } from './diccionario';
import { AREAS, TEXTOS } from './textos';

/**
 * Revisa que los diccionarios estén completos y sean coherentes, además de lo que ya garantiza el compilador (el inglés
 * tiene las mismas claves que el español). Lo que no garantiza el compilador son los parámetros, el estilo y el glosario.
 */

/** Textos que se escriben igual en los dos idiomas a propósito (nombres propios, siglas). */
const IGUALES_EN_AMBOS: ReadonlySet<string> = new Set<string>();

/**
 * El glosario (`GLOSARIO.md`): si el texto en español usa el término de la izquierda, el inglés debe usar el de la
 * derecha. Así los términos son iguales en toda la aplicación.
 */
const GLOSARIO: readonly { readonly es: RegExp; readonly en: RegExp; readonly termino: string }[] =
  [
    { es: /turista/i, en: /tourist/i, termino: 'turista → tourist' },
    { es: /prestador/i, en: /provider/i, termino: 'prestador → provider' },
    { es: /oferta/i, en: /offer/i, termino: 'oferta → offer' },
    { es: /actividad/i, en: /activit/i, termino: 'actividad → activity' },
    { es: /paquete/i, en: /package/i, termino: 'paquete → package' },
    { es: /alojamiento/i, en: /accommodation/i, termino: 'alojamiento → accommodation' },
    { es: /restaurante/i, en: /restaurant/i, termino: 'restaurante → restaurant' },
    {
      es: /agencia operadora/i,
      en: /tour operator agency/i,
      termino: 'agencia operadora → tour operator agency',
    },
    {
      es: /guía independiente/i,
      en: /independent guide/i,
      termino: 'guía independiente → independent guide',
    },
    {
      es: /solicitud(es)? de disponibilidad/i,
      en: /availability request/i,
      termino: 'solicitud de disponibilidad → availability request',
    },
    {
      es: /operador(es)? de disponibilidad/i,
      en: /availability operator/i,
      termino: 'operador de disponibilidad → availability operator',
    },
    {
      es: /gestor del destino/i,
      en: /destination manager/i,
      termino: 'gestor del destino → destination manager',
    },
    { es: /administrador/i, en: /administrator/i, termino: 'administrador → administrator' },
    { es: /zona/i, en: /area/i, termino: 'zona → area' },
    {
      es: /área protegida|áreas protegidas/i,
      en: /protected area/i,
      termino: 'área protegida → protected area',
    },
    {
      es: /alta afluencia/i,
      en: /high visitor traffic/i,
      termino: 'alta afluencia → high visitor traffic',
    },
    { es: /opinión|opiniones/i, en: /review/i, termino: 'opinión → review' },
    { es: /RNT/, en: /RNT/, termino: 'RNT → RNT' },
    {
      es: /política de tratamiento de datos/i,
      en: /data processing policy/i,
      termino: 'política de tratamiento de datos → data processing policy',
    },
  ];

/** Excepciones al glosario: clave → términos que no aplican en ese texto, con su motivo en el diccionario. */
const EXCEPCIONES_DE_GLOSARIO: Readonly<Record<string, readonly string[]>> = {};

const variantes = (texto: Texto): string[] =>
  typeof texto === 'string' ? [texto] : [texto.uno, texto.otros];
const marcas = (texto: string): string[] =>
  [...new Set([...texto.matchAll(/\{(\w+)\}/g)].map((m) => m[1]))].sort();

const entradas = AREAS.flatMap((area) =>
  Object.keys(area.es).map((clave) => ({ clave, es: area.es[clave], en: area.en[clave] })),
);

describe('diccionarios de la interfaz', () => {
  it('hay textos', () => {
    expect(entradas.length).toBeGreaterThan(0);
  });

  it.each(AREAS.map((area, indice) => [indice, area] as const))(
    'el área %i tiene las mismas claves en español y en inglés',
    (_indice, area) => {
      expect(Object.keys(area.en).sort()).toEqual(Object.keys(area.es).sort());
    },
  );

  it('ninguna clave se repite entre áreas', () => {
    const claves = entradas.map((entrada) => entrada.clave);
    const repetidas = claves.filter((clave, i) => claves.indexOf(clave) !== i);

    expect(repetidas).toEqual([]);
  });

  it('el registro (TEXTOS) incluye todas las áreas, ni más ni menos', () => {
    const todas = entradas.map((entrada) => entrada.clave).sort();

    expect(Object.keys(TEXTOS.es).sort()).toEqual(todas);
    expect(Object.keys(TEXTOS.en).sort()).toEqual(todas);
  });

  it('las claves se escriben area.pantalla.texto', () => {
    const mal = entradas
      .map((e) => e.clave)
      .filter((clave) => !/^[a-z][a-zA-Z0-9]*(\.[a-zA-Z0-9]+)+$/.test(clave));

    expect(mal).toEqual([]);
  });

  it('ningún texto está vacío ni empieza o termina con espacios', () => {
    const mal = entradas.flatMap(({ clave, es, en }) =>
      [...variantes(es), ...variantes(en)]
        .filter((t) => t.trim() === '' || t !== t.trim())
        .map(() => clave),
    );

    expect(mal).toEqual([]);
  });

  it('un texto es plural en los dos idiomas o en ninguno, y sus variantes llevan {n}', () => {
    const mal = entradas.filter(({ es, en }) => {
      const esPlural = typeof es !== 'string';
      return (
        esPlural !== (typeof en !== 'string') ||
        (esPlural &&
          variantes(es)
            .concat(variantes(en))
            .some((t) => !t.includes('{n}')))
      );
    });

    expect(mal.map((e) => e.clave)).toEqual([]);
  });

  it('el español y el inglés usan los mismos parámetros', () => {
    const mal = entradas.filter(({ es, en }) => {
      const delEs = variantes(es).map(marcas);
      const delEn = variantes(en).map(marcas);
      return JSON.stringify(delEs) !== JSON.stringify(delEn);
    });

    expect(mal.map((e) => e.clave)).toEqual([]);
  });

  it('el inglés no tiene letras ni signos del español: no quedó un texto sin traducir', () => {
    const mal = entradas.filter(({ en }) => variantes(en).some((t) => /[áéíóúñÁÉÍÓÚÑ¿¡]/.test(t)));

    expect(mal.map((e) => e.clave)).toEqual([]);
  });

  it('ningún texto en inglés es idéntico al español, salvo los permitidos', () => {
    const iguales = entradas.filter(
      ({ clave, es, en }) =>
        !IGUALES_EN_AMBOS.has(clave) && JSON.stringify(es) === JSON.stringify(en),
    );

    expect(iguales.map((e) => e.clave)).toEqual([]);
  });

  it('el inglés usa los términos del glosario', () => {
    const faltas = entradas.flatMap(({ clave, es, en }) => {
      const textoEs = variantes(es).join(' ');
      const textoEn = variantes(en).join(' ');
      return GLOSARIO.filter(
        (regla) =>
          regla.es.test(textoEs) &&
          !regla.en.test(textoEn) &&
          !(EXCEPCIONES_DE_GLOSARIO[clave] ?? []).includes(regla.termino),
      ).map((regla) => `${clave}: ${regla.termino}`);
    });

    expect(faltas).toEqual([]);
  });
});
