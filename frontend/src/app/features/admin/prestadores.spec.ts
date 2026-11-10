import { CuentaInterna, PrestadorAdmin, SincronizacionRnt } from '../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../core/idioma/idioma';
import {
  CLASE_DE_ESTADO_RNT,
  ETIQUETA_ESTADO_RNT,
  contarSinOperador,
  datosDelOperador,
  etiquetaDeOperador,
  filtroDePrestadores,
  operadoresAsignables,
  puedeAsignar,
  resumenDeSincronizacion,
} from './prestadores';

function cuenta(id: string, nombre: string, extra: Partial<CuentaInterna> = {}): CuentaInterna {
  return {
    id,
    rol: 'OPERADOR_DISPONIBILIDAD',
    nombre,
    correo: `${nombre.toLowerCase().replace(/\s/g, '.')}@samariantour.co`,
    estado: 'ACTIVA',
    contrasenaEstablecida: true,
    registradaEn: '2026-01-01T00:00:00Z',
    ...extra,
  };
}

function prestador(id: string, operadorId?: string): PrestadorAdmin {
  return {
    id,
    numeroRnt: '910005',
    razonSocial: `Prestador ${id}`,
    tipo: 'AGENCIA_OPERADORA',
    estadoRnt: 'ACTIVO',
    operadorId,
    registradoEn: '2026-10-30T14:00:00Z',
  };
}

const sincronizacion = (extra: Partial<SincronizacionRnt> = {}): SincronizacionRnt => ({
  revisados: 5,
  retirados: 0,
  restituidos: 0,
  sinRespuesta: 0,
  interrumpida: false,
  ...extra,
});

describe('estado del prestador frente al RNT', () => {
  beforeEach(() => establecerIdioma('es', { recordar: false }));

  it('cada uno de los cuatro estados tiene su nombre y su color', () => {
    const estados = ['ACTIVO', 'INACTIVO', 'NO_ENCONTRADO', 'VERIFICACION_PENDIENTE'] as const;

    expect(estados.map((e) => ETIQUETA_ESTADO_RNT[e])).toEqual([
      'Activo',
      'Inactivo',
      'No encontrado',
      'Verificación pendiente',
    ]);
    for (const estado of estados) {
      expect(CLASE_DE_ESTADO_RNT[estado]).toMatch(/^etiqueta--/);
    }
  });

  it('los nombres siguen el idioma', () => {
    establecerIdioma('en', { recordar: false });

    expect(ETIQUETA_ESTADO_RNT.INACTIVO).toBe('Inactive');
    expect(ETIQUETA_ESTADO_RNT.VERIFICACION_PENDIENTE).toBe('Verification pending');
  });
});

describe('filtroDePrestadores', () => {
  it('?sinOperador=true muestra solo los que faltan por asignar', () => {
    expect(filtroDePrestadores('true')).toBe('sin-operador');
  });

  it.each([null, '', 'false', 'TRUE', '1', 'x'])('con %j muestra todos', (valor) => {
    expect(filtroDePrestadores(valor)).toBe('todos');
  });
});

describe('operadoresAsignables', () => {
  it('son solo los operadores de disponibilidad activos, por nombre', () => {
    const cuentas = [
      cuenta('c', 'Carlos'),
      cuenta('a', 'Ana'),
      cuenta('d', 'Diana', { estado: 'DESACTIVADA' }),
      cuenta('g', 'Gestor Gómez', { rol: 'GESTOR_DESTINO' }),
      cuenta('x', 'Admin', { rol: 'ADMINISTRADOR' }),
      cuenta('b', 'Beatriz'),
    ];

    expect(operadoresAsignables(cuentas).map((c) => c.nombre)).toEqual(['Ana', 'Beatriz', 'Carlos']);
  });

  it('ordena sin distinguir mayúsculas ni tildes', () => {
    const cuentas = [cuenta('1', 'zoe'), cuenta('2', 'Álvaro'), cuenta('3', 'Beto')];

    expect(operadoresAsignables(cuentas).map((c) => c.nombre)).toEqual(['Álvaro', 'Beto', 'zoe']);
  });

  it('no modifica la lista original', () => {
    const cuentas = [cuenta('b', 'Beatriz'), cuenta('a', 'Ana')];
    const copia = [...cuentas];

    operadoresAsignables(cuentas);

    expect(cuentas).toEqual(copia);
  });

  it('sin operadores activos no hay a quién asignar', () => {
    expect(operadoresAsignables([])).toEqual([]);
    expect(operadoresAsignables([cuenta('d', 'Diana', { estado: 'DESACTIVADA' })])).toEqual([]);
  });
});

