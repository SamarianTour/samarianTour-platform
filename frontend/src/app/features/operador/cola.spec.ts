import { SolicitudCola } from '../../core/api/modelos-nucleo';
import { codigoCorto, nivelDeUrgencia, ordenarPorUrgencia } from './cola';

const AHORA = Date.parse('2026-10-26T15:00:00Z');
const HORA = 3_600_000;
const en = (ms: number) => new Date(AHORA + ms).toISOString();

function solicitud(id: string, extra: Partial<SolicitudCola> = {}): SolicitudCola {
  return {
    id,
    oferta: { id: 'b1', tipo: 'ACTIVIDAD', nombre: `Oferta ${id}`, zonaId: 'z1' },
    fechaInicio: '2026-10-28',
    personas: 2,
    estado: 'PENDIENTE',
    proximaAVencer: false,
    venceEn: en(20 * HORA),
    creadaEn: en(-4 * HORA),
    ...extra,
  };
}

describe('nivelDeUrgencia', () => {
  it('con más de 6 horas está en plazo', () => {
    expect(nivelDeUrgencia(solicitud('a', { venceEn: en(6 * HORA + 60_000) }), AHORA)).toBe(
      'en-plazo',
    );
  });

  it('con 6 horas o menos es urgente (RF-36)', () => {
    expect(nivelDeUrgencia(solicitud('a', { venceEn: en(6 * HORA) }), AHORA)).toBe('urgente');
    expect(nivelDeUrgencia(solicitud('a', { venceEn: en(10 * 60_000) }), AHORA)).toBe('urgente');
  });

  it('es urgente si la API la marca, aunque el reloj de la pantalla vaya atrasado', () => {
    expect(
      nivelDeUrgencia(solicitud('a', { proximaAVencer: true, venceEn: en(20 * HORA) }), AHORA),
    ).toBe('urgente');
  });

  it('con el plazo terminado está vencida', () => {
    expect(nivelDeUrgencia(solicitud('a', { venceEn: en(-60_000) }), AHORA)).toBe('vencida');
    expect(nivelDeUrgencia(solicitud('a', { venceEn: en(0) }), AHORA)).toBe('vencida');
  });

  it('una solicitud que ya no está pendiente (historial) nunca es urgente ni vencida', () => {
    expect(
      nivelDeUrgencia(
        solicitud('a', { estado: 'ATENDIDA', proximaAVencer: true, venceEn: en(-HORA) }),
        AHORA,
      ),
    ).toBe('en-plazo');
  });

  it('sin fecha de vencimiento solo es urgente si la API lo dice', () => {
    expect(nivelDeUrgencia(solicitud('a', { venceEn: undefined }), AHORA)).toBe('en-plazo');
    expect(
      nivelDeUrgencia(solicitud('a', { venceEn: undefined, proximaAVencer: true }), AHORA),
    ).toBe('urgente');
  });
});

describe('ordenarPorUrgencia', () => {
  it('pone primero las urgentes, luego las que están en plazo y al final las vencidas', () => {
    const lista = [
      solicitud('vencida', { venceEn: en(-HORA) }),
      solicitud('plazo', { venceEn: en(10 * HORA) }),
      solicitud('urgente', { venceEn: en(2 * HORA) }),
    ];

    expect(ordenarPorUrgencia(lista, AHORA).map((s) => s.id)).toEqual([
      'urgente',
      'plazo',
      'vencida',
    ]);
  });

  it('dentro de un grupo, la que vence antes va antes', () => {
    const lista = [
      solicitud('c', { venceEn: en(5 * HORA) }),
      solicitud('a', { venceEn: en(1 * HORA) }),
      solicitud('b', { venceEn: en(3 * HORA) }),
    ];

    expect(ordenarPorUrgencia(lista, AHORA).map((s) => s.id)).toEqual(['a', 'b', 'c']);
  });

  it('si vencen a la vez, la que llegó antes va antes', () => {
    const lista = [
      solicitud('nueva', { venceEn: en(10 * HORA), creadaEn: en(-1 * HORA) }),
      solicitud('vieja', { venceEn: en(10 * HORA), creadaEn: en(-5 * HORA) }),
    ];

    expect(ordenarPorUrgencia(lista, AHORA).map((s) => s.id)).toEqual(['vieja', 'nueva']);
  });

  it('una solicitud sin vencimiento va después de las que sí lo tienen en su grupo', () => {
    const lista = [
      solicitud('sin', { venceEn: undefined }),
      solicitud('con', { venceEn: en(15 * HORA) }),
    ];

    expect(ordenarPorUrgencia(lista, AHORA).map((s) => s.id)).toEqual(['con', 'sin']);
  });

  it('no modifica la lista original', () => {
    const lista = [solicitud('b', { venceEn: en(9 * HORA) }), solicitud('a', { venceEn: en(HORA) })];
    const copia = [...lista];

    ordenarPorUrgencia(lista, AHORA);

    expect(lista).toEqual(copia);
  });
});

describe('codigoCorto', () => {
  it('son las ocho primeras letras del identificador, en mayúsculas', () => {
    expect(codigoCorto('6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20')).toBe('6F1C2A9E');
  });
});
