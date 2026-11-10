import { TipoOferta } from '../../../core/api/modelos';
import {
  ContextoSolicitud,
  VALORES_VACIOS,
  ValoresSolicitud,
  aPeticion,
  camposDe,
  esFechaValida,
  fechaEnColombia,
  fechaMinima,
  nochesEntre,
  normalizarHora,
  precioDeReferencia,
  sumarDias,
  validarSolicitud,
} from './peticion-solicitud';

/** 26 de octubre de 2026, 10:00 en Colombia (UTC-5). */
const AHORA = Date.parse('2026-10-26T15:00:00Z');

const contexto = (tipo: TipoOferta, resto: Partial<ContextoSolicitud> = {}): ContextoSolicitud => ({
  tipo,
  capacidadMaxima: 10,
  horariosSalida: ['07:00', '14:00'],
  ahora: AHORA,
  ...resto,
});

const valores = (cambios: Partial<ValoresSolicitud>): ValoresSolicitud => ({
  ...VALORES_VACIOS,
  ...cambios,
});

describe('fechas en la hora de Colombia', () => {
  it('a las 22:00 en Colombia ya es el día siguiente en UTC, pero sigue siendo hoy aquí', () => {
    // 2026-10-27T03:00Z son las 22:00 del 26 en Colombia.
    expect(fechaEnColombia(Date.parse('2026-10-27T03:00:00Z'))).toBe('2026-10-26');
    expect(fechaEnColombia(Date.parse('2026-10-27T05:00:00Z'))).toBe('2026-10-27');
  });

  it('la primera fecha posible es dos días después de hoy', () => {
    expect(fechaMinima(AHORA)).toBe('2026-10-28');
    expect(fechaMinima(Date.parse('2026-10-27T03:00:00Z'))).toBe('2026-10-28');
  });

  it('suma días cruzando el fin de mes y de año', () => {
    expect(sumarDias('2026-10-30', 3)).toBe('2026-11-02');
    expect(sumarDias('2026-12-31', 2)).toBe('2027-01-02');
    expect(sumarDias('2028-02-28', 1)).toBe('2028-02-29');
  });

  it('reconoce las fechas que existen', () => {
    expect(esFechaValida('2026-10-28')).toBe(true);
    expect(esFechaValida('2026-02-31')).toBe(false);
    expect(esFechaValida('28/10/2026')).toBe(false);
    expect(esFechaValida('')).toBe(false);
  });

  it('cuenta las noches de un alojamiento', () => {
    expect(nochesEntre('2026-10-28', '2026-10-30')).toBe(2);
    expect(nochesEntre('2026-10-30', '2026-11-02')).toBe(3);
    expect(nochesEntre('2026-10-30', '2026-10-28')).toBe(0);
    expect(nochesEntre('', '2026-10-28')).toBe(0);
  });

  it('quita los segundos de una hora que manda la API', () => {
    expect(normalizarHora('07:00:00')).toBe('07:00');
    expect(normalizarHora('07:00')).toBe('07:00');
  });
});

describe('campos de cada tipo de oferta (RF-30)', () => {
  it.each([
    ['ACTIVIDAD', ['fechaInicio', 'horaSalida', 'personas', 'nota']],
    ['PAQUETE', ['fechaInicio', 'horaSalida', 'personas', 'nota']],
    ['ALOJAMIENTO', ['fechaInicio', 'fechaSalida', 'numeroUnidades', 'personas', 'nota']],
    ['RESTAURANTE', ['fechaInicio', 'horaReserva', 'personas', 'nota']],
  ] as const)('%s', (tipo, esperados) => {
    expect(camposDe(tipo)).toEqual(esperados);
  });
});

