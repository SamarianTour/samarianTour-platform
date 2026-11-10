import { CuentaInterna } from '../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../core/idioma/idioma';
import {
  CLASE_DE_ESTADO_CUENTA,
  CORREO_MAXIMO,
  ETIQUETA_ESTADO_CUENTA,
  FILTROS_CUENTAS,
  NOMBRE_MAXIMO,
  ROLES_CREABLES,
  accionDeCuenta,
  construirCuenta,
  contarPorRol,
  filtrarCuentas,
  filtroDeCuentas,
  validarCuenta,
} from './cuentas';

function cuenta(id: string, rol: CuentaInterna['rol'], estado: CuentaInterna['estado'] = 'ACTIVA'): CuentaInterna {
  return {
    id,
    rol,
    nombre: `Cuenta ${id}`,
    correo: `${id}@samariantour.co`,
    estado,
    contrasenaEstablecida: true,
    registradaEn: '2026-01-01T00:00:00Z',
  };
}

const VALIDA = { rol: 'OPERADOR_DISPONIBILIDAD', nombre: 'Valentina Ríos', correo: 'valentina@samariantour.co' };

describe('roles que se pueden crear', () => {
  it('son el operador de disponibilidad y el gestor del destino, no el administrador', () => {
    expect(ROLES_CREABLES).toEqual(['OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO']);
  });
});

describe('estado de una cuenta', () => {
  beforeEach(() => establecerIdioma('es', { recordar: false }));

  it('tiene nombre y color, con texto', () => {
    expect(ETIQUETA_ESTADO_CUENTA.ACTIVA).toBe('Activa');
    expect(ETIQUETA_ESTADO_CUENTA.DESACTIVADA).toBe('Desactivada');
    expect(CLASE_DE_ESTADO_CUENTA.ACTIVA).toMatch(/^etiqueta--/);
    expect(CLASE_DE_ESTADO_CUENTA.DESACTIVADA).toMatch(/^etiqueta--/);
  });

  it('sigue el idioma', () => {
    establecerIdioma('en', { recordar: false });

    expect(ETIQUETA_ESTADO_CUENTA.DESACTIVADA).toBe('Deactivated');
  });
});

describe('validarCuenta', () => {
  it('una cuenta completa no tiene errores', () => {
    expect(validarCuenta(VALIDA)).toEqual({});
    expect(validarCuenta({ ...VALIDA, rol: 'GESTOR_DESTINO' })).toEqual({});
  });

  it('el formulario vacío exige el rol, el nombre y el correo', () => {
    expect(validarCuenta({ rol: '', nombre: '', correo: '' })).toEqual({
      rol: 'rolRequerido',
      nombre: 'nombreRequerido',
      correo: 'correoRequerido',
    });
  });

  it('no se puede crear un administrador desde el panel', () => {
    expect(validarCuenta({ ...VALIDA, rol: 'ADMINISTRADOR' }).rol).toBe('rolRequerido');
  });

  it('un nombre o un correo de solo espacios cuenta como vacío', () => {
    expect(validarCuenta({ ...VALIDA, nombre: '   ' }).nombre).toBe('nombreRequerido');
    expect(validarCuenta({ ...VALIDA, correo: '  ' }).correo).toBe('correoRequerido');
  });

  it('el nombre admite hasta 100 caracteres', () => {
    expect(NOMBRE_MAXIMO).toBe(100);
    expect(validarCuenta({ ...VALIDA, nombre: 'a'.repeat(100) })).toEqual({});
    expect(validarCuenta({ ...VALIDA, nombre: 'a'.repeat(101) }).nombre).toBe('nombreLargo');
  });

  it('el correo admite hasta 254 caracteres', () => {
    expect(CORREO_MAXIMO).toBe(254);
    const largo = `${'a'.repeat(250)}@b.co`;
    expect(validarCuenta({ ...VALIDA, correo: largo }).correo).toBe('correoLargo');
  });

  it.each(['sin-arroba', 'dos@@arrobas.co', 'a@b', 'a b@c.co', '@dominio.co', 'nombre@'])(
    'el correo %j no tiene forma de correo',
    (correo) => {
      expect(validarCuenta({ ...VALIDA, correo }).correo).toBe('correoFormato');
    },
  );

  it.each(['a@b.co', 'nombre.apellido@samariantour.co', 'x+y@dominio.com.co'])('el correo %j es válido', (correo) => {
    expect(validarCuenta({ ...VALIDA, correo })).toEqual({});
  });
});

