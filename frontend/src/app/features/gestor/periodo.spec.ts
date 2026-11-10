import {
  ATAJOS,
  MAXIMO_DIAS,
  atajoActivo,
  diasDelPeriodo,
  esFecha,
  hoyEnColombia,
  parametrosDe,
  periodoDeAtajo,
  periodoDeParametros,
  periodoPorDefecto,
  validarPeriodo,
} from './periodo';

const HOY = '2026-11-10';

describe('esFecha', () => {
  it('acepta una fecha que existe', () => {
    expect(esFecha('2026-11-10')).toBe(true);
    expect(esFecha('2028-02-29')).toBe(true);
  });

  it.each(['', null, undefined, '2026-11-1', '10/11/2026', '2026-13-01', '2026-02-30', '2027-02-29', 'hoy'])(
    '"%s" no es una fecha',
    (valor) => expect(esFecha(valor)).toBe(false),
  );
});

describe('hoyEnColombia', () => {
  it('cuenta el día en la hora de Colombia, no en UTC', () => {
    // 03:00 UTC del 10 de noviembre son las 22:00 del 9 en Colombia (UTC-5).
    expect(hoyEnColombia(Date.parse('2026-11-10T03:00:00Z'))).toBe('2026-11-09');
    expect(hoyEnColombia(Date.parse('2026-11-10T05:00:00Z'))).toBe('2026-11-10');
    expect(hoyEnColombia(Date.parse('2026-11-10T23:30:00Z'))).toBe('2026-11-10');
  });
});

describe('diasDelPeriodo', () => {
  it('cuenta el primer y el último día', () => {
    expect(diasDelPeriodo({ desde: '2026-11-10', hasta: '2026-11-10' })).toBe(1);
    expect(diasDelPeriodo({ desde: '2026-11-01', hasta: '2026-11-30' })).toBe(30);
    expect(diasDelPeriodo({ desde: '2026-01-01', hasta: '2026-12-31' })).toBe(365);
  });

  it('cruza meses, años y años bisiestos', () => {
    expect(diasDelPeriodo({ desde: '2026-12-25', hasta: '2027-01-05' })).toBe(12);
    expect(diasDelPeriodo({ desde: '2028-01-01', hasta: '2028-12-31' })).toBe(366);
  });
});

describe('los atajos', () => {
  it('los últimos N días terminan hoy y cuentan hoy', () => {
    expect(periodoDeAtajo('ultimos7', HOY)).toEqual({ desde: '2026-11-04', hasta: HOY });
    expect(periodoDeAtajo('ultimos30', HOY)).toEqual({ desde: '2026-10-12', hasta: HOY });
    expect(periodoDeAtajo('ultimos90', HOY)).toEqual({ desde: '2026-08-13', hasta: HOY });
    expect(diasDelPeriodo(periodoDeAtajo('ultimos7', HOY))).toBe(7);
    expect(diasDelPeriodo(periodoDeAtajo('ultimos30', HOY))).toBe(30);
    expect(diasDelPeriodo(periodoDeAtajo('ultimos90', HOY))).toBe(90);
  });

  it('este mes va del día 1 a hoy', () => {
    expect(periodoDeAtajo('esteMes', HOY)).toEqual({ desde: '2026-11-01', hasta: HOY });
    expect(periodoDeAtajo('esteMes', '2026-11-01')).toEqual({ desde: '2026-11-01', hasta: '2026-11-01' });
  });

  it('el mes anterior es el mes completo anterior', () => {
    expect(periodoDeAtajo('mesAnterior', HOY)).toEqual({ desde: '2026-10-01', hasta: '2026-10-31' });
  });

  it('el mes anterior de enero es diciembre del año pasado', () => {
    expect(periodoDeAtajo('mesAnterior', '2027-01-15')).toEqual({ desde: '2026-12-01', hasta: '2026-12-31' });
  });

  it('el mes anterior de marzo termina el 28 o el 29 de febrero según el año', () => {
    expect(periodoDeAtajo('mesAnterior', '2027-03-05')).toEqual({ desde: '2027-02-01', hasta: '2027-02-28' });
    expect(periodoDeAtajo('mesAnterior', '2028-03-05')).toEqual({ desde: '2028-02-01', hasta: '2028-02-29' });
  });

  it('todos son válidos', () => {
    for (const atajo of ATAJOS) {
      const periodo = periodoDeAtajo(atajo, HOY);
      expect(validarPeriodo(periodo.desde, periodo.hasta, HOY)).toBeNull();
    }
  });

  it('por defecto son los últimos 30 días, como el núcleo', () => {
    expect(periodoPorDefecto(HOY)).toEqual(periodoDeAtajo('ultimos30', HOY));
  });

  it('se reconoce cuál es el atajo activo y ninguno si se escribió a mano', () => {
    expect(atajoActivo(periodoDeAtajo('ultimos90', HOY), HOY)).toBe('ultimos90');
    expect(atajoActivo(periodoDeAtajo('mesAnterior', HOY), HOY)).toBe('mesAnterior');
    expect(atajoActivo({ desde: '2026-10-05', hasta: '2026-10-20' }, HOY)).toBeNull();
  });
});

