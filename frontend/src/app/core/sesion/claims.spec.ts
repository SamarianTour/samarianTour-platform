import { leerClaims } from './claims';
import { crearToken } from './testing';

describe('leerClaims', () => {
  it('lee el usuario, el rol y la sesión del token', () => {
    const claims = leerClaims(
      crearToken({ sub: 'u1', rol: 'TURISTA', sid: 's1', exp: 1_800_000_000 }),
    );

    expect(claims).toEqual({ sub: 'u1', rol: 'TURISTA', sid: 's1', exp: 1_800_000_000 });
  });

  it('lee el tipo de prestador cuando el token lo trae', () => {
    const claims = leerClaims(
      crearToken({ sub: 'u2', rol: 'PRESTADOR', tipoPrestador: 'GUIA_INDEPENDIENTE' }),
    );

    expect(claims?.tipoPrestador).toBe('GUIA_INDEPENDIENTE');
  });

  it('deja sin tipo de prestador al prestador que todavía no registra su RNT', () => {
    expect(leerClaims(crearToken({ sub: 'u3', rol: 'PRESTADOR' }))?.tipoPrestador).toBeUndefined();
  });

  it('entiende caracteres con tilde y los símbolos propios de base64url', () => {
    const claims = leerClaims(
      crearToken({ sub: 'u4', rol: 'TURISTA', nombre: '¿Mañana? ñandú >>>???~~~' }),
    );

    expect(claims?.sub).toBe('u4');
  });

  it.each(['', 'no-es-un-token', 'a.b', 'a.b.c.d', 'x.%%%.y', `a.${btoa('no es json')}.c`])(
    'devuelve null con un texto que no es un token: %j',
    (texto) => {
      expect(leerClaims(texto)).toBeNull();
    },
  );

  it('devuelve null si faltan el usuario o el rol', () => {
    expect(leerClaims(crearToken({ rol: 'TURISTA' }))).toBeNull();
    expect(leerClaims(crearToken({ sub: 'u5' }))).toBeNull();
    expect(leerClaims(crearToken(['sub']))).toBeNull();
  });
});
