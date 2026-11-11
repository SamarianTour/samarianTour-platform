import { AlertaSesgo, PrestadorAdmin } from '../../core/api/modelos-nucleo';
import {
  codigoDelPrestador,
  nombreDelPrestador,
  ordenarPorIndice,
} from './alertas-sesgo';

function alerta(id: string, indice: number, actualizadaEn = '2026-11-02T09:00:00Z'): AlertaSesgo {
  return {
    prestadorId: id,
    indice,
    porcentajeApariciones: 30,
    porcentajeOfertas: 10,
    apariciones: 450,
    recomendaciones: 300,
    abiertaEn: '2026-11-01T09:00:00Z',
    actualizadaEn,
  };
}

function prestador(id: string, razonSocial?: string): PrestadorAdmin {
  return {
    id,
    numeroRnt: '910005',
    razonSocial,
    tipo: 'AGENCIA_OPERADORA',
    estadoRnt: 'ACTIVO',
    registradoEn: '2026-10-30T14:00:00Z',
  };
}

describe('alertas de sesgo', () => {

  describe('ordenarPorIndice', () => {
    it('pone primero la de mayor índice', () => {
      const orden = ordenarPorIndice([alerta('a', 1.6), alerta('b', 3), alerta('c', 2.2)]);

      expect(orden.map((a) => a.prestadorId)).toEqual(['b', 'c', 'a']);
    });

    it('a igual índice, va primero la actualizada más recientemente', () => {
      const orden = ordenarPorIndice([
        alerta('vieja', 2, '2026-11-01T09:00:00Z'),
        alerta('nueva', 2, '2026-11-03T09:00:00Z'),
      ]);

      expect(orden.map((a) => a.prestadorId)).toEqual(['nueva', 'vieja']);
    });

    it('no modifica la lista original', () => {
      const lista = [alerta('a', 1.6), alerta('b', 3)];
      const copia = [...lista];

      ordenarPorIndice(lista);

      expect(lista).toEqual(copia);
    });
  });

  describe('nombreDelPrestador', () => {
    const prestadores = [prestador('p1', ' TURISMO SIERRA NEVADA LTDA. '), prestador('p2', ''), prestador('p3')];

    it('es la razón social del prestador, sin espacios en los extremos', () => {
      expect(nombreDelPrestador('p1', prestadores)).toBe('TURISMO SIERRA NEVADA LTDA.');
    });

    it('sin razón social (vacía o ausente) no hay nombre', () => {
      expect(nombreDelPrestador('p2', prestadores)).toBeNull();
      expect(nombreDelPrestador('p3', prestadores)).toBeNull();
    });

    it('un prestador que no está en la lista no tiene nombre', () => {
      expect(nombreDelPrestador('otro', prestadores)).toBeNull();
      expect(nombreDelPrestador('p1', [])).toBeNull();
    });
  });

  describe('codigoDelPrestador', () => {
    it('son las ocho primeras letras del identificador, en mayúsculas', () => {
      expect(codigoDelPrestador('10000000-0000-0000-0000-000000000001')).toBe('10000000');
      expect(codigoDelPrestador('abcdef12-3456')).toBe('ABCDEF12');
    });
  });
});
