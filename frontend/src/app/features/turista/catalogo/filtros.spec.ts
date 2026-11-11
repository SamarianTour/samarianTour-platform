import { convertToParamMap } from '@angular/router';
import {
  FiltrosCatalogo,
  normalizarTexto,
  textoInvalido,
  SIN_FILTROS,
  TAMANO_PAGINA,
  consultaDeFiltros,
  filtrosDesdeUrl,
  hayFiltros,
  urlDeFiltros,
} from './filtros';

const TODOS: FiltrosCatalogo = {
  texto: 'café minca',
  tipo: 'ACTIVIDAD',
  zonaId: 'a00000000000000000000007',
  tipoActividad: 'SENDERISMO',
  precioMin: 50000,
  precioMax: 200000,
  nivelEsfuerzoMaximo: 'MODERADO',
  accesibleSillaRuedas: true,
  pagina: 2,
};

describe('consultaDeFiltros', () => {
  it('sin filtros solo envía la página y el tamaño', () => {
    expect(consultaDeFiltros(SIN_FILTROS)).toEqual({ page: 0, size: TAMANO_PAGINA });
  });

  it('envía cada filtro con el nombre que usa la API', () => {
    expect(consultaDeFiltros(TODOS)).toEqual({
      texto: 'café minca',
      tipo: 'ACTIVIDAD',
      zonaId: 'a00000000000000000000007',
      tipoActividad: 'SENDERISMO',
      precioMin: 50000,
      precioMax: 200000,
      nivelEsfuerzoMaximo: 'MODERADO',
      accesibleSillaRuedas: true,
      page: 2,
      size: 12,
    });
  });

  it('envía un precio mínimo de cero, porque sí es un filtro', () => {
    expect(consultaDeFiltros({ ...SIN_FILTROS, precioMin: 0 })).toEqual({ precioMin: 0, page: 0, size: 12 });
  });

  it('no envía la accesibilidad cuando no está marcada', () => {
    expect(consultaDeFiltros({ ...SIN_FILTROS, tipo: 'PAQUETE' })).toEqual({ tipo: 'PAQUETE', page: 0, size: 12 });
  });
});

describe('urlDeFiltros', () => {
  it('deja la URL sin query params cuando no hay filtros', () => {
    expect(urlDeFiltros(SIN_FILTROS)).toEqual({});
  });

  it('escribe solo los filtros con valor y la página si no es la primera', () => {
    expect(urlDeFiltros({ ...SIN_FILTROS, zonaId: 'a00000000000000000000005', pagina: 3 })).toEqual({
      zonaId: 'a00000000000000000000005',
      page: 3,
    });
  });
});

describe('filtrosDesdeUrl', () => {
  it('lee de vuelta lo que escribe urlDeFiltros', () => {
    // En la URL todo es texto: así lo entrega el enrutador.
    const enLaUrl = Object.fromEntries(
      Object.entries(urlDeFiltros(TODOS)).map(([nombre, valor]) => [nombre, String(valor)]),
    );

    expect(filtrosDesdeUrl(convertToParamMap(enLaUrl))).toEqual(TODOS);
  });

  it('devuelve los filtros vacíos para una URL sin query params', () => {
    expect(filtrosDesdeUrl(convertToParamMap({}))).toEqual(SIN_FILTROS);
  });

  it('ignora los valores desconocidos o mal formados', () => {
    const params = convertToParamMap({
      tipo: 'CRUCERO',
      tipoActividad: 'surf',
      nivelEsfuerzoMaximo: 'EXTREMO',
      precioMin: 'barato',
      precioMax: '-5',
      accesibleSillaRuedas: 'quizas',
      page: '-2',
    });

    expect(filtrosDesdeUrl(params)).toEqual(SIN_FILTROS);
  });
});

describe('hayFiltros', () => {
  it('no cuenta la página como filtro', () => {
    expect(hayFiltros({ ...SIN_FILTROS, pagina: 4 })).toBe(false);
  });

  it('detecta cualquier filtro puesto', () => {
    expect(hayFiltros({ ...SIN_FILTROS, accesibleSillaRuedas: true })).toBe(true);
    expect(hayFiltros({ ...SIN_FILTROS, precioMin: 0 })).toBe(true);
  });
});

describe('búsqueda por texto (DC-37)', () => {
  it.each([
    ['  buceo  ', 'buceo'],
    ['bu', 'bu'],
    ['a'.repeat(100), 'a'.repeat(100)],
    ['café minca', 'café minca'],
  ])('"%s" se puede buscar', (escrito, esperado) => {
    expect(normalizarTexto(escrito)).toBe(esperado);
    expect(textoInvalido(escrito)).toBe(false);
  });

  it.each([[''], ['   '], [null], [undefined]])('%s es una búsqueda vacía: no filtra y no es un error', (escrito) => {
    expect(normalizarTexto(escrito)).toBeNull();
    expect(textoInvalido(escrito)).toBe(false);
  });

  it.each([['b'], [' b '], ['a'.repeat(101)]])('%s no cumple el largo: no se envía y se avisa', (escrito) => {
    expect(normalizarTexto(escrito)).toBeNull();
    expect(textoInvalido(escrito)).toBe(true);
  });

  it('viaja como texto en la consulta y en la URL, sin los espacios de los extremos', () => {
    const filtros = filtrosDesdeUrl(convertToParamMap({ texto: '  buceo ' }));

    expect(filtros.texto).toBe('buceo');
    expect(consultaDeFiltros(filtros)).toEqual({ texto: 'buceo', page: 0, size: 12 });
    expect(urlDeFiltros(filtros)).toEqual({ texto: 'buceo' });
    expect(hayFiltros(filtros)).toBe(true);
  });

  it('una URL con un texto de un solo carácter se ignora', () => {
    expect(filtrosDesdeUrl(convertToParamMap({ texto: 'b' })).texto).toBeNull();
  });
});
