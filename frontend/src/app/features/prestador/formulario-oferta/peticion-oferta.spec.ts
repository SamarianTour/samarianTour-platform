import { FichaOferta } from '../../../core/api/modelos';
import { ValoresOferta, aLista, aPeticion, valoresDeFicha } from './peticion-oferta';

const VACIO: ValoresOferta = {
  tipo: 'ACTIVIDAD',
  nombre: '',
  descripcion: '',
  precioReferencia: null,
  horarios: '',
  zonaId: '',
  comoLlegar: '',
  capacidadMaxima: null,
  accesibleSillaRuedas: false,
  tipoActividad: '',
  nivelEsfuerzo: '',
  horariosSalida: '',
  puntoEncuentro: '',
  duracionMinutos: null,
  modalidad: '',
  idiomasAtencion: '',
  actividadIds: [],
  tipoUnidad: '',
};

const COMUN: ValoresOferta = {
  ...VACIO,
  nombre: '  Caminata a Pozo Azul ',
  descripcion: 'Sendero entre cafetales.',
  precioReferencia: 80000,
  zonaId: 'a00000000000000000000007',
  capacidadMaxima: 10,
};

describe('aLista', () => {
  it('separa por comas y descarta espacios y vacíos', () => {
    expect(aLista(' 07:30, 14:00 ,, ')).toEqual(['07:30', '14:00']);
    expect(aLista('')).toEqual([]);
  });
});

describe('aPeticion', () => {
  it('arma una actividad con sus listas y sin los opcionales vacíos', () => {
    const peticion = aPeticion(
      {
        ...COMUN,
        tipoActividad: 'SENDERISMO',
        nivelEsfuerzo: 'MODERADO',
        horariosSalida: '07:30, 14:00',
        accesibleSillaRuedas: true,
        tipoUnidad: 'cabaña',
        actividadIds: ['b00000000000000000000001'],
      },
      null,
      ['3f2504e0-4f89-11d3-9a0c-0305e82c3301'],
    );

    expect(peticion).toEqual({
      tipo: 'ACTIVIDAD',
      nombre: 'Caminata a Pozo Azul',
      descripcion: 'Sendero entre cafetales.',
      precioReferencia: 80000,
      zonaId: 'a00000000000000000000007',
      capacidadMaxima: 10,
      fotos: ['3f2504e0-4f89-11d3-9a0c-0305e82c3301'],
      accesibleSillaRuedas: true,
      tipoActividad: 'SENDERISMO',
      nivelEsfuerzo: 'MODERADO',
      horariosSalida: ['07:30', '14:00'],
    });
  });

  it('incluye los datos del recorrido y la ubicación cuando tienen valor', () => {
    const peticion = aPeticion(
      {
        ...COMUN,
        tipoActividad: 'SENDERISMO',
        nivelEsfuerzo: 'BAJO',
        horariosSalida: '08:00',
        puntoEncuentro: 'Parque de Minca',
        duracionMinutos: 180,
        modalidad: 'CAMINATA',
        idiomasAtencion: 'es, en',
        comoLlegar: 'Bus desde el mercado',
      },
      { latitud: 11.14309, longitud: -74.11675 },
      [],
    );

    expect(peticion.puntoEncuentro).toBe('Parque de Minca');
    expect(peticion.duracionMinutos).toBe(180);
    expect(peticion.modalidad).toBe('CAMINATA');
    expect(peticion.idiomasAtencion).toEqual(['es', 'en']);
    expect(peticion.comoLlegar).toBe('Bus desde el mercado');
    expect(peticion.ubicacion).toEqual({ latitud: 11.14309, longitud: -74.11675 });
  });

  it('arma un paquete con sus actividades y sin la accesibilidad, que se calcula', () => {
    const peticion = aPeticion(
      {
        ...COMUN,
        tipo: 'PAQUETE',
        accesibleSillaRuedas: true,
        actividadIds: ['b00000000000000000000001', 'b00000000000000000000002'],
        duracionMinutos: 480,
        horariosSalida: '06:00',
        tipoActividad: 'SENDERISMO',
      },
      null,
      [],
    );

    expect(peticion.actividadIds).toEqual(['b00000000000000000000001', 'b00000000000000000000002']);
    expect(peticion.duracionMinutos).toBe(480);
    expect(peticion.horariosSalida).toEqual(['06:00']);
    expect(peticion.accesibleSillaRuedas).toBe(false);
    expect(peticion.tipoActividad).toBeUndefined();
  });

  it('arma un alojamiento con su tipo de unidad y un restaurante solo con lo común', () => {
    const alojamiento = aPeticion({ ...COMUN, tipo: 'ALOJAMIENTO', tipoUnidad: ' cabaña ' }, null, []);
    const restaurante = aPeticion(
      { ...COMUN, tipo: 'RESTAURANTE', tipoUnidad: 'cabaña', horariosSalida: '07:00' },
      null,
      [],
    );

    expect(alojamiento.tipoUnidad).toBe('cabaña');
    expect(restaurante.tipoUnidad).toBeUndefined();
    expect(restaurante.horariosSalida).toBeUndefined();
  });
});

