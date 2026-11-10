import { ContenidoVersion } from '../../../core/api/modelos';
import { ValoresOferta, aLista, aPeticion, valoresDeContenido } from './peticion-oferta';

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

describe('valoresDeContenido', () => {
  const FOTO = {
    id: '3f2504e0-4f89-11d3-9a0c-0305e82c3301',
    url: '/api/catalogo/fotos/3f2504e0-4f89-11d3-9a0c-0305e82c3301/galeria',
    urlMiniatura: '/api/catalogo/fotos/3f2504e0-4f89-11d3-9a0c-0305e82c3301/miniatura',
  };

  it('llena el formulario con el contenido de una actividad', () => {
    const contenido: ContenidoVersion = {
      nombre: 'Caminata a Pozo Azul',
      descripcion: 'Sendero entre cafetales.',
      precioReferencia: 80000,
      unidadPrecio: 'por persona',
      zonaId: 'a00000000000000000000007',
      capacidadMaxima: 10,
      fotos: [FOTO],
      ubicacion: { latitud: 11.14309, longitud: -74.11675 },
      tipoActividad: 'SENDERISMO',
      nivelEsfuerzo: 'MODERADO',
      horariosSalida: ['07:30:00', '14:00'],
      puntoEncuentro: 'Parque central',
      duracionMinutos: 180,
      modalidad: 'CAMINATA',
      idiomasAtencion: ['es', 'en'],
      accesibleSillaRuedas: true,
    };

    const { valores, ubicacion, fotos } = valoresDeContenido('ACTIVIDAD', contenido);

    expect(valores.tipo).toBe('ACTIVIDAD');
    expect(valores.zonaId).toBe('a00000000000000000000007');
    expect(valores.horariosSalida).toBe('07:30, 14:00');
    expect(valores.idiomasAtencion).toBe('es, en');
    expect(valores.puntoEncuentro).toBe('Parque central');
    expect(valores.duracionMinutos).toBe(180);
    expect(valores.modalidad).toBe('CAMINATA');
    expect(valores.accesibleSillaRuedas).toBe(true);
    expect(ubicacion).toEqual({ latitud: 11.14309, longitud: -74.11675 });
    expect(fotos).toEqual([FOTO]);
    expect(fotos[0].urlMiniatura).toBe(FOTO.urlMiniatura);
  });

  it('toma de un paquete sus actividades, duración y horarios; su accesibilidad no se escribe', () => {
    const contenido: ContenidoVersion = {
      nombre: 'Sierra y mar',
      descripcion: 'Dos días.',
      precioReferencia: 350000,
      unidadPrecio: 'por persona',
      zonaId: 'z1',
      capacidadMaxima: 6,
      fotos: [],
      accesibleSillaRuedas: true,
      actividadIds: ['b00000000000000000000001', 'b00000000000000000000002'],
      duracionMinutos: 480,
      horariosSalida: ['06:00'],
    };

    const { valores, ubicacion, fotos } = valoresDeContenido('PAQUETE', contenido);

    expect(valores.actividadIds).toEqual(['b00000000000000000000001', 'b00000000000000000000002']);
    expect(valores.duracionMinutos).toBe(480);
    expect(valores.horariosSalida).toBe('06:00');
    expect(valores.accesibleSillaRuedas).toBe(false);
    expect(ubicacion).toBeNull();
    // Una versión anterior a las fotos (DC-47) no tiene ninguna: el prestador tendrá que agregarlas para editarla.
    expect(fotos).toEqual([]);
  });

  it('los campos que no corresponden al tipo quedan vacíos', () => {
    const contenido: ContenidoVersion = {
      nombre: 'Cabañas del río',
      descripcion: 'Alojamiento.',
      precioReferencia: 120000,
      unidadPrecio: 'por noche y unidad',
      zonaId: 'z1',
      capacidadMaxima: 4,
      fotos: [],
      tipoUnidad: 'cabaña',
    };

    const { valores } = valoresDeContenido('ALOJAMIENTO', contenido);

    expect(valores.tipoUnidad).toBe('cabaña');
    expect(valores.tipoActividad).toBe('');
    expect(valores.nivelEsfuerzo).toBe('');
    expect(valores.horariosSalida).toBe('');
    expect(valores.idiomasAtencion).toBe('');
    expect(valores.duracionMinutos).toBeNull();
    expect(valores.actividadIds).toEqual([]);
  });
});
