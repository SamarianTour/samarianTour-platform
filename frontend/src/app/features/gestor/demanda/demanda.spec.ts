import { Zona } from '../../../core/api/modelos';
import { Tablero } from '../../../core/api/modelos-nucleo';
import {
  ANCHO_MENOS_DE_TRES,
  FilaDemanda,
  anchoDeBarra,
  cifrasDeDemanda,
  datosDeZona,
  filasDeDemanda,
  maximoConocido,
} from './demanda';

const TABLERO: Tablero = {
  desde: '2026-10-01',
  hasta: '2026-10-31',
  total: 42,
  filas: [
    { zonaId: 'tayrona', tipoOferta: 'ACTIVIDAD', solicitudes: 18 },
    { zonaId: 'tayrona', tipoOferta: 'ALOJAMIENTO', solicitudes: 9 },
    { zonaId: 'minca', tipoOferta: 'ACTIVIDAD', solicitudes: 7 },
    { zonaId: 'minca', tipoOferta: 'ALOJAMIENTO' },
    { zonaId: 'taganga', tipoOferta: 'RESTAURANTE' },
  ],
};

describe('filasDeDemanda', () => {
  it('conserva el orden del núcleo y le da a cada fila una clave propia', () => {
    const filas = filasDeDemanda(TABLERO);

    expect(filas.map((f) => f.clave)).toEqual([
      'tayrona|ACTIVIDAD',
      'tayrona|ALOJAMIENTO',
      'minca|ACTIVIDAD',
      'minca|ALOJAMIENTO',
      'taganga|RESTAURANTE',
    ]);
    expect(new Set(filas.map((f) => f.clave)).size).toBe(filas.length);
  });

  it('un conteo que el núcleo omitió queda en null: no se inventa', () => {
    const filas = filasDeDemanda(TABLERO);

    expect(filas.map((f) => f.solicitudes)).toEqual([18, 9, 7, null, null]);
  });

  it('un cero es un cero', () => {
    const filas = filasDeDemanda({ ...TABLERO, filas: [{ zonaId: 'z', tipoOferta: 'ACTIVIDAD', solicitudes: 0 }] });

    expect(filas[0].solicitudes).toBe(0);
  });

  it('sin solicitudes no hay filas', () => {
    expect(filasDeDemanda({ desde: '2026-10-01', hasta: '2026-10-31', total: 0, filas: [] })).toEqual([]);
  });
});

describe('cifrasDeDemanda', () => {
  it('el total es el que entrega el núcleo, no la suma de las filas', () => {
    // Las filas conocidas suman 34; el total real es 42 porque dos filas tienen 1 o 2 solicitudes y no traen su conteo.
    expect(cifrasDeDemanda(TABLERO).total).toBe(42);
  });

  it('un total omitido (1 o 2 solicitudes) es null', () => {
    const cifras = cifrasDeDemanda({ desde: 'a', hasta: 'b', filas: [{ zonaId: 'z', tipoOferta: 'ACTIVIDAD' }] });

    expect(cifras.total).toBeNull();
  });

  it('un total de cero es cero', () => {
    expect(cifrasDeDemanda({ desde: 'a', hasta: 'b', total: 0, filas: [] }).total).toBe(0);
  });

  it('cuenta las zonas y los tipos distintos, también los de conteo omitido', () => {
    const cifras = cifrasDeDemanda(TABLERO);

    expect(cifras.zonas).toBe(3);
    expect(cifras.tiposDeOferta).toBe(3);
  });

  it('sin filas no hay zonas ni tipos', () => {
    const cifras = cifrasDeDemanda({ desde: 'a', hasta: 'b', total: 0, filas: [] });

    expect(cifras.zonas).toBe(0);
    expect(cifras.tiposDeOferta).toBe(0);
  });
});

describe('las barras', () => {
  const fila = (solicitudes: number | null): FilaDemanda => ({
    clave: 'z|ACTIVIDAD',
    zonaId: 'z',
    tipoOferta: 'ACTIVIDAD',
    solicitudes,
  });

  it('la escala es la mayor cantidad conocida', () => {
    expect(maximoConocido(filasDeDemanda(TABLERO))).toBe(18);
    expect(maximoConocido([fila(null), fila(null)])).toBe(0);
    expect(maximoConocido([])).toBe(0);
  });

  it('el ancho es proporcional a la mayor, de 0 a 100', () => {
    expect(anchoDeBarra(18, 18)).toBe(100);
    expect(anchoDeBarra(9, 18)).toBe(50);
    expect(anchoDeBarra(7, 18)).toBe(39);
  });

  it('una barra con pocas solicitudes se sigue viendo', () => {
    expect(anchoDeBarra(3, 1000)).toBe(3);
  });

  it('un cero no tiene barra', () => {
    expect(anchoDeBarra(0, 18)).toBe(0);
  });

  it('un conteo omitido lleva una marca corta, siempre la misma', () => {
    expect(anchoDeBarra(null, 18)).toBe(ANCHO_MENOS_DE_TRES);
    expect(anchoDeBarra(null, 0)).toBe(ANCHO_MENOS_DE_TRES);
  });

  it('si ninguna cantidad se conoce, ninguna barra tiene ancho propio', () => {
    expect(anchoDeBarra(5, 0)).toBe(0);
  });
});

describe('datosDeZona', () => {
  const zonas = [
    { id: 'tayrona', nombre: 'Parque Tayrona', areaProtegida: true, altaAfluencia: true, activa: true, buenasPracticas: [] },
    { id: 'minca', nombre: 'Minca', areaProtegida: false, altaAfluencia: false, activa: true, buenasPracticas: [] },
  ] as Zona[];

  it('toma el nombre y las marcas de la lista de zonas', () => {
    expect(datosDeZona('tayrona', zonas)).toEqual({ nombre: 'Parque Tayrona', areaProtegida: true, altaAfluencia: true });
    expect(datosDeZona('minca', zonas)).toEqual({ nombre: 'Minca', areaProtegida: false, altaAfluencia: false });
  });

  it('una zona que no está en la lista (inactiva) no tiene nombre ni marcas', () => {
    expect(datosDeZona('vieja', zonas)).toEqual({ nombre: null, areaProtegida: false, altaAfluencia: false });
  });

  it('sin lista de zonas tampoco', () => {
    expect(datosDeZona('tayrona', [])).toEqual({ nombre: null, areaProtegida: false, altaAfluencia: false });
  });
});
