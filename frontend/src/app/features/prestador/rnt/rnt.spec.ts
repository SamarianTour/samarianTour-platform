import { PrestadorPropio } from '../../../core/api/modelos-nucleo';
import {
  VALORES_TIPO_VACIOS,
  ValoresRegistro,
  construirRegistro,
  construirTipo,
  esGuia,
  idiomasDe,
  validarNumeroRnt,
  validarRegistro,
  validarTipo,
  valoresDeTipo,
} from './rnt';

const AGENCIA: ValoresRegistro = {
  numeroRnt: ' 910006 ',
  tipo: 'AGENCIA_OPERADORA',
  numeroEmpleados: 14,
  modalidades: ['KAYAK'],
  idiomas: 'ES, en',
};

const GUIA: ValoresRegistro = {
  numeroRnt: '910006',
  tipo: 'GUIA_INDEPENDIENTE',
  numeroEmpleados: 3,
  modalidades: ['CAMINATA', 'KAYAK'],
  idiomas: 'ES, en , es',
};

describe('el número de RNT', () => {
  it('es obligatorio', () => {
    expect(validarNumeroRnt('')).toBe('numeroRequerido');
    expect(validarNumeroRnt('   ')).toBe('numeroRequerido');
  });

  it('lleva solo dígitos, de 1 a 10', () => {
    expect(validarNumeroRnt('910006')).toBeNull();
    expect(validarNumeroRnt(' 910006 ')).toBeNull();
    expect(validarNumeroRnt('1234567890')).toBeNull();
    expect(validarNumeroRnt('12345678901')).toBe('numeroInvalido');
    expect(validarNumeroRnt('91-0006')).toBe('numeroInvalido');
    expect(validarNumeroRnt('RNT910')).toBe('numeroInvalido');
  });
});

describe('los idiomas', () => {
  it('se escriben en minúscula, sin repetir y sin vacíos', () => {
    expect(idiomasDe(' ES, en , es,, ')).toEqual(['es', 'en']);
    expect(idiomasDe('')).toEqual([]);
  });
});

describe('el tipo de prestador', () => {
  it('solo el guía independiente es guía', () => {
    expect(esGuia('GUIA_INDEPENDIENTE')).toBe(true);
    expect(esGuia('AGENCIA_OPERADORA')).toBe(false);
    expect(esGuia('')).toBe(false);
  });

  it('hay que elegirlo', () => {
    expect(validarTipo(VALORES_TIPO_VACIOS)).toEqual({ tipo: 'tipoRequerido' });
  });

  describe('los que no son guía piden empleados', () => {
    it.each(['AGENCIA_OPERADORA', 'ALOJAMIENTO', 'RESTAURANTE'] as const)('%s sin empleados', (tipo) => {
      expect(validarTipo({ ...VALORES_TIPO_VACIOS, tipo })).toEqual({ numeroEmpleados: 'empleadosRequerido' });
    });

    it('de 1 a 100000, enteros', () => {
      const con = (numeroEmpleados: number | null) =>
        validarTipo({ ...VALORES_TIPO_VACIOS, tipo: 'ALOJAMIENTO', numeroEmpleados }).numeroEmpleados;

      expect(con(1)).toBeUndefined();
      expect(con(100000)).toBeUndefined();
      expect(con(0)).toBe('empleadosRango');
      expect(con(100001)).toBe('empleadosRango');
      expect(con(2.5)).toBe('empleadosRango');
      expect(con(Number.NaN)).toBe('empleadosRequerido');
    });

    it('no piden modalidades ni idiomas', () => {
      expect(validarTipo({ ...VALORES_TIPO_VACIOS, tipo: 'RESTAURANTE', numeroEmpleados: 5 })).toEqual({});
    });
  });

  describe('el guía pide modalidades e idiomas y no empleados', () => {
    it('sin nada pide las dos', () => {
      expect(validarTipo({ ...VALORES_TIPO_VACIOS, tipo: 'GUIA_INDEPENDIENTE' })).toEqual({
        modalidades: 'modalidadesRequerida',
        idiomas: 'idiomasRequerido',
      });
    });

    it('cada idioma lleva un código de dos letras', () => {
      const con = (idiomas: string) =>
        validarTipo({ ...GUIA, idiomas }).idiomas;

      expect(con('es, en')).toBeUndefined();
      expect(con('español')).toBe('idiomasInvalidos');
      expect(con('es, eng')).toBe('idiomasInvalidos');
      expect(con('e1')).toBe('idiomasInvalidos');
    });

    it('no pide empleados', () => {
      expect(validarTipo({ ...GUIA, numeroEmpleados: null })).toEqual({});
    });
  });

  it('al registrar también se valida el número', () => {
    expect(validarRegistro({ ...AGENCIA, numeroRnt: '' })).toEqual({ numeroRnt: 'numeroRequerido' });
    expect(validarRegistro(AGENCIA)).toEqual({});
  });
});

describe('las peticiones', () => {
  it('una agencia manda sus empleados y nada del guía', () => {
    expect(construirTipo(AGENCIA)).toEqual({ tipo: 'AGENCIA_OPERADORA', numeroEmpleados: 14 });
  });

  it('un guía manda modalidades e idiomas y no empleados', () => {
    expect(construirTipo(GUIA)).toEqual({
      tipo: 'GUIA_INDEPENDIENTE',
      modalidades: ['CAMINATA', 'KAYAK'],
      idiomas: ['es', 'en'],
    });
    expect('numeroEmpleados' in construirTipo(GUIA)).toBe(false);
  });

  it('el registro suma el número sin espacios', () => {
    expect(construirRegistro(AGENCIA)).toEqual({
      numeroRnt: '910006',
      tipo: 'AGENCIA_OPERADORA',
      numeroEmpleados: 14,
    });
  });
});

describe('lo que el formulario muestra de un prestador', () => {
  function prestador(extra: Partial<PrestadorPropio>): PrestadorPropio {
    return {
      id: 'p1',
      numeroRnt: '910006',
      tipo: 'AGENCIA_OPERADORA',
      categoria: 'MICRO_PEQUENO',
      estadoRnt: 'ACTIVO',
      habilitado: true,
      ...extra,
    };
  }

  it('una agencia: su tipo y sus empleados', () => {
    expect(valoresDeTipo(prestador({ numeroEmpleados: 14 }))).toEqual({
      tipo: 'AGENCIA_OPERADORA',
      numeroEmpleados: 14,
      modalidades: [],
      idiomas: '',
    });
  });

  it('un guía: sus modalidades conocidas y sus idiomas en una línea', () => {
    expect(
      valoresDeTipo(prestador({ tipo: 'GUIA_INDEPENDIENTE', modalidades: ['CAMINATA', 'RAPEL'], idiomas: ['es', 'en'] })),
    ).toEqual({ tipo: 'GUIA_INDEPENDIENTE', numeroEmpleados: null, modalidades: ['CAMINATA'], idiomas: 'es, en' });
  });
});
