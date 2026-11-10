import { PagoTardio } from '../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../core/idioma/idioma';
import {
  EXPLICACION_MOTIVO_PAGO,
  ETIQUETA_MOTIVO_PAGO,
  codigoDeSolicitud,
  contarSinAvisar,
  ordenarPagosTardios,
  totalPorDevolver,
} from './pagos-tardios';

function pago(id: string, monto: number, extra: Partial<PagoTardio> = {}): PagoTardio {
  return {
    id,
    solicitudId: '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20',
    referencia: `ST-${id}`,
    transaccionId: `T-${id}`,
    monto,
    motivo: 'PAGO_TARDIO',
    registradoEn: '2026-10-26T19:31:00Z',
    avisado: true,
    ...extra,
  };
}

describe('motivos de un pago tardío', () => {
  beforeEach(() => establecerIdioma('es', { recordar: false }));

  it('cada uno tiene su nombre y su explicación', () => {
    expect(ETIQUETA_MOTIVO_PAGO.PAGO_TARDIO).toBe('Pago tardío');
    expect(ETIQUETA_MOTIVO_PAGO.MONTO_DISTINTO).toBe('Monto distinto');
    expect(EXPLICACION_MOTIVO_PAGO.PAGO_TARDIO).toContain('vencida, cerrada o ya pagada');
    expect(EXPLICACION_MOTIVO_PAGO.MONTO_DISTINTO).toContain('otro monto');
  });

  it('siguen el idioma', () => {
    establecerIdioma('en', { recordar: false });

    expect(ETIQUETA_MOTIVO_PAGO.PAGO_TARDIO).toBe('Late payment');
    expect(ETIQUETA_MOTIVO_PAGO.MONTO_DISTINTO).toBe('Different amount');
  });
});

describe('ordenarPagosTardios', () => {
  it('pone primero el más reciente', () => {
    const orden = ordenarPagosTardios([
      pago('a', 100, { registradoEn: '2026-10-20T10:00:00Z' }),
      pago('b', 100, { registradoEn: '2026-10-27T10:00:00Z' }),
      pago('c', 100, { registradoEn: '2026-10-22T10:00:00Z' }),
    ]);

    expect(orden.map((p) => p.id)).toEqual(['b', 'c', 'a']);
  });

  it('no modifica la lista original', () => {
    const lista = [pago('a', 1, { registradoEn: '2026-10-20T10:00:00Z' }), pago('b', 1)];
    const copia = [...lista];

    ordenarPagosTardios(lista);

    expect(lista).toEqual(copia);
  });
});

describe('totalPorDevolver', () => {
  it('suma lo que aprobó la pasarela en cada pago', () => {
    expect(totalPorDevolver([pago('a', 300000), pago('b', 150000), pago('c', 50000)])).toBe(500000);
  });

  it('sin pagos no hay nada que devolver', () => {
    expect(totalPorDevolver([])).toBe(0);
  });
});

describe('contarSinAvisar', () => {
  it('cuenta los pagos que todavía no se avisaron por WhatsApp', () => {
    expect(contarSinAvisar([pago('a', 1), pago('b', 1, { avisado: false }), pago('c', 1, { avisado: false })])).toBe(2);
    expect(contarSinAvisar([pago('a', 1)])).toBe(0);
    expect(contarSinAvisar([])).toBe(0);
  });
});

describe('codigoDeSolicitud', () => {
  it('son las ocho primeras letras del identificador, en mayúsculas', () => {
    expect(codigoDeSolicitud('6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20')).toBe('6F1C2A9E');
  });
});