describe('validarSolicitud', () => {
  const buena = valores({ fechaInicio: '2026-10-28', horaSalida: '07:00', personas: 2 });

  it('una actividad completa es válida', () => {
    expect(validarSolicitud(buena, contexto('ACTIVIDAD'))).toEqual({});
  });

  it('un formulario vacío marca lo que su tipo pide', () => {
    expect(validarSolicitud(VALORES_VACIOS, contexto('ACTIVIDAD'))).toEqual({
      fechaInicio: 'requerido',
      horaSalida: 'requerido',
      personas: 'requerido',
    });
    expect(validarSolicitud(VALORES_VACIOS, contexto('ALOJAMIENTO'))).toEqual({
      fechaInicio: 'requerido',
      fechaSalida: 'requerido',
      numeroUnidades: 'requerido',
      personas: 'requerido',
    });
    expect(validarSolicitud(VALORES_VACIOS, contexto('RESTAURANTE'))).toEqual({
      fechaInicio: 'requerido',
      horaReserva: 'requerido',
      personas: 'requerido',
    });
  });

  describe('fecha de inicio', () => {
    it('mañana y pasado mañana no alcanzan: se pide con dos días de anticipación', () => {
      expect(
        validarSolicitud({ ...buena, fechaInicio: '2026-10-27' }, contexto('ACTIVIDAD'))
          .fechaInicio,
      ).toBe('pasada');
      expect(
        validarSolicitud({ ...buena, fechaInicio: '2026-10-26' }, contexto('ACTIVIDAD'))
          .fechaInicio,
      ).toBe('pasada');
    });

    it('dos días después de hoy sí', () => {
      expect(
        validarSolicitud({ ...buena, fechaInicio: '2026-10-28' }, contexto('ACTIVIDAD'))
          .fechaInicio,
      ).toBe(undefined);
    });

    it('una fecha que no existe es de formato incorrecto', () => {
      expect(
        validarSolicitud({ ...buena, fechaInicio: '2026-02-31' }, contexto('ACTIVIDAD'))
          .fechaInicio,
      ).toBe('formato');
    });

    it('usa la hora de Colombia: a las 22:00 del 26 todavía es 26', () => {
      const tarde = contexto('ACTIVIDAD', { ahora: Date.parse('2026-10-27T03:00:00Z') });

      expect(
        validarSolicitud({ ...buena, fechaInicio: '2026-10-28' }, tarde).fechaInicio,
      ).toBeUndefined();
      expect(validarSolicitud({ ...buena, fechaInicio: '2026-10-27' }, tarde).fechaInicio).toBe(
        'pasada',
      );
    });
  });

  describe('hora de salida (actividad y paquete)', () => {
    it('debe ser una de las que ofrece la oferta', () => {
      expect(
        validarSolicitud({ ...buena, horaSalida: '09:30' }, contexto('ACTIVIDAD')).horaSalida,
      ).toBe('noOfrecida');
      expect(
        validarSolicitud({ ...buena, horaSalida: '14:00' }, contexto('PAQUETE')).horaSalida,
      ).toBeUndefined();
    });

    it('compara sin los segundos que pueda traer la API', () => {
      const con = contexto('ACTIVIDAD', { horariosSalida: ['07:00:00'] });

      expect(validarSolicitud(buena, con).horaSalida).toBeUndefined();
    });

    it('con un formato que no es hora, falla el formato', () => {
      expect(
        validarSolicitud({ ...buena, horaSalida: '7am' }, contexto('ACTIVIDAD')).horaSalida,
      ).toBe('formato');
    });

    it('si la oferta no trae la lista, basta con que sea una hora', () => {
      const sinLista = contexto('ACTIVIDAD', { horariosSalida: [] });

      expect(
        validarSolicitud({ ...buena, horaSalida: '09:30' }, sinLista).horaSalida,
      ).toBeUndefined();
    });
  });

  describe('restaurante', () => {
    const reserva = valores({ fechaInicio: '2026-10-28', horaReserva: '19:30', personas: 4 });

    it('pide la hora de la reserva, la que quiera', () => {
      expect(validarSolicitud(reserva, contexto('RESTAURANTE'))).toEqual({});
      expect(
        validarSolicitud({ ...reserva, horaReserva: '25:00' }, contexto('RESTAURANTE')).horaReserva,
      ).toBe('formato');
    });
  });

  describe('alojamiento', () => {
    const estadia = valores({
      fechaInicio: '2026-10-28',
      fechaSalida: '2026-10-30',
      numeroUnidades: 1,
      personas: 3,
    });

    it('con entrada, salida, unidades y personas es válido', () => {
      expect(validarSolicitud(estadia, contexto('ALOJAMIENTO'))).toEqual({});
    });

    it('la salida debe ser posterior a la entrada', () => {
      const igual = validarSolicitud(
        { ...estadia, fechaSalida: '2026-10-28' },
        contexto('ALOJAMIENTO'),
      );
      const antes = validarSolicitud(
        { ...estadia, fechaSalida: '2026-10-27' },
        contexto('ALOJAMIENTO'),
      );

      expect(igual.fechaSalida).toBe('antesDeInicio');
      expect(antes.fechaSalida).toBe('antesDeInicio');
    });

    it('las unidades son un entero positivo', () => {
      expect(
        validarSolicitud({ ...estadia, numeroUnidades: 0 }, contexto('ALOJAMIENTO')).numeroUnidades,
      ).toBe('minimo');
      expect(
        validarSolicitud({ ...estadia, numeroUnidades: 1.5 }, contexto('ALOJAMIENTO'))
          .numeroUnidades,
      ).toBe('entero');
    });
  });

  describe('personas', () => {
    it('de 1 hasta la capacidad máxima', () => {
      expect(
        validarSolicitud({ ...buena, personas: 1 }, contexto('ACTIVIDAD')).personas,
      ).toBeUndefined();
      expect(
        validarSolicitud({ ...buena, personas: 10 }, contexto('ACTIVIDAD')).personas,
      ).toBeUndefined();
      expect(validarSolicitud({ ...buena, personas: 11 }, contexto('ACTIVIDAD')).personas).toBe(
        'maximo',
      );
      expect(validarSolicitud({ ...buena, personas: 0 }, contexto('ACTIVIDAD')).personas).toBe(
        'minimo',
      );
      expect(validarSolicitud({ ...buena, personas: -2 }, contexto('ACTIVIDAD')).personas).toBe(
        'minimo',
      );
    });

    it('un número con decimales no es válido', () => {
      expect(validarSolicitud({ ...buena, personas: 2.5 }, contexto('ACTIVIDAD')).personas).toBe(
        'entero',
      );
    });

    it('respeta la capacidad de cada oferta', () => {
      expect(
        validarSolicitud({ ...buena, personas: 3 }, contexto('ACTIVIDAD', { capacidadMaxima: 2 }))
          .personas,
      ).toBe('maximo');
    });

    it('un campo vacío (NaN) cuenta como falta', () => {
      expect(
        validarSolicitud({ ...buena, personas: Number.NaN }, contexto('ACTIVIDAD')).personas,
      ).toBe('requerido');
    });
  });

  describe('nota', () => {
    it('hasta 500 caracteres sin contar los espacios de los extremos', () => {
      expect(
        validarSolicitud({ ...buena, nota: 'a'.repeat(500) }, contexto('ACTIVIDAD')).nota,
      ).toBeUndefined();
      expect(
        validarSolicitud({ ...buena, nota: `  ${'a'.repeat(500)}  ` }, contexto('ACTIVIDAD')).nota,
      ).toBe(undefined);
      expect(
        validarSolicitud({ ...buena, nota: 'a'.repeat(501) }, contexto('ACTIVIDAD')).nota,
      ).toBe('largo');
    });
  });

  it('un campo de otro tipo no se valida', () => {
    // La hora de reserva no importa en una actividad.
    expect(validarSolicitud({ ...buena, horaReserva: 'basura' }, contexto('ACTIVIDAD'))).toEqual(
      {},
    );
  });
});

