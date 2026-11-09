import { ClaveDeIdempotencia, nuevaClave } from './idempotencia';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe('nuevaClave', () => {
  it('es un UUID v4', () => {
    expect(nuevaClave()).toMatch(UUID);
  });

  it('cada llamada da una clave distinta', () => {
    expect(new Set(Array.from({ length: 50 }, () => nuevaClave())).size).toBe(50);
  });

  it('sin crypto.randomUUID sigue dando un UUID v4', () => {
    const original = globalThis.crypto.randomUUID;
    Object.defineProperty(globalThis.crypto, 'randomUUID', {
      value: undefined,
      configurable: true,
    });
    try {
      expect(nuevaClave()).toMatch(UUID);
    } finally {
      Object.defineProperty(globalThis.crypto, 'randomUUID', {
        value: original,
        configurable: true,
      });
    }
  });
});

describe('ClaveDeIdempotencia', () => {
  it('un reintento con el mismo cuerpo usa la misma clave', () => {
    const clave = new ClaveDeIdempotencia();

    const primera = clave.para({ ofertaId: 'b1', personas: 2 });
    const reintento = clave.para({ ofertaId: 'b1', personas: 2 });

    expect(reintento).toBe(primera);
  });

  it('si el usuario cambia un dato antes de reintentar, la clave cambia (si no, el núcleo respondería 422)', () => {
    const clave = new ClaveDeIdempotencia();

    const primera = clave.para({ ofertaId: 'b1', personas: 2 });
    const corregida = clave.para({ ofertaId: 'b1', personas: 3 });

    expect(corregida).not.toBe(primera);
  });

  it('vuelve a la misma clave solo mientras el cuerpo no cambie de nuevo', () => {
    const clave = new ClaveDeIdempotencia();

    clave.para({ personas: 2 });
    const segunda = clave.para({ personas: 3 });

    expect(clave.para({ personas: 3 })).toBe(segunda);
  });

  it('después de reiniciar, el mismo cuerpo es una operación nueva', () => {
    const clave = new ClaveDeIdempotencia();
    const primera = clave.para({ personas: 2 });

    clave.reiniciar();

    expect(clave.para({ personas: 2 })).not.toBe(primera);
  });

  it('sirve también para un cuerpo sin datos, como confirmar', () => {
    const clave = new ClaveDeIdempotencia();

    expect(clave.para({ decision: 'CONFIRMAR' })).toBe(clave.para({ decision: 'CONFIRMAR' }));
    expect(clave.para({ decision: 'DECLINAR' })).toMatch(UUID);
  });
});
