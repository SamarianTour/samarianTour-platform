import { estadoSolicitudValues } from '../../../core/api/nucleo-api';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { TransicionSolicitud } from '../../../core/api/modelos-nucleo';
import {
  CLASE_DE_ESTADO_SOLICITUD,
  DESCRIPCION_ESTADO,
  ESTADOS_SOLICITUD,
  ETIQUETA_ACTOR,
  ETIQUETA_ESTADO,
  FILTROS_SOLICITUDES,
  accionPrincipal,
  coincideConFiltro,
  estaActiva,
  ordenarHistorial,
  pasoDe,
} from './estado-solicitud';

describe('estados de una solicitud', () => {
  it('son los nueve del contrato, sin que falte ni sobre ninguno', () => {
    expect([...ESTADOS_SOLICITUD].sort()).toEqual([...estadoSolicitudValues].sort());
    expect(ESTADOS_SOLICITUD).toHaveLength(9);
  });

  it('cada estado tiene su propia etiqueta, descripción y color', () => {
    const etiquetas = ESTADOS_SOLICITUD.map((estado) => ETIQUETA_ESTADO[estado]);
    expect(new Set(etiquetas).size).toBe(9);
    for (const estado of ESTADOS_SOLICITUD) {
      expect(DESCRIPCION_ESTADO[estado].length, estado).toBeGreaterThan(10);
      expect(CLASE_DE_ESTADO_SOLICITUD[estado], estado).toMatch(/^etiqueta--/);
    }
  });

  it('las etiquetas en español', () => {
    expect(ESTADOS_SOLICITUD.map((estado) => ETIQUETA_ESTADO[estado])).toEqual([
      'Pendiente',
      'Disponible, confirma',
      'Por pagar',
      'No disponible',
      'Sin respuesta',
      'Declinada',
      'Pago fallido',
      'Pagada',
      'Vencida',
    ]);
  });

  it('las etiquetas en inglés', () => {
    establecerIdioma('en', { recordar: false });

    expect(ESTADOS_SOLICITUD.map((estado) => ETIQUETA_ESTADO[estado])).toEqual([
      'Pending',
      'Available, confirm',
      'Awaiting payment',
      'Unavailable',
      'No response',
      'Declined',
      'Payment failed',
      'Paid',
      'Expired',
    ]);
    expect(ETIQUETA_ACTOR.TURISTA).toBe('You');
  });

  it('el estado de una solicitud atendida pide confirmar y se distingue del de una confirmada', () => {
    expect(ETIQUETA_ESTADO.ATENDIDA).not.toBe(ETIQUETA_ESTADO.CONFIRMADA);
    expect(CLASE_DE_ESTADO_SOLICITUD.ATENDIDA).not.toBe(CLASE_DE_ESTADO_SOLICITUD.CONFIRMADA);
  });

  it('los actores del historial tienen nombre', () => {
    expect(ETIQUETA_ACTOR.OPERADOR).toBe('Operador');
    expect(ETIQUETA_ACTOR.SISTEMA).toBe('Sistema');
    expect(ETIQUETA_ACTOR.PAGOS).toBe('Pagos');
  });
});

describe('filtros de la lista', () => {
  it('activas son las que siguen su curso; finalizadas, las cinco terminales', () => {
    const activas = ESTADOS_SOLICITUD.filter(estaActiva);
    const finalizadas = ESTADOS_SOLICITUD.filter((estado) => !estaActiva(estado));

    expect(activas).toEqual(['PENDIENTE', 'ATENDIDA', 'CONFIRMADA', 'PAGO_FALLIDO']);
    expect(finalizadas).toEqual([
      'NO_DISPONIBLE',
      'SIN_RESPUESTA',
      'DECLINADA',
      'PAGADA',
      'VENCIDA',
    ]);
  });

  it.each(ESTADOS_SOLICITUD)('"todas" incluye %s y cada estado está en un solo grupo', (estado) => {
    expect(coincideConFiltro(estado, 'todas')).toBe(true);
    expect(coincideConFiltro(estado, 'activas')).not.toBe(coincideConFiltro(estado, 'finalizadas'));
  });

  it('hay tres filtros', () => {
    expect(FILTROS_SOLICITUDES).toEqual(['todas', 'activas', 'finalizadas']);
  });
});

describe('acción principal de cada estado', () => {
  it.each([
    ['PENDIENTE', 'ver-oferta'],
    ['ATENDIDA', 'responder'],
    ['CONFIRMADA', 'pagar'],
    ['NO_DISPONIBLE', 'volver-a-solicitar'],
    ['SIN_RESPUESTA', 'volver-a-solicitar'],
    ['DECLINADA', 'ver-oferta'],
    ['PAGO_FALLIDO', 'ver-oferta'],
    ['PAGADA', 'opinar'],
    ['VENCIDA', 'volver-a-solicitar'],
  ] as const)('%s → %s', (estado, accion) => {
    expect(accionPrincipal(estado)).toBe(accion);
  });

  it('solo una solicitud confirmada se ofrece pagar (con el pago fallido llega en la pantalla de reintento)', () => {
    expect(ESTADOS_SOLICITUD.filter((estado) => accionPrincipal(estado) === 'pagar')).toEqual([
      'CONFIRMADA',
    ]);
  });

  it('solo una solicitud atendida se puede responder', () => {
    expect(ESTADOS_SOLICITUD.filter((estado) => accionPrincipal(estado) === 'responder')).toEqual([
      'ATENDIDA',
    ]);
  });
});

describe('ordenarHistorial', () => {
  const pasos: TransicionSolicitud[] = [
    { de: 'PENDIENTE', a: 'ATENDIDA', en: '2026-10-26T16:00:00Z', actor: 'OPERADOR' },
    { a: 'PENDIENTE', en: '2026-10-26T15:00:00Z', actor: 'TURISTA' },
    { de: 'ATENDIDA', a: 'CONFIRMADA', en: '2026-10-26T17:00:00Z', actor: 'TURISTA' },
  ];

  it('va de la más antigua a la más reciente', () => {
    expect(ordenarHistorial(pasos).map((paso) => paso.a)).toEqual([
      'PENDIENTE',
      'ATENDIDA',
      'CONFIRMADA',
    ]);
  });

  it('no modifica el original', () => {
    const antes = pasos.map((paso) => paso.a);

    ordenarHistorial(pasos);

    expect(pasos.map((paso) => paso.a)).toEqual(antes);
  });
});

describe('pasoDe (pasos del flujo)', () => {
  it.each([
    ['PENDIENTE', 'solicitar'],
    ['ATENDIDA', 'confirmar'],
    ['CONFIRMADA', 'pagar'],
    ['PAGO_FALLIDO', 'pagar'],
    ['PAGADA', 'listo'],
  ] as const)('%s → %s', (estado, paso) => {
    expect(pasoDe(estado)).toBe(paso);
  });

  it.each(['NO_DISPONIBLE', 'SIN_RESPUESTA', 'DECLINADA', 'VENCIDA'] as const)(
    '%s ya no avanza: sin paso',
    (estado) => {
      expect(pasoDe(estado)).toBeNull();
    },
  );

  it('todo estado activo o pagado tiene paso; ninguno terminal sin pagar lo tiene', () => {
    for (const estado of ESTADOS_SOLICITUD) {
      expect(pasoDe(estado) !== null, estado).toBe(estaActiva(estado) || estado === 'PAGADA');
    }
  });
});