describe('aPeticion', () => {
  it('actividad y paquete envían la hora de salida y nada de los otros tipos', () => {
    const v = valores({
      fechaInicio: '2026-10-28',
      horaSalida: '07:00',
      personas: 2,
      horaReserva: '19:30',
      fechaSalida: '2026-10-30',
      numeroUnidades: 3,
    });

    for (const tipo of ['ACTIVIDAD', 'PAQUETE'] as const) {
      expect(aPeticion(v, 'b1', tipo)).toEqual({
        ofertaId: 'b1',
        fechaInicio: '2026-10-28',
        horaSalida: '07:00',
        personas: 2,
      });
    }
  });

  it('alojamiento envía la fecha de salida y las unidades', () => {
    const v = valores({
      fechaInicio: '2026-10-28',
      fechaSalida: '2026-10-30',
      numeroUnidades: 1,
      personas: 3,
      horaSalida: '07:00',
    });

    expect(aPeticion(v, 'b2', 'ALOJAMIENTO')).toEqual({
      ofertaId: 'b2',
      fechaInicio: '2026-10-28',
      fechaSalida: '2026-10-30',
      numeroUnidades: 1,
      personas: 3,
    });
  });

  it('restaurante envía la hora de la reserva', () => {
    const v = valores({ fechaInicio: '2026-10-28', horaReserva: '19:30', personas: 4 });

    expect(aPeticion(v, 'b3', 'RESTAURANTE')).toEqual({
      ofertaId: 'b3',
      fechaInicio: '2026-10-28',
      horaReserva: '19:30',
      personas: 4,
    });
  });

  it('la nota viaja sin espacios sobrantes, y vacía no viaja', () => {
    const base = valores({ fechaInicio: '2026-10-28', horaSalida: '07:00', personas: 2 });

    expect(aPeticion({ ...base, nota: '  Vamos con una niña  ' }, 'b1', 'ACTIVIDAD').nota).toBe(
      'Vamos con una niña',
    );
    expect('nota' in aPeticion({ ...base, nota: '   ' }, 'b1', 'ACTIVIDAD')).toBe(false);
  });
});

describe('precioDeReferencia', () => {
  it('actividad, paquete y restaurante: precio por persona', () => {
    const v = { personas: 3, fechaInicio: '', fechaSalida: '', numeroUnidades: null };

    expect(precioDeReferencia('ACTIVIDAD', 70_000, v)).toBe(210_000);
    expect(precioDeReferencia('RESTAURANTE', 50_000, v)).toBe(150_000);
  });

  it('alojamiento: precio por noche y por unidad', () => {
    const v = {
      personas: 3,
      fechaInicio: '2026-10-28',
      fechaSalida: '2026-10-30',
      numeroUnidades: 2,
    };

    expect(precioDeReferencia('ALOJAMIENTO', 200_000, v)).toBe(800_000);
  });

  it('sin datos suficientes no hay precio', () => {
    expect(
      precioDeReferencia('ACTIVIDAD', 70_000, {
        personas: null,
        fechaInicio: '',
        fechaSalida: '',
        numeroUnidades: null,
      }),
    ).toBeNull();
    expect(
      precioDeReferencia('ALOJAMIENTO', 200_000, {
        personas: 2,
        fechaInicio: '2026-10-28',
        fechaSalida: '',
        numeroUnidades: 1,
      }),
    ).toBeNull();
  });
});
