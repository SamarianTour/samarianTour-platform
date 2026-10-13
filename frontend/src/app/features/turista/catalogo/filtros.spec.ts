import { convertToParamMap } from '@angular/router';
import {
  FiltrosCatalogo,
  SIN_FILTROS,
  TAMANO_PAGINA,
  consultaDeFiltros,
  filtrosDesdeUrl,
  hayFiltros,
  urlDeFiltros,
} from './filtros';

const TODOS: FiltrosCatalogo = {
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