describe('validarPeriodo', () => {
  it('un periodo bien escrito sirve', () => {
    expect(validarPeriodo('2026-10-01', '2026-10-31', HOY)).toBeNull();
    expect(validarPeriodo(HOY, HOY, HOY)).toBeNull();
  });

  it('pide las dos fechas', () => {
    expect(validarPeriodo('', '2026-10-31', HOY)).toBe('desdeRequerida');
    expect(validarPeriodo('2026-10-01', '', HOY)).toBe('hastaRequerida');
    expect(validarPeriodo('2026-02-30', '2026-10-31', HOY)).toBe('desdeRequerida');
  });

  it('el inicio no puede ser posterior al final', () => {
    expect(validarPeriodo('2026-10-31', '2026-10-01', HOY)).toBe('desdeMayor');
  });

  it('el final no puede ser posterior a hoy', () => {
    expect(validarPeriodo('2026-11-01', '2026-11-11', HOY)).toBe('hastaFutura');
  });

  it('como máximo 366 días', () => {
    // Del 11 de noviembre de 2025 al 10 de noviembre de 2026 hay 365 días.
    expect(validarPeriodo('2025-11-11', HOY, HOY)).toBeNull();
    // Un día más sigue sirviendo mientras no pase de 366.
    expect(validarPeriodo('2025-11-10', HOY, HOY)).toBeNull();
    expect(diasDelPeriodo({ desde: '2025-11-10', hasta: HOY })).toBe(366);
    expect(validarPeriodo('2025-11-09', HOY, HOY)).toBe('demasiadoLargo');
    expect(MAXIMO_DIAS).toBe(366);
  });
});

describe('periodoDeParametros', () => {
  it('toma el periodo de la dirección si es válido', () => {
    expect(periodoDeParametros('2026-10-01', '2026-10-31', HOY)).toEqual({ desde: '2026-10-01', hasta: '2026-10-31' });
  });

  it('sin parámetros usa los últimos 30 días', () => {
    expect(periodoDeParametros(null, null, HOY)).toEqual(periodoPorDefecto(HOY));
  });

  it.each([
    ['solo desde', '2026-10-01', null],
    ['solo hasta', null, '2026-10-31'],
    ['una fecha mal escrita', '2026-1-01', '2026-10-31'],
    ['invertido', '2026-10-31', '2026-10-01'],
    ['en el futuro', '2026-11-01', '2026-12-31'],
    ['de más de un año', '2024-01-01', '2026-10-31'],
  ])('una dirección con %s no rompe la pantalla: usa el periodo por defecto', (_caso, desde, hasta) => {
    expect(periodoDeParametros(desde, hasta, HOY)).toEqual(periodoPorDefecto(HOY));
  });

  it('los parámetros de un periodo son sus dos fechas', () => {
    expect(parametrosDe({ desde: '2026-10-01', hasta: '2026-10-31' })).toEqual({ desde: '2026-10-01', hasta: '2026-10-31' });
  });
});
