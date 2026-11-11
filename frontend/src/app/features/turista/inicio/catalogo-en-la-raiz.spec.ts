import { convertToParamMap } from '@angular/router';
import { PARAMETROS_DEL_CATALOGO, esEnlaceViejoDelCatalogo } from './catalogo-en-la-raiz';
import { urlDeFiltros, SIN_FILTROS } from '../catalogo/filtros';

describe('enlaces viejos del catálogo en la raíz', () => {
  it.each(PARAMETROS_DEL_CATALOGO.map((nombre) => [nombre]))('"?%s=..." es un enlace viejo', (nombre) => {
    expect(esEnlaceViejoDelCatalogo(convertToParamMap({ [nombre]: 'x' }))).toBe(true);
  });

  it('la raíz sola, o con parámetros ajenos, es el Inicio', () => {
    expect(esEnlaceViejoDelCatalogo(convertToParamMap({}))).toBe(false);
    expect(esEnlaceViejoDelCatalogo(convertToParamMap({ utm_source: 'correo' }))).toBe(false);
  });

  it('reconoce todos los parámetros que escribe el catálogo', () => {
    const todos = urlDeFiltros({
      ...SIN_FILTROS,
      texto: 'a1',
      tipo: 'ACTIVIDAD',
      zonaId: 'z',
      tipoActividad: 'SENDERISMO',
      precioMin: 1,
      precioMax: 2,
      nivelEsfuerzoMaximo: 'MODERADO',
      accesibleSillaRuedas: true,
      pagina: 1,
    });

    expect(Object.keys(todos).sort()).toEqual([...PARAMETROS_DEL_CATALOGO].sort());
  });
});
