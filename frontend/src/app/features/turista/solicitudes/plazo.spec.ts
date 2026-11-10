import { UMBRAL_URGENTE_MS, plazoRestante } from './plazo';

const AHORA = Date.parse('2026-10-26T15:00:00Z');
const en = (ms: number) => new Date(AHORA + ms).toISOString();
const MINUTO = 60_000;
const HORA = 60 * MINUTO;

describe('plazoRestante', () => {
  it('sin fecha no hay plazo: la API la omite en una solicitud cerrada', () => {
    expect(plazoRestante(undefined, AHORA).nivel).toBe('sin-plazo');
    expect(plazoRestante(null, AHORA).nivel).toBe('sin-plazo');
    expect(plazoRestante('', AHORA).nivel).toBe('sin-plazo');
  });

  it('una fecha que no se entiende tampoco es un plazo', () => {
    expect(plazoRestante('mañana', AHORA).nivel).toBe('sin-plazo');
  });

  it('con más de 6 horas está vigente y se parte en días, horas y minutos', () => {
    const plazo = plazoRestante(en(26 * HORA + 30 * MINUTO), AHORA);

    expect(plazo).toMatchObject({ nivel: 'vigente', dias: 1, horas: 2, minutos: 30 });
    expect(plazo.restanteMs).toBe(26 * HORA + 30 * MINUTO);
  });

  it('con 6 horas o menos es urgente', () => {
    expect(plazoRestante(en(UMBRAL_URGENTE_MS), AHORA).nivel).toBe('urgente');
    expect(plazoRestante(en(UMBRAL_URGENTE_MS + 1000), AHORA).nivel).toBe('vigente');
    expect(plazoRestante(en(30 * MINUTO), AHORA)).toMatchObject({
      nivel: 'urgente',
      dias: 0,
      horas: 0,
      minutos: 30,
    });
  });

  it('los minutos se redondean hacia arriba: nunca dice 0 min con tiempo por delante', () => {
    expect(plazoRestante(en(1000), AHORA)).toMatchObject({
      nivel: 'urgente',
      horas: 0,
      minutos: 1,
    });
    expect(plazoRestante(en(59 * MINUTO + 30_000), AHORA)).toMatchObject({ horas: 1, minutos: 0 });
  });

  it('justo en el límite o después, venció', () => {
    expect(plazoRestante(en(0), AHORA)).toMatchObject({ nivel: 'vencido', restanteMs: 0 });
    expect(plazoRestante(en(-HORA), AHORA)).toMatchObject({ nivel: 'vencido', restanteMs: 0 });
  });

  it('el plazo de 24 horas de una solicitud recién enviada está vigente', () => {
    expect(plazoRestante(en(24 * HORA), AHORA)).toMatchObject({
      nivel: 'vigente',
      dias: 1,
      horas: 0,
    });
  });
});
