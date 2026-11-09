import { establecerIdioma } from '../core/idioma/idioma';
import { FechaPipe } from './fecha.pipe';
import { NumeroPipe } from './numero.pipe';
import { PrecioPipe } from './precio.pipe';

/** Espacios normales en lugar de los espacios duros que usa Intl, para escribir lo esperado a simple vista. */
const normal = (texto: string) => texto.replace(/[  ]/g, ' ');

describe('PrecioPipe', () => {
  const pipe = new PrecioPipe();

  it('en español: $ 150.000', () => {
    expect(normal(pipe.transform(150000))).toBe('$ 150.000');
  });

  it('en inglés: COP 150,000', () => {
    establecerIdioma('en', { recordar: false });

    expect(normal(pipe.transform(150000))).toBe('COP 150,000');
  });

  it('sin decimales y con miles en los dos idiomas', () => {
    expect(normal(pipe.transform(1234567.89))).toBe('$ 1.234.568');

    establecerIdioma('en', { recordar: false });

    expect(normal(pipe.transform(1234567.89))).toBe('COP 1,234,568');
  });

  it('agrega la unidad, y un precio ausente no deja nada', () => {
    expect(normal(pipe.transform(150000, 'por persona'))).toBe('$ 150.000 por persona');
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined, 'por persona')).toBe('');
  });

  it('un precio en inglés no se corta entre la sigla y el número', () => {
    establecerIdioma('en', { recordar: false });

    expect(pipe.transform(150000)).toContain(' ');
  });
});

describe('FechaPipe', () => {
  const pipe = new FechaPipe();
  // Hora local, para que la prueba no dependa de la zona horaria de quien la corre.
  const fecha = new Date(2026, 10, 5, 15, 30);

  it.each([
    ['corta', '5 nov 2026'],
    ['larga', '5 de noviembre de 2026'],
    ['conHora', '5 nov 2026, 3:30 p. m.'],
  ] as const)('en español, formato %s', (formato, esperado) => {
    expect(normal(pipe.transform(fecha, formato))).toBe(esperado);
  });

  it.each([
    ['corta', 'Nov 5, 2026'],
    ['larga', 'November 5, 2026'],
    ['conHora', 'Nov 5, 2026, 3:30 PM'],
  ] as const)('en inglés, formato %s', (formato, esperado) => {
    establecerIdioma('en', { recordar: false });

    expect(normal(pipe.transform(fecha, formato))).toBe(esperado);
  });

  it('el formato por defecto es el corto', () => {
    expect(normal(pipe.transform(fecha))).toBe('5 nov 2026');
  });

  it('acepta el texto ISO que entrega la API', () => {
    expect(pipe.transform('2026-11-05T12:00:00')).toContain('2026');
  });

  it('una fecha ausente no deja nada', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
    expect(pipe.transform('')).toBe('');
  });
});

describe('NumeroPipe', () => {
  const pipe = new NumeroPipe();

  it('usa la coma decimal en español y el punto en inglés', () => {
    expect(pipe.transform(4.7, 1)).toBe('4,7');

    establecerIdioma('en', { recordar: false });

    expect(pipe.transform(4.7, 1)).toBe('4.7');
  });

  it('fija los decimales: 5 con un decimal es 5,0', () => {
    expect(pipe.transform(5, 1)).toBe('5,0');
    expect(pipe.transform(5)).toBe('5');
  });

  it('un valor ausente no deja nada', () => {
    expect(pipe.transform(null, 1)).toBe('');
  });
});
