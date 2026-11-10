import { DistribucionCategoria } from '../../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../../core/idioma/idioma';
import {
  FilaCategoria,
  NOMBRE_DE_CATEGORIA,
  categoriasMayores,
  cifrasDeDistribucion,
  distribucionVacia,
  filasDeDistribucion,
  listaDeNombres,
} from './distribucion';

/** El ejemplo del contrato, con las categorías en el orden en que las manda el núcleo. */
const DISTRIBUCION: DistribucionCategoria = {
  desde: '2026-10-01',
  hasta: '2026-10-31',
  total: 42,
  categorias: [
    { categoria: 'GUIA_INDEPENDIENTE', solicitudes: 6, porcentaje: 14.3 },
    { categoria: 'MICRO_PEQUENO', solicitudes: 25, porcentaje: 59.5, pagadas: 10 },
    { categoria: 'MEDIANO', solicitudes: 11, porcentaje: 26.2, pagadas: 6 },
    { categoria: 'GRANDE', solicitudes: 0, porcentaje: 0, pagadas: 0 },
  ],
};

describe('filasDeDistribucion', () => {
  it('van de los prestadores más grandes a los más pequeños, no en el orden del núcleo', () => {
    expect(filasDeDistribucion(DISTRIBUCION).map((f) => f.categoria)).toEqual([
      'GRANDE',
      'MEDIANO',
      'MICRO_PEQUENO',
      'GUIA_INDEPENDIENTE',
    ]);
  });

  it('"sin categoría" va al final y una categoría desconocida también', () => {
    const filas = filasDeDistribucion({
      ...DISTRIBUCION,
      categorias: [
        { categoria: 'SIN_CATEGORIA', solicitudes: 4, porcentaje: 8, pagadas: 3 },
        ...DISTRIBUCION.categorias,
      ],
    });

    expect(filas[filas.length - 1].categoria).toBe('SIN_CATEGORIA');
  });

  it('el porcentaje sobre el total es el del núcleo', () => {
    const micro = filasDeDistribucion(DISTRIBUCION).find((f) => f.categoria === 'MICRO_PEQUENO')!;

    expect(micro.porcentaje).toBe(59.5);
  });

  it('la tasa de pago es qué parte de las solicitudes de la categoría se pagó, con un decimal', () => {
    const filas = filasDeDistribucion(DISTRIBUCION);
    const tasa = (categoria: string) => filas.find((f) => f.categoria === categoria)?.tasaDePago;

    expect(tasa('MICRO_PEQUENO')).toBe(40);
    expect(tasa('MEDIANO')).toBe(54.5);
  });

  it('sin solicitudes no hay tasa que calcular, aunque las pagadas sean 0', () => {
    expect(filasDeDistribucion(DISTRIBUCION).find((f) => f.categoria === 'GRANDE')?.tasaDePago).toBeNull();
  });

  it('un conteo omitido sigue omitido y no se calcula con él', () => {
    // Los guías tienen 6 solicitudes pero entre 1 y 2 pagadas: el núcleo no entrega ese conteo.
    const guias = filasDeDistribucion(DISTRIBUCION).find((f) => f.categoria === 'GUIA_INDEPENDIENTE')!;

    expect(guias.solicitudes).toBe(6);
    expect(guias.pagadas).toBeNull();
    expect(guias.tasaDePago).toBeNull();
  });

  it('con las solicitudes omitidas (1 o 2) todo lo demás de la fila queda sin dato', () => {
    const [fila] = filasDeDistribucion({ ...DISTRIBUCION, categorias: [{ categoria: 'GRANDE' }] });

    expect(fila).toEqual({ categoria: 'GRANDE', solicitudes: null, porcentaje: null, pagadas: null, tasaDePago: null });
  });
});

