import {
  VALORES_ATENCION_VACIOS,
  ValoresAtencion,
  construirAtencion,
  enlaceWhatsapp,
  notaMaxima,
  validarAtencion,
} from './atencion';

const CON_CUPO: ValoresAtencion = {
  resultado: 'ATENDIDA',
  precioTotal: 300000,
  politicaCancelacion: 'Sin reembolso a menos de 24 horas del servicio',
  aparteAcordado: true,
  nota: '',
};

describe('validarAtencion', () => {
  it('hay cupo con todo completo no tiene errores', () => {
    expect(validarAtencion(CON_CUPO)).toEqual({});
  });

  it('hay cupo con el formulario vacío exige precio, política y la confirmación del aparte', () => {
    expect(validarAtencion(VALORES_ATENCION_VACIOS)).toEqual({
      precioTotal: 'requerido',
      politicaCancelacion: 'requerido',
      aparteAcordado: 'sinAparte',
    });
  });

  it('el aparte acordado es obligatorio aunque lo demás esté bien (RF-38)', () => {
    expect(validarAtencion({ ...CON_CUPO, aparteAcordado: false })).toEqual({
      aparteAcordado: 'sinAparte',
    });
  });

  it.each([
    [0, 'minimo'],
    [-5, 'minimo'],
    [1500.5, 'entero'],
    [Number.NaN, 'requerido'],
    [null, 'requerido'],
  ] as const)('el precio %s da el error %s', (precioTotal, motivo) => {
    expect(validarAtencion({ ...CON_CUPO, precioTotal }).precioTotal).toBe(motivo);
  });

  it('un precio de 1 peso es válido: el contrato pide mínimo 1', () => {
    expect(validarAtencion({ ...CON_CUPO, precioTotal: 1 })).toEqual({});
  });

  it('una política de solo espacios cuenta como vacía', () => {
    expect(validarAtencion({ ...CON_CUPO, politicaCancelacion: '   ' }).politicaCancelacion).toBe(
      'requerido',
    );
  });

  it('la política admite hasta 1000 caracteres', () => {
    expect(
      validarAtencion({ ...CON_CUPO, politicaCancelacion: 'a'.repeat(1000) }),
    ).toEqual({});
    expect(
      validarAtencion({ ...CON_CUPO, politicaCancelacion: 'a'.repeat(1001) }).politicaCancelacion,
    ).toBe('largo');
  });

  it('la nota de las condiciones admite hasta 1000 caracteres', () => {
    expect(validarAtencion({ ...CON_CUPO, nota: 'a'.repeat(1000) })).toEqual({});
    expect(validarAtencion({ ...CON_CUPO, nota: 'a'.repeat(1001) }).nota).toBe('largo');
  });

  it('no hay cupo no exige precio ni política ni aparte, y la nota es opcional', () => {
    expect(validarAtencion({ ...VALORES_ATENCION_VACIOS, resultado: 'NO_DISPONIBLE' })).toEqual({});
  });

  it('el motivo de no hay cupo admite hasta 500 caracteres', () => {
    const base: ValoresAtencion = { ...VALORES_ATENCION_VACIOS, resultado: 'NO_DISPONIBLE' };

    expect(validarAtencion({ ...base, nota: 'a'.repeat(500) })).toEqual({});
    expect(validarAtencion({ ...base, nota: 'a'.repeat(501) })).toEqual({ nota: 'largo' });
  });
});

describe('notaMaxima', () => {
  it('es 1000 con hay cupo y 500 con no hay cupo', () => {
    expect(notaMaxima('ATENDIDA')).toBe(1000);
    expect(notaMaxima('NO_DISPONIBLE')).toBe(500);
  });
});

describe('construirAtencion', () => {
  it('hay cupo manda el precio total, la política, el aparte y la nota recortados', () => {
    expect(
      construirAtencion({
        ...CON_CUPO,
        politicaCancelacion: '  Sin reembolso  ',
        nota: ' Incluye el ingreso al parque ',
      }),
    ).toEqual({
      resultado: 'ATENDIDA',
      precioBase: 300000,
      politicaCancelacion: 'Sin reembolso',
      aparteAcordado: true,
      nota: 'Incluye el ingreso al parque',
    });
  });

  it('hay cupo sin nota no manda la nota', () => {
    expect(construirAtencion(CON_CUPO)).not.toHaveProperty('nota');
  });

  it('no hay cupo manda solo el resultado y el motivo, aunque haya condiciones escritas', () => {
    expect(construirAtencion({ ...CON_CUPO, resultado: 'NO_DISPONIBLE', nota: 'Sin cupo esa semana' })).toEqual(
      { resultado: 'NO_DISPONIBLE', nota: 'Sin cupo esa semana' },
    );
  });

  it('no hay cupo sin motivo manda solo el resultado', () => {
    expect(construirAtencion({ ...CON_CUPO, resultado: 'NO_DISPONIBLE' })).toEqual({
      resultado: 'NO_DISPONIBLE',
    });
  });
});

describe('enlaceWhatsapp', () => {
  it('arma el enlace con los dígitos del número, sin el signo +', () => {
    expect(enlaceWhatsapp('+573001110101')).toBe('https://wa.me/573001110101');
  });

  it('ignora espacios y guiones', () => {
    expect(enlaceWhatsapp('+57 300 111-0101')).toBe('https://wa.me/573001110101');
  });

  it.each([undefined, null, '', '   ', 'sin número', '123'])(
    'sin un número usable (%j) no hay enlace',
    (numero) => {
      expect(enlaceWhatsapp(numero)).toBeNull();
    },
  );
});
