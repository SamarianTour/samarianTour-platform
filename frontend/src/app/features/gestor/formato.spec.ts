import { establecerIdioma } from '../../core/idioma/idioma';
import { formatoCantidad } from './formato';

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