describe('construirCuenta', () => {
  it('manda el rol, el nombre y el correo sin espacios en los extremos', () => {
    expect(
      construirCuenta({ rol: 'GESTOR_DESTINO', nombre: '  Valentina Ríos ', correo: ' valentina@samariantour.co  ' }),
    ).toEqual({ rol: 'GESTOR_DESTINO', nombre: 'Valentina Ríos', correo: 'valentina@samariantour.co' });
  });
});

describe('accionDeCuenta', () => {
  it('una cuenta activa de operador o gestor se puede desactivar', () => {
    expect(accionDeCuenta(cuenta('1', 'OPERADOR_DISPONIBILIDAD'))).toBe('desactivar');
    expect(accionDeCuenta(cuenta('2', 'GESTOR_DESTINO'))).toBe('desactivar');
  });

  it('una desactivada se puede reactivar', () => {
    expect(accionDeCuenta(cuenta('1', 'OPERADOR_DISPONIBILIDAD', 'DESACTIVADA'))).toBe('reactivar');
    expect(accionDeCuenta(cuenta('2', 'GESTOR_DESTINO', 'DESACTIVADA'))).toBe('reactivar');
  });

  it('las cuentas de administrador solo se consultan', () => {
    expect(accionDeCuenta(cuenta('1', 'ADMINISTRADOR'))).toBeNull();
    expect(accionDeCuenta(cuenta('2', 'ADMINISTRADOR', 'DESACTIVADA'))).toBeNull();
  });
});

describe('filtrar las cuentas', () => {
  const cuentas = [
    cuenta('a', 'ADMINISTRADOR'),
    cuenta('o1', 'OPERADOR_DISPONIBILIDAD'),
    cuenta('g', 'GESTOR_DESTINO'),
    cuenta('o2', 'OPERADOR_DISPONIBILIDAD', 'DESACTIVADA'),
  ];

  it('hay un filtro por cada rol, además de "todas"', () => {
    expect(FILTROS_CUENTAS).toEqual(['todas', 'OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO', 'ADMINISTRADOR']);
  });

  it('el filtro de la dirección acepta un rol y, si no, muestra todas', () => {
    expect(filtroDeCuentas('GESTOR_DESTINO')).toBe('GESTOR_DESTINO');
    expect(filtroDeCuentas('ADMINISTRADOR')).toBe('ADMINISTRADOR');
    for (const valor of [null, '', 'todas', 'TURISTA', 'PRESTADOR', 'x']) {
      expect(filtroDeCuentas(valor)).toBe('todas');
    }
  });

  it('"todas" muestra todas, activas y desactivadas, en el mismo orden', () => {
    expect(filtrarCuentas(cuentas, 'todas').map((c) => c.id)).toEqual(['a', 'o1', 'g', 'o2']);
  });

  it('un rol muestra solo las cuentas de ese rol', () => {
    expect(filtrarCuentas(cuentas, 'OPERADOR_DISPONIBILIDAD').map((c) => c.id)).toEqual(['o1', 'o2']);
    expect(filtrarCuentas(cuentas, 'GESTOR_DESTINO').map((c) => c.id)).toEqual(['g']);
  });

  it('no modifica la lista original', () => {
    const copia = [...cuentas];

    filtrarCuentas(cuentas, 'todas');

    expect(cuentas).toEqual(copia);
  });

  it('cuenta las cuentas de un rol', () => {
    expect(contarPorRol(cuentas, 'OPERADOR_DISPONIBILIDAD')).toBe(2);
    expect(contarPorRol(cuentas, 'ADMINISTRADOR')).toBe(1);
    expect(contarPorRol([], 'GESTOR_DESTINO')).toBe(0);
  });
});
