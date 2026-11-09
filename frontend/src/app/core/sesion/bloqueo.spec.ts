import { establecerIdioma } from '../idioma/idioma';
import { mensajeDeBloqueo } from './bloqueo';

const AHORA = new Date('2026-11-08T15:00:00').getTime();
const enMinutos = (minutos: number, segundos = 0) =>
  new Date(AHORA + minutos * 60_000 + segundos * 1000).toISOString();

describe('mensajeDeBloqueo', () => {
  it('dice cuántos minutos faltan', () => {
    expect(mensajeDeBloqueo(enMinutos(15), AHORA)).toContain('en 15 minutos');
  });

  it('redondea hacia arriba para no prometer un desbloqueo antes de tiempo', () => {
    expect(mensajeDeBloqueo(enMinutos(4, 1), AHORA)).toContain('en 5 minutos');
  });

  it('usa el singular cuando falta un minuto o menos', () => {
    expect(mensajeDeBloqueo(enMinutos(0, 30), AHORA)).toContain('en 1 minuto,');
    expect(mensajeDeBloqueo(enMinutos(1), AHORA)).toContain('en 1 minuto,');
  });

  it('incluye la hora a la que se puede intentar de nuevo', () => {
    const mensaje = mensajeDeBloqueo(enMinutos(15), AHORA);

    expect(mensaje).toMatch(/a las 3:15\s?p\.\s?m\./);
  });

  it('si el bloqueo termina otro día, incluye el día', () => {
    const mensaje = mensajeDeBloqueo(new Date(AHORA + 10 * 3600_000).toISOString(), AHORA);

    expect(mensaje).toContain('9 de noviembre');
  });

  describe('en inglés', () => {
    beforeEach(() => establecerIdioma('en', { recordar: false }));

    it('dice cuántos minutos faltan, con singular y plural', () => {
      expect(mensajeDeBloqueo(enMinutos(15), AHORA)).toContain('in 15 minutes');
      expect(mensajeDeBloqueo(enMinutos(1), AHORA)).toContain('in 1 minute,');
    });

    it('incluye la hora a la que se puede intentar de nuevo', () => {
      expect(mensajeDeBloqueo(enMinutos(15), AHORA)).toMatch(/at 3:15\s?PM/);
    });

    it('si el bloqueo termina otro día, incluye el día', () => {
      const mensaje = mensajeDeBloqueo(new Date(AHORA + 10 * 3600_000).toISOString(), AHORA);

      expect(mensaje).toContain('on November 9');
    });

    it('da un mensaje general cuando la fecha ya pasó', () => {
      expect(mensajeDeBloqueo(new Date(AHORA - 1000).toISOString(), AHORA)).toBe(
        'Your account is locked after too many failed attempts. Try again in a few minutes.',
      );
    });
  });

  it.each([
    ['ya pasó', new Date(AHORA - 1000).toISOString()],
    ['es exactamente ahora', new Date(AHORA).toISOString()],
    ['no es una fecha', 'mañana'],
    ['está vacía', ''],
  ])('da un mensaje general cuando la fecha %s', (_caso, fecha) => {
    expect(mensajeDeBloqueo(fecha, AHORA)).toBe(
      'Tu cuenta está bloqueada por intentos fallidos. Inténtalo de nuevo en unos minutos.',
    );
  });
});
