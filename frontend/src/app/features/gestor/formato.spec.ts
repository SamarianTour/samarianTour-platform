import { establecerIdioma } from '../../core/idioma/idioma';
import { formatoCantidad, formatoPorcentaje } from './formato';

describe('formatoCantidad', () => {
  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('una cantidad que el núcleo no entrega (1 o 2) se escribe "menos de 3"', () => {
    expect(formatoCantidad(null)).toBe('menos de 3');
    expect(formatoCantidad(undefined)).toBe('menos de 3');
  });

  it('un cero es un cero, no "menos de 3"', () => {
    expect(formatoCantidad(0)).toBe('0');
  });

  it('separa los miles según el idioma', () => {
    expect(formatoCantidad(18)).toBe('18');
    expect(formatoCantidad(1234)).toBe('1.234');
    establecerIdioma('en', { recordar: false });
    expect(formatoCantidad(1234)).toBe('1,234');
  });

  it('"menos de 3" cambia de idioma', () => {
    establecerIdioma('en', { recordar: false });

    expect(formatoCantidad(null)).toBe('fewer than 3');
  });
});

describe('formatoPorcentaje', () => {
  afterEach(() => establecerIdioma('es', { recordar: false }));

  const normal = (texto: string) => texto.replace(/[\u00a0\u202f]/g, ' ');

  it('escribe un decimal y el signo según el idioma', () => {
    expect(normal(formatoPorcentaje(71.4))).toBe('71,4%');
    expect(normal(formatoPorcentaje(100))).toBe('100,0%');
    expect(normal(formatoPorcentaje(0))).toBe('0,0%');
    establecerIdioma('en', { recordar: false });
    expect(normal(formatoPorcentaje(71.4))).toBe('71.4%');
  });

  it('no inventa decimales ni redondea de más', () => {
    expect(normal(formatoPorcentaje(14.3))).toBe('14,3%');
    expect(normal(formatoPorcentaje(59.5))).toBe('59,5%');
  });

  it('sin porcentaje escribe "sin dato", no 0%', () => {
    expect(formatoPorcentaje(null)).toBe('sin dato');
    expect(formatoPorcentaje(undefined)).toBe('sin dato');
    establecerIdioma('en', { recordar: false });
    expect(formatoPorcentaje(null)).toBe('no data');
  });
});