describe('etiquetaDeOperador', () => {
  it('es el nombre del operador, no su identificador', () => {
    const ana = cuenta('uuid-ana', 'Ana Gómez');

    expect(etiquetaDeOperador(ana, [ana, cuenta('uuid-b', 'Beto')])).toBe('Ana Gómez');
  });

  it('si otro operador se llama igual, agrega el correo para distinguirlos', () => {
    const uno = cuenta('1', 'Ana Gómez', { correo: 'ana1@samariantour.co' });
    const dos = cuenta('2', 'ana gómez', { correo: 'ana2@samariantour.co' });

    expect(etiquetaDeOperador(uno, [uno, dos])).toBe('Ana Gómez (ana1@samariantour.co)');
    expect(etiquetaDeOperador(dos, [uno, dos])).toBe('ana gómez (ana2@samariantour.co)');
  });
});

describe('datosDelOperador', () => {
  const cuentas = [cuenta('a', 'Ana'), cuenta('d', 'Diana', { estado: 'DESACTIVADA' })];

  it('da el nombre del operador asignado', () => {
    expect(datosDelOperador('a', cuentas)).toEqual({ nombre: 'Ana', activo: true });
  });

  it('avisa si su cuenta está desactivada', () => {
    expect(datosDelOperador('d', cuentas)).toEqual({ nombre: 'Diana', activo: false });
  });

  it('sin operador asignado, o con uno que no se conoce, no hay datos', () => {
    expect(datosDelOperador(undefined, cuentas)).toBeNull();
    expect(datosDelOperador('', cuentas)).toBeNull();
    expect(datosDelOperador('otro', cuentas)).toBeNull();
  });
});

describe('puedeAsignar', () => {
  const asignables = [cuenta('a', 'Ana'), cuenta('b', 'Beto')];

  it('se puede asignar un operador asignable distinto del actual', () => {
    expect(puedeAsignar('a', prestador('p1'), asignables)).toBe(true);
    expect(puedeAsignar('b', prestador('p1', 'a'), asignables)).toBe(true);
  });

  it('no se envía lo que no cambia', () => {
    expect(puedeAsignar('a', prestador('p1', 'a'), asignables)).toBe(false);
  });

  it('sin elegir nada no se puede', () => {
    expect(puedeAsignar('', prestador('p1'), asignables)).toBe(false);
    expect(puedeAsignar(null, prestador('p1'), asignables)).toBe(false);
    expect(puedeAsignar(undefined, prestador('p1'), asignables)).toBe(false);
  });

  it('un operador que no es asignable (desactivado o de otro rol) no se acepta', () => {
    expect(puedeAsignar('d', prestador('p1'), asignables)).toBe(false);
  });
});

describe('contarSinOperador', () => {
  it('cuenta los prestadores que no tienen operador', () => {
    expect(contarSinOperador([prestador('1'), prestador('2', 'a'), prestador('3')])).toBe(2);
    expect(contarSinOperador([])).toBe(0);
  });
});

describe('resumenDeSincronizacion', () => {
  it('sin retirados ni restituidos no hubo cambios', () => {
    expect(resumenDeSincronizacion(sincronizacion())).toEqual({
      huboCambios: false,
      interrumpida: false,
      conservaron: false,
    });
  });

  it('retirar o restituir un prestador es un cambio en el catálogo', () => {
    expect(resumenDeSincronizacion(sincronizacion({ retirados: 1 })).huboCambios).toBe(true);
    expect(resumenDeSincronizacion(sincronizacion({ restituidos: 2 })).huboCambios).toBe(true);
  });

  it('si el RNT dejó de responder, la sincronización quedó interrumpida (QAS-03)', () => {
    expect(resumenDeSincronizacion(sincronizacion({ interrumpida: true })).interrumpida).toBe(true);
  });

  it('los prestadores sin respuesta conservan su último estado', () => {
    expect(resumenDeSincronizacion(sincronizacion({ sinRespuesta: 2 })).conservaron).toBe(true);
    expect(resumenDeSincronizacion(sincronizacion({ sinRespuesta: 0 })).conservaron).toBe(false);
  });
});
