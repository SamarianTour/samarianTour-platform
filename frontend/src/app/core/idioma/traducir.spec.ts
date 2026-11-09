import type { Texto } from './diccionario';
import { Idioma, establecerIdioma } from './idioma';
import { traducir, traducirDe } from './traducir';

const TEXTOS_DE_PRUEBA: Record<Idioma, Record<string, Texto>> = {
  es: {
    saludo: 'Hola',
    aviso: 'Hola {nombre}, tienes {n} avisos',
    repetido: '{n} de {n}',
    cosas: { uno: '{n} cosa', otros: '{n} cosas' },
    soloEnEspanol: 'Solo en español',
  },
  en: {
    saludo: 'Hello',
    aviso: 'Hello {nombre}, you have {n} alerts',
    repetido: '{n} of {n}',
    cosas: { uno: '{n} thing', otros: '{n} things' },
  },
};

const de = (idioma: Idioma, clave: string, parametros?: Record<string, string | number>) =>
  traducirDe(TEXTOS_DE_PRUEBA, idioma, clave, parametros);

describe('traducirDe', () => {
  it('devuelve el texto en el idioma pedido', () => {
    expect(de('es', 'saludo')).toBe('Hola');
    expect(de('en', 'saludo')).toBe('Hello');
  });

  it('reemplaza los parámetros', () => {
    expect(de('es', 'aviso', { nombre: 'Laura', n: 3 })).toBe('Hola Laura, tienes 3 avisos');
    expect(de('en', 'aviso', { nombre: 'Laura', n: 3 })).toBe('Hello Laura, you have 3 alerts');
  });

  it('reemplaza un parámetro cada vez que aparece', () => {
    expect(de('es', 'repetido', { n: 4 })).toBe('4 de 4');
  });

  it('un cero es un valor, no un parámetro ausente', () => {
    expect(de('es', 'aviso', { nombre: 'Laura', n: 0 })).toBe('Hola Laura, tienes 0 avisos');
  });

  it('un parámetro que no se entregó queda a la vista', () => {
    expect(de('es', 'aviso', { n: 2 })).toBe('Hola {nombre}, tienes 2 avisos');
    expect(de('es', 'aviso')).toBe('Hola {nombre}, tienes {n} avisos');
  });

  it('los caracteres especiales de los parámetros se muestran tal cual', () => {
    expect(de('es', 'aviso', { nombre: '$1 & $& $$', n: 1 })).toBe(
      'Hola $1 & $& $$, tienes 1 avisos',
    );
  });

  describe('plural', () => {
    it.each([
      [1, '1 cosa'],
      [0, '0 cosas'],
      [2, '2 cosas'],
      [1.5, '1.5 cosas'],
      [21, '21 cosas'],
    ])('en español n = %s es "%s"', (n, esperado) => {
      expect(de('es', 'cosas', { n })).toBe(esperado);
    });

    it.each([
      [1, '1 thing'],
      [0, '0 things'],
      [2, '2 things'],
    ])('en inglés n = %s es "%s"', (n, esperado) => {
      expect(de('en', 'cosas', { n })).toBe(esperado);
    });

    it('sin la cantidad usa la forma plural', () => {
      expect(de('es', 'cosas')).toBe('{n} cosas');
      expect(de('es', 'cosas', { n: '1' })).toBe('1 cosas');
    });
  });

  describe('lo que falta', () => {
    it('si la clave no está en el idioma pedido usa el español', () => {
      expect(de('en', 'soloEnEspanol')).toBe('Solo en español');
    });

    it('si no está en ninguno devuelve la clave, para que el hueco se vea', () => {
      expect(de('es', 'no.existe')).toBe('no.existe');
      expect(de('en', 'no.existe')).toBe('no.existe');
    });
  });
});

describe('traducir (con los diccionarios reales)', () => {
  it('sigue el idioma vigente', () => {
    expect(traducir('comun.cargando')).toBe('Cargando…');

    establecerIdioma('en', { recordar: false });
    expect(traducir('comun.cargando')).toBe('Loading…');

    establecerIdioma('es', { recordar: false });
    expect(traducir('comun.cargando')).toBe('Cargando…');
  });

  it('arma textos con parámetros', () => {
    expect(traducir('comun.pagina', { pagina: 2, total: 5 })).toBe('Página 2 de 5');

    establecerIdioma('en', { recordar: false });
    expect(traducir('comun.pagina', { pagina: 2, total: 5 })).toBe('Page 2 of 5');
  });

  it('elige singular o plural según la cantidad en cada idioma', () => {
    expect(traducir('comun.minutos', { n: 1 })).toBe('1 minuto');
    expect(traducir('comun.minutos', { n: 15 })).toBe('15 minutos');

    establecerIdioma('en', { recordar: false });
    expect(traducir('comun.minutos', { n: 1 })).toBe('1 minute');
    expect(traducir('comun.minutos', { n: 15 })).toBe('15 minutes');
  });
});