describe('valoresDeFicha', () => {
  it('llena el formulario con la ficha de una actividad', () => {
    const ficha: FichaOferta = {
      id: 'b00000000000000000000001',
      tipo: 'ACTIVIDAD',
      nombre: 'Caminata a Pozo Azul',
      descripcion: 'Sendero entre cafetales.',
      precioReferencia: 80000,
      unidadPrecio: 'por persona',
      capacidadMaxima: 10,
      fotos: [
        {
          id: '3f2504e0-4f89-11d3-9a0c-0305e82c3301',
          url: '/api/catalogo/fotos/3f2504e0-4f89-11d3-9a0c-0305e82c3301/galeria',
          urlMiniatura: '/api/catalogo/fotos/3f2504e0-4f89-11d3-9a0c-0305e82c3301/miniatura',
        },
      ],
      accesibleSillaRuedas: false,
      ubicacion: { latitud: 11.14309, longitud: -74.11675 },
      actividad: {
        tipoActividad: 'SENDERISMO',
        nivelEsfuerzo: 'MODERADO',
        horariosSalida: ['07:30:00', '14:00'],
        idiomasAtencion: ['es', 'en'],
      },
      zona: {
        id: 'a00000000000000000000007',
        nombre: 'Minca',
        areaProtegida: false,
        altaAfluencia: false,
        activa: true,
        buenasPracticas: [],
      },
    };

    const { valores, ubicacion, fotos } = valoresDeFicha(ficha);

    expect(fotos).toEqual(ficha.fotos);
    expect(fotos[0].urlMiniatura).toBe('/api/catalogo/fotos/3f2504e0-4f89-11d3-9a0c-0305e82c3301/miniatura');
    expect(valores.zonaId).toBe('a00000000000000000000007');
    expect(valores.horariosSalida).toBe('07:30, 14:00');
    expect(valores.idiomasAtencion).toBe('es, en');
    expect(valores.duracionMinutos).toBeNull();
    expect(ubicacion).toEqual({ latitud: 11.14309, longitud: -74.11675 });
  });

  it('toma de la ficha de un paquete sus actividades, duración y horarios', () => {
    const ficha: FichaOferta = {
      id: 'b00000000000000000000010',
      tipo: 'PAQUETE',
      nombre: 'Sierra y mar',
      descripcion: 'Dos días.',
      precioReferencia: 350000,
      unidadPrecio: 'por persona',
      capacidadMaxima: 6,
      fotos: [],
      accesibleSillaRuedas: true,
      paquete: {
        actividades: [
          { id: 'b00000000000000000000001', nombre: 'Caminata' },
          { id: 'b00000000000000000000002', nombre: 'Kayak' },
        ],
        tiposActividad: ['SENDERISMO', 'NAUTICO'],
        nivelEsfuerzo: 'MODERADO',
        duracionMinutos: 480,
        horariosSalida: ['06:00'],
      },
    };

    const { valores, ubicacion, fotos } = valoresDeFicha(ficha);

    expect(valores.actividadIds).toEqual(['b00000000000000000000001', 'b00000000000000000000002']);
    expect(valores.duracionMinutos).toBe(480);
    expect(valores.horariosSalida).toBe('06:00');
    expect(valores.accesibleSillaRuedas).toBe(false);
    expect(ubicacion).toBeNull();
    // Una versión anterior a las fotos (DC-47) no tiene ninguna: el prestador tendrá que agregarlas para editarla.
    expect(fotos).toEqual([]);
  });
});
