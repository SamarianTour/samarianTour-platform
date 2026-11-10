import { AvisoOperador } from '../../core/api/modelos-nucleo';
import { claveDeTipoDeAviso, resumenDeAviso } from './avisos';

function aviso(datos: Record<string, string>): AvisoOperador {
  return {
    id: '0b7e3c55-8f0a-4d63-b2a1-7c9d4e5f6a10',
    tipo: 'NUEVA_SOLICITUD',
    solicitudId: '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20',
    datos,
    creadoEn: '2026-10-26T15:00:00Z',
  };
}

describe('resumenDeAviso', () => {
  it('toma la oferta, la fecha, la hora y las personas del aviso de una solicitud nueva', () => {
    expect(
      resumenDeAviso(
        aviso({
          nombreOferta: 'Caminata a Cabo San Juan del Guía',
          fechaInicio: '2026-10-28',
          horaSalida: '07:00:00',
          personas: '2',
          estado: 'PENDIENTE',
        }),
      ),
    ).toEqual({
      nombreOferta: 'Caminata a Cabo San Juan del Guía',
      fechaInicio: '2026-10-28',
      hora: '07:00',
      personas: 2,
    });
  });

  it('en un restaurante usa la hora de la reserva', () => {
    expect(resumenDeAviso(aviso({ horaReserva: '19:30:00' })).hora).toBe('19:30');
  });

  it('lo que falta se omite, no se inventa', () => {
    expect(resumenDeAviso(aviso({}))).toEqual({
      nombreOferta: null,
      fechaInicio: null,
      hora: null,
      personas: null,
    });
  });

  it('un dato vacío o de solo espacios cuenta como ausente', () => {
    expect(resumenDeAviso(aviso({ nombreOferta: '   ', horaSalida: '' })).nombreOferta).toBeNull();
    expect(resumenDeAviso(aviso({ horaSalida: '' })).hora).toBeNull();
  });

  it.each(['dos', '0', '-3', '2.5', ''])('personas %j no es un número usable', (personas) => {
    expect(resumenDeAviso(aviso({ personas })).personas).toBeNull();
  });
});

describe('claveDeTipoDeAviso', () => {
  it('NUEVA_SOLICITUD tiene su título', () => {
    expect(claveDeTipoDeAviso('NUEVA_SOLICITUD')).toBe('operador.avisos.tipo.NUEVA_SOLICITUD');
  });

  it('cualquier otro tipo es un aviso genérico', () => {
    expect(claveDeTipoDeAviso('SOLICITUD_PAGADA')).toBe('operador.avisos.tipo.otro');
  });
});
