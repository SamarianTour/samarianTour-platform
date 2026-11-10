import { ESTADOS_HISTORIAL, ESTADO_INICIAL_HISTORIAL, estadoDelHistorial } from './historial';

describe('historial del operador', () => {
  it('ofrece todos los estados menos Pendiente, que es la cola', () => {
    expect(ESTADOS_HISTORIAL).toHaveLength(8);
    expect(ESTADOS_HISTORIAL).not.toContain('PENDIENTE');
    expect(ESTADOS_HISTORIAL).toEqual([
      'ATENDIDA',
      'CONFIRMADA',
      'NO_DISPONIBLE',
      'SIN_RESPUESTA',
      'DECLINADA',
      'PAGO_FALLIDO',
      'PAGADA',
      'VENCIDA',
    ]);
  });

  it('el estado inicial es uno de los del historial', () => {
    expect(ESTADOS_HISTORIAL).toContain(ESTADO_INICIAL_HISTORIAL);
  });

  it.each(['ATENDIDA', 'PAGADA', 'NO_DISPONIBLE', 'VENCIDA'])('acepta %s', (estado) => {
    expect(estadoDelHistorial(estado)).toBe(estado);
  });

  it.each([null, '', 'PENDIENTE', 'pagada', 'CUALQUIER_COSA'])(
    'con %j usa el estado inicial, sin llegar a la API',
    (valor) => {
      expect(estadoDelHistorial(valor)).toBe(ESTADO_INICIAL_HISTORIAL);
    },
  );
});
