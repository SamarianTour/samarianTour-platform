import { CuentaCobro } from '../../../core/api/modelos-nucleo';
import {
  VALORES_CUENTA_VACIOS,
  ValoresCuenta,
  construirCuenta,
  esBilletera,
  normalizarNumero,
  pideEntidad,
  ultimosDigitos,
  validarCuenta,
  valoresDeCuenta,
} from './cuenta-cobro';

const BANCO: ValoresCuenta = {
  tipo: 'AHORROS',
  entidad: ' Bancolombia ',
  numero: '4011 2233-4401',
  titular: ' Mar y Selva S.A.S. ',
};

const NEQUI: ValoresCuenta = { tipo: 'NEQUI', entidad: 'Bancolombia', numero: '300 111 0202', titular: 'Laura' };

describe('los tipos de cuenta', () => {
  it('solo Nequi y Daviplata son billeteras', () => {
    expect(esBilletera('NEQUI')).toBe(true);
    expect(esBilletera('DAVIPLATA')).toBe(true);
    expect(esBilletera('AHORROS')).toBe(false);
    expect(esBilletera('')).toBe(false);
  });

  it('solo las cuentas bancarias piden el banco', () => {
    expect(pideEntidad('AHORROS')).toBe(true);
    expect(pideEntidad('CORRIENTE')).toBe(true);
    expect(pideEntidad('NEQUI')).toBe(false);
    expect(pideEntidad('')).toBe(false);
  });
});

describe('el número', () => {
  it('se limpia de espacios y guiones', () => {
    expect(normalizarNumero(' 4011 2233-4401 ')).toBe('401122334401');
  });
});

describe('la validación', () => {
  it('sin nada pide el tipo, el número y el titular', () => {
    expect(validarCuenta(VALORES_CUENTA_VACIOS)).toEqual({
      tipo: 'tipoRequerido',
      numero: 'numeroRequerido',
      titular: 'titularRequerido',
    });
  });

  it('una cuenta bancaria completa es válida', () => {
    expect(validarCuenta(BANCO)).toEqual({});
  });

  it('una cuenta bancaria pide el banco', () => {
    expect(validarCuenta({ ...BANCO, entidad: '  ' })).toEqual({ entidad: 'entidadRequerida' });
    expect(validarCuenta({ ...BANCO, tipo: 'CORRIENTE', entidad: '' })).toEqual({ entidad: 'entidadRequerida' });
    expect(validarCuenta({ ...BANCO, entidad: 'B'.repeat(101) })).toEqual({ entidad: 'entidadLarga' });
  });

  it('una billetera no pide el banco', () => {
    expect(validarCuenta({ ...NEQUI, entidad: '' })).toEqual({});
  });

  describe('el número de una cuenta bancaria', () => {
    const con = (numero: string) => validarCuenta({ ...BANCO, numero }).numero;

    it('tiene de 6 a 20 dígitos', () => {
      expect(con('123456')).toBeUndefined();
      expect(con('12345678901234567890')).toBeUndefined();
      expect(con('12345')).toBe('numeroBancoInvalido');
      expect(con('123456789012345678901')).toBe('numeroBancoInvalido');
    });

    it('no admite letras', () => {
      expect(con('40112233AB')).toBe('numeroBancoInvalido');
    });
  });

  describe('el número de una billetera', () => {
    const con = (numero: string, tipo: 'NEQUI' | 'DAVIPLATA' = 'NEQUI') => validarCuenta({ ...NEQUI, tipo, numero }).numero;

    it('es un celular de 10 dígitos que empieza por 3', () => {
      expect(con('3001110202')).toBeUndefined();
      expect(con('300 111 0202', 'DAVIPLATA')).toBeUndefined();
      expect(con('2001110202')).toBe('numeroCelularInvalido');
      expect(con('300111020')).toBe('numeroCelularInvalido');
      expect(con('30011102022')).toBe('numeroCelularInvalido');
    });
  });

  it('el titular es obligatorio y llega a 100 caracteres', () => {
    expect(validarCuenta({ ...BANCO, titular: ' ' })).toEqual({ titular: 'titularRequerido' });
    expect(validarCuenta({ ...BANCO, titular: 'T'.repeat(100) })).toEqual({});
    expect(validarCuenta({ ...BANCO, titular: 'T'.repeat(101) })).toEqual({ titular: 'titularLargo' });
  });

  it('ningún motivo lleva el número escrito', () => {
    const errores = validarCuenta({ ...BANCO, numero: '1234567890123456789012' });

    expect(JSON.stringify(errores)).not.toContain('1234567890');
  });
});

describe('la petición', () => {
  it('una cuenta bancaria manda el banco, el número limpio y el titular sin espacios sobrantes', () => {
    expect(construirCuenta(BANCO)).toEqual({
      tipo: 'AHORROS',
      entidad: 'Bancolombia',
      numero: '401122334401',
      titular: 'Mar y Selva S.A.S.',
    });
  });

  it('una billetera no manda banco aunque se haya escrito antes de cambiar de tipo', () => {
    const cuerpo = construirCuenta(NEQUI);

    expect(cuerpo).toEqual({ tipo: 'NEQUI', numero: '3001110202', titular: 'Laura' });
    expect('entidad' in cuerpo).toBe(false);
  });
});

describe('lo que se muestra de la cuenta vigente', () => {
  const vigente: CuentaCobro = {
    tipo: 'AHORROS',
    entidad: 'Bancolombia',
    numeroEnmascarado: '******0101',
    titular: 'Mar y Selva S.A.S.',
    vigenteDesde: '2026-10-29T15:00:00Z',
  };

  it('el formulario nace con todo menos el número', () => {
    expect(valoresDeCuenta(vigente)).toEqual({
      tipo: 'AHORROS',
      entidad: 'Bancolombia',
      numero: '',
      titular: 'Mar y Selva S.A.S.',
    });
  });

  it('en una billetera no pone banco', () => {
    expect(valoresDeCuenta({ ...vigente, tipo: 'NEQUI', entidad: 'Nequi' }).entidad).toBe('');
  });

  it('los últimos dígitos salen del número enmascarado', () => {
    expect(ultimosDigitos('******0101')).toBe('0101');
    expect(ultimosDigitos('****')).toBe('');
  });
});
