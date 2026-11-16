/**
 * Convierte en bloques el subconjunto de Markdown que usa la política de datos: `#` y `##` (títulos), `**negrita**`,
 * `>` (cita), `-` (lista), `1.` (lista numerada) y párrafos separados por una línea en blanco. No es un Markdown completo
 * ni pretende serlo: lo que no reconoce se muestra como texto. No produce HTML: el resultado son datos, y la plantilla los
 * pinta con interpolación, así que cualquier HTML del texto se ve como texto.
 */

/** Un trozo de texto de una línea: normal o en negrita. */
export interface Segmento {
  readonly texto: string;
  readonly negrita: boolean;
}

export type TipoDeBloque = 'titulo' | 'parrafo' | 'cita' | 'lista' | 'numerada';

/**
 * Un bloque. Todos los campos existen siempre (vacíos si no aplican) para que la plantilla no tenga que distinguir tipos:
 * `segmentos` sirve a título, párrafo y cita; `items` a las listas; `nivel` es el del encabezado HTML (2 o 3).
 */
export interface Bloque {
  readonly tipo: TipoDeBloque;
  readonly nivel: number;
  readonly segmentos: readonly Segmento[];
  readonly items: readonly (readonly Segmento[])[];
}

/** Lo que se va armando: sus líneas se unen con un espacio al cerrarse el bloque. */
interface Pendiente {
  tipo: TipoDeBloque;
  nivel: number;
  /** Para párrafo, cita y título, una sola entrada; para las listas, una por elemento. */
  lineas: string[];
}

const TITULO = /^(#{1,6})\s+(.*)$/;
const CITA = /^>\s?(.*)$/;
const VINETA = /^[-*]\s+(.*)$/;
const NUMERADO = /^\d+[.)]\s+(.*)$/;

/** Separa el texto de una línea en segmentos normales y en negrita. Una `**` sin pareja queda como texto. */
export function segmentos(linea: string): Segmento[] {
  const resultado: Segmento[] = [];
  const negrita = /\*\*(.+?)\*\*/g;
  let desde = 0;
  for (const coincidencia of linea.matchAll(negrita)) {
    if (coincidencia.index > desde) {
      resultado.push({ texto: linea.slice(desde, coincidencia.index), negrita: false });
    }
    resultado.push({ texto: coincidencia[1], negrita: true });
    desde = coincidencia.index + coincidencia[0].length;
  }
  if (desde < linea.length) {
    resultado.push({ texto: linea.slice(desde), negrita: false });
  }
  return resultado;
}

/** Texto plano de unos segmentos, sin la negrita. */
export function textoPlano(partes: readonly Segmento[]): string {
  return partes.map((parte) => parte.texto).join('');
}

export function parsearMarkdown(markdown: string): Bloque[] {
  const bloques: Bloque[] = [];
  let actual: Pendiente | null = null;

  const cerrar = (): void => {
    if (!actual) {
      return;
    }
    const lineas = actual.lineas.map((linea) => linea.replace(/\s+/g, ' ').trim());
    const esLista = actual.tipo === 'lista' || actual.tipo === 'numerada';
    bloques.push({
      tipo: actual.tipo,
      nivel: actual.nivel,
      segmentos: esLista ? [] : segmentos(lineas.join(' ')),
      items: esLista ? lineas.map(segmentos) : [],
    });
    actual = null;
  };

  for (const crudo of markdown.replace(/\r\n?/g, '\n').split('\n')) {
    const linea = crudo.trim();
    if (linea === '') {
      cerrar();
      continue;
    }

    const titulo = TITULO.exec(linea);
    if (titulo) {
      cerrar();
      // El texto trae `#` y `##`; como la página ya tiene su h1, bajan un nivel: h2 y h3.
      actual = { tipo: 'titulo', nivel: Math.min(titulo[1].length + 1, 6), lineas: [titulo[2]] };
      cerrar();
      continue;
    }

    const cita = CITA.exec(linea);
    if (cita) {
      if (actual?.tipo !== 'cita') {
        cerrar();
        actual = { tipo: 'cita', nivel: 0, lineas: [''] };
      }
      actual.lineas[0] += ` ${cita[1]}`;
      continue;
    }

    const vineta = VINETA.exec(linea);
    const numerado = NUMERADO.exec(linea);
    const elemento = vineta ?? numerado;
    if (elemento) {
      const tipo: TipoDeBloque = vineta ? 'lista' : 'numerada';
      if (actual?.tipo !== tipo) {
        cerrar();
        actual = { tipo, nivel: 0, lineas: [] };
      }
      actual.lineas.push(elemento[1]);
      continue;
    }

    // Una línea sin marca sigue al bloque abierto: se une a su última línea (a la de su elemento, en una lista).
    if (actual) {
      actual.lineas[actual.lineas.length - 1] += ` ${linea}`;
    } else {
      actual = { tipo: 'parrafo', nivel: 0, lineas: [linea] };
    }
  }
  cerrar();
  return bloques;
}