describe('categoriasMayores', () => {
  const fila = (categoria: FilaCategoria['categoria'], solicitudes: number | null): FilaCategoria => ({
    categoria,
    solicitudes,
    porcentaje: null,
    pagadas: null,
    tasaDePago: null,
  });

  it('es la de más solicitudes', () => {
    expect(categoriasMayores(filasDeDistribucion(DISTRIBUCION))).toEqual(['MICRO_PEQUENO']);
  });

  it('con un empate salen todas', () => {
    expect(categoriasMayores([fila('GRANDE', 5), fila('MEDIANO', 5), fila('MICRO_PEQUENO', 2)])).toEqual(['GRANDE', 'MEDIANO']);
  });

  it('una categoría de conteo omitido nunca gana: se desconoce y es menor que 3', () => {
    expect(categoriasMayores([fila('GRANDE', null), fila('MEDIANO', 4)])).toEqual(['MEDIANO']);
  });

  it('si ninguna cantidad se conoce o todas son cero, no hay mayor', () => {
    expect(categoriasMayores([fila('GRANDE', null), fila('MEDIANO', null)])).toEqual([]);
    expect(categoriasMayores([fila('GRANDE', 0), fila('MEDIANO', 0)])).toEqual([]);
    expect(categoriasMayores([])).toEqual([]);
  });
});

describe('cifrasDeDistribucion', () => {
  it('el total es el del núcleo, no la suma de las categorías', () => {
    // Las categorías conocidas suman 42 aquí, pero en general las omitidas hacen que la suma salga de menos.
    const filas = filasDeDistribucion(DISTRIBUCION);

    expect(cifrasDeDistribucion(DISTRIBUCION, filas).total).toBe(42);
    expect(cifrasDeDistribucion({ ...DISTRIBUCION, total: undefined }, filas).total).toBeNull();
  });

  it('cuenta las categorías con solicitudes, también las de conteo omitido', () => {
    const distribucion: DistribucionCategoria = {
      ...DISTRIBUCION,
      categorias: [
        { categoria: 'GRANDE', solicitudes: 0, porcentaje: 0, pagadas: 0 },
        { categoria: 'MEDIANO' },
        { categoria: 'MICRO_PEQUENO', solicitudes: 7, porcentaje: 70, pagadas: 3 },
        { categoria: 'GUIA_INDEPENDIENTE', solicitudes: 0, porcentaje: 0, pagadas: 0 },
      ],
    };

    expect(cifrasDeDistribucion(distribucion, filasDeDistribucion(distribucion)).categoriasConSolicitudes).toBe(2);
  });

  it('trae las categorías mayores', () => {
    expect(cifrasDeDistribucion(DISTRIBUCION, filasDeDistribucion(DISTRIBUCION)).mayores).toEqual(['MICRO_PEQUENO']);
  });
});

describe('distribucionVacia', () => {
  it('solo si el núcleo dice que el total es cero, no si fue omitido', () => {
    expect(distribucionVacia({ ...DISTRIBUCION, total: 0 })).toBe(true);
    expect(distribucionVacia({ ...DISTRIBUCION, total: undefined })).toBe(false);
    expect(distribucionVacia(DISTRIBUCION)).toBe(false);
  });
});

describe('los nombres', () => {
  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('son los mismos del panel del prestador y siguen el idioma', () => {
    expect(NOMBRE_DE_CATEGORIA.MICRO_PEQUENO).toBe('Micro o pequeña empresa');
    expect(NOMBRE_DE_CATEGORIA.GUIA_INDEPENDIENTE).toBe('Guía independiente');
    expect(NOMBRE_DE_CATEGORIA.SIN_CATEGORIA).toBe('Sin categoría');
    establecerIdioma('en', { recordar: false });
    expect(NOMBRE_DE_CATEGORIA.MICRO_PEQUENO).toBe('Micro or small business');
    expect(NOMBRE_DE_CATEGORIA.SIN_CATEGORIA).toBe('No category');
  });

  it('una lista de nombres se escribe según el idioma', () => {
    expect(listaDeNombres(['Gran empresa', 'Mediana empresa'])).toBe('Gran empresa y Mediana empresa');
    expect(listaDeNombres(['Gran empresa'])).toBe('Gran empresa');
    establecerIdioma('en', { recordar: false });
    expect(listaDeNombres(['Large business', 'Medium-sized business'])).toBe('Large business and Medium-sized business');
  });
});
