import { CambioComision, CuentaInterna } from '../../core/api/modelos-nucleo';
import {
  PRECIO_DE_EJEMPLO,
  ejemploDeComision,
  nombreDelAdministrador,
  ordenarHistorial,
  porcentajeParaEnviar,
  validarPorcentaje,
} from './comision';

describe('validarPorcentaje', () => {
  it('un porcentaje distinto del vigente y dentro del rango se puede enviar', () => {
    expect(validarPorcentaje(5.5, 4)).toBeNull();
    expect(validarPorcentaje(0, 4)).toBeNull();
    expect(validarPorcentaje(99.99, 4)).toBeNull();
    expect(validarPorcentaje(12, 4)).toBeNull();
  });

  it.each([null, undefined, Number.NaN])('%s es obligatorio', (valor) => {
    expect(validarPorcentaje(valor, 4)).toBe('requerido');
  });

  it('el rango es de 0 a menos de 100: el 100 no vale', () => {
    expect(validarPorcentaje(-0.01, 4)).toBe('rango');
    expect(validarPorcentaje(100, 4)).toBe('rango');
    expect(validarPorcentaje(150, 4)).toBe('rango');
    expect(validarPorcentaje(Number.POSITIVE_INFINITY, 4)).toBe('rango');
  });

  it('admite hasta dos decimales', () => {
    expect(validarPorcentaje(5.55, 4)).toBeNull();
    expect(validarPorcentaje(5.5, 4)).toBeNull();
    expect(validarPorcentaje(5.555, 4)).toBe('decimales');
    expect(validarPorcentaje(0.001, 4)).toBe('decimales');
  });

  it('los restos de la coma flotante no cuentan como decimales de más', () => {
    // 0.07 * 100 = 7.000000000000001 en coma flotante.
    expect(validarPorcentaje(0.07, 4)).toBeNull();
    expect(validarPorcentaje(1.15, 4)).toBeNull();
  });

  it('un porcentaje igual al vigente no cambiaría nada', () => {
    expect(validarPorcentaje(4, 4)).toBe('igual');
    expect(validarPorcentaje(4.0, 4.0)).toBe('igual');
    expect(validarPorcentaje(5.5, 5.5)).toBe('igual');
  });

  it('si no se conoce la vigente no compara', () => {
    expect(validarPorcentaje(4, null)).toBeNull();
    expect(validarPorcentaje(4, undefined)).toBeNull();
  });
});

describe('porcentajeParaEnviar', () => {
  it('deja exactamente las centésimas escritas', () => {
    expect(porcentajeParaEnviar(5.5)).toBe(5.5);
    expect(porcentajeParaEnviar(0.07)).toBe(0.07);
    expect(porcentajeParaEnviar(1.15)).toBe(1.15);
    expect(porcentajeParaEnviar(4)).toBe(4);
  });
});

describe('ejemploDeComision', () => {
  it('descuenta la comisión al prestador sobre el precio base, sin sumarla al turista', () => {
    expect(ejemploDeComision(4)).toEqual({ precioBase: 100_000, comision: 4_000, prestador: 96_000 });
    expect(ejemploDeComision(5.5)).toEqual({ precioBase: 100_000, comision: 5_500, prestador: 94_500 });
  });

  it('con 0 % el prestador recibe todo', () => {
    expect(ejemploDeComision(0)).toEqual({ precioBase: 100_000, comision: 0, prestador: 100_000 });
  });

  it('usa el precio que se le dé y redondea al peso', () => {
    expect(ejemploDeComision(4, 300_000)).toEqual({ precioBase: 300_000, comision: 12_000, prestador: 288_000 });
    expect(ejemploDeComision(3.33, 1_000)?.comision).toBe(33);
  });

  it('el precio de ejemplo es de 100.000 pesos', () => {
    expect(PRECIO_DE_EJEMPLO).toBe(100_000);
  });

  it.each([null, undefined, -1, 100, 120, Number.NaN])('con %s no hay ejemplo', (valor) => {
    expect(ejemploDeComision(valor)).toBeNull();
  });
});

describe('ordenarHistorial', () => {
  const cambio = (porcentaje: number, vigenteDesde: string): CambioComision => ({ porcentaje, vigenteDesde });

  it('pone primero el cambio más reciente', () => {
    const orden = ordenarHistorial([
      cambio(4, '2026-01-01T00:00:00Z'),
      cambio(6, '2026-10-27T15:00:00Z'),
      cambio(5, '2026-05-01T00:00:00Z'),
    ]);

    expect(orden.map((c) => c.porcentaje)).toEqual([6, 5, 4]);
  });

  it('no modifica la lista original', () => {
    const lista = [cambio(4, '2026-01-01T00:00:00Z'), cambio(6, '2026-10-27T15:00:00Z')];
    const copia = [...lista];

    ordenarHistorial(lista);

    expect(lista).toEqual(copia);
  });
});

describe('nombreDelAdministrador', () => {
  const cuentas = [
    { id: 'a1', nombre: 'Marta Ríos', rol: 'ADMINISTRADOR' },
    { id: 'o1', nombre: 'Operador', rol: 'OPERADOR_DISPONIBILIDAD' },
  ] as CuentaInterna[];

  it('da el nombre de la cuenta que hizo el cambio', () => {
    expect(nombreDelAdministrador('a1', cuentas)).toBe('Marta Ríos');
  });

  it('el cambio inicial no trae quién lo hizo', () => {
    expect(nombreDelAdministrador(undefined, cuentas)).toBeNull();
    expect(nombreDelAdministrador('', cuentas)).toBeNull();
  });

  it('una cuenta que no se conoce no tiene nombre', () => {
    expect(nombreDelAdministrador('otro', cuentas)).toBeNull();
    expect(nombreDelAdministrador('a1', [])).toBeNull();
  });
});
