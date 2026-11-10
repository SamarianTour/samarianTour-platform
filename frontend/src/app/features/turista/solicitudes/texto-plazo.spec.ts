import { establecerIdioma } from '../../../core/idioma/idioma';
import { Plazo, plazoRestante } from './plazo';
import { avisoDePlazo, duracionTexto } from './texto-plazo';

const AHORA = Date.parse('2026-10-26T15:00:00Z');
const MINUTO = 60_000;
const HORA = 60 * MINUTO;
const plazo = (ms: number): Plazo => plazoRestante(new Date(AHORA + ms).toISOString(), AHORA);

describe('duracionTexto', () => {
  it('muestra solo las dos unidades más grandes', () => {
    expect(duracionTexto(plazo(26 * HORA + 30 * MINUTO))).toBe('1 d 2 h');
    expect(duracionTexto(plazo(2 * HORA + 30 * MINUTO))).toBe('2 h 30 min');
    expect(duracionTexto(plazo(45 * MINUTO))).toBe('45 min');
  });

  it('una hora exacta se dice con sus minutos en cero', () => {
    expect(duracionTexto(plazo(HORA))).toBe('1 h 0 min');
  });
});

describe('avisoDePlazo', () => {
  it('quién tiene que actuar depende del estado', () => {
    expect(avisoDePlazo('PENDIENTE', plazo(3 * HORA))).toBe(
      'El operador tiene 3 h 0 min para responder.',
    );
    expect(avisoDePlazo('ATENDIDA', plazo(3 * HORA))).toBe(
      'Te quedan 3 h 0 min para confirmar o declinar.',
    );
    expect(avisoDePlazo('CONFIRMADA', plazo(3 * HORA))).toBe('Te quedan 3 h 0 min para pagar.');
    expect(avisoDePlazo('PAGO_FALLIDO', plazo(3 * HORA))).toBe('Te quedan 3 h 0 min para pagar.');
  });

  it('un plazo vencido avisa que se está actualizando el estado', () => {
    expect(avisoDePlazo('ATENDIDA', plazo(-HORA))).toBe(
      'El plazo terminó. Estamos actualizando el estado.',
    );
  });

  it('sin plazo no hay aviso', () => {
    expect(avisoDePlazo('PAGADA', plazoRestante(undefined, AHORA))).toBeNull();
  });

  it('en inglés', () => {
    establecerIdioma('en', { recordar: false });

    expect(avisoDePlazo('PENDIENTE', plazo(3 * HORA))).toBe(
      'The operator has 3 h 0 min left to respond.',
    );
    expect(avisoDePlazo('ATENDIDA', plazo(45 * MINUTO))).toBe(
      'You have 45 min left to confirm or decline.',
    );
    expect(avisoDePlazo('CONFIRMADA', plazo(HORA))).toBe('You have 1 h 0 min left to pay.');
    expect(avisoDePlazo('ATENDIDA', plazo(-1))).toBe(
      'The deadline has passed. We are updating the status.',
    );
  });
});
