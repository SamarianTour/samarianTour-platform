import { Zona } from '../../core/api/modelos';
import {
  FILTROS_ZONAS,
  VALORES_ZONA_VACIOS,
  ValoresZona,
  buenasPracticasALineas,
  construirEdicion,
  construirZona,
  filtrarZonas,
  filtroDeZonas,
  lineasABuenasPracticas,
  ordenarZonasPorNombre,
  peticionDesdeZona,
  validarZona,
  valoresDeZona,
} from './zonas';

const VALIDOS: ValoresZona = {
  nombre: 'Isla de Salamanca',
  descripcion: 'Vía Parque entre Barranquilla y Ciénaga',
  latitud: 10.98,
  longitud: -74.47,
  areaProtegida: true,
  altaAfluencia: false,
  buenasPracticas: 'No alimentar a los animales\nLlevar de vuelta la basura',
  condicionesVisita: 'Solo en horas de la mañana',
};

function zona(extra: Partial<Zona> = {}): Zona {
  return {
    id: 'z1',
    nombre: 'Taganga',
    descripcion: 'Bahía de pescadores',
    ubicacion: { latitud: 11.27, longitud: -74.19 },
    areaProtegida: false,
    altaAfluencia: true,
    activa: true,
    buenasPracticas: ['No dejar basura'],
    condicionesVisita: 'Llevar agua',
    ...extra,
  };
}

describe('validarZona', () => {
  it('una zona completa no tiene errores', () => {
    expect(validarZona(VALIDOS)).toEqual({});
  });

  it('el formulario vacío exige el nombre, la descripción y la ubicación', () => {
    expect(validarZona(VALORES_ZONA_VACIOS)).toEqual({
      nombre: 'nombreRequerido',
      descripcion: 'descripcionRequerida',
      latitud: 'latitudRequerida',
      longitud: 'longitudRequerida',
    });
  });

  it('un nombre o una descripción de solo espacios cuenta como vacío', () => {
    const errores = validarZona({ ...VALIDOS, nombre: '   ', descripcion: ' \n ' });

    expect(errores.nombre).toBe('nombreRequerido');
    expect(errores.descripcion).toBe('descripcionRequerida');
  });

  it('la latitud va de -90 a 90 y la longitud de -180 a 180', () => {
    expect(validarZona({ ...VALIDOS, latitud: 90, longitud: 180 })).toEqual({});
    expect(validarZona({ ...VALIDOS, latitud: -90, longitud: -180 })).toEqual({});
    expect(validarZona({ ...VALIDOS, latitud: 90.01 }).latitud).toBe('latitudRango');
    expect(validarZona({ ...VALIDOS, latitud: -91 }).latitud).toBe('latitudRango');
    expect(validarZona({ ...VALIDOS, longitud: 180.5 }).longitud).toBe('longitudRango');
    expect(validarZona({ ...VALIDOS, longitud: -200 }).longitud).toBe('longitudRango');
  });

  it('el cero es una coordenada válida, no un campo vacío', () => {
    expect(validarZona({ ...VALIDOS, latitud: 0, longitud: 0 })).toEqual({});
  });

  it('una coordenada que no es un número se trata como vacía', () => {
    expect(validarZona({ ...VALIDOS, latitud: Number.NaN }).latitud).toBe('latitudRequerida');
    expect(validarZona({ ...VALIDOS, longitud: Number.POSITIVE_INFINITY }).longitud).toBe('longitudRequerida');
  });

  it('las marcas y las buenas prácticas no son obligatorias', () => {
    expect(validarZona({ ...VALIDOS, areaProtegida: false, buenasPracticas: '', condicionesVisita: '' })).toEqual({});
  });
});

describe('buenas prácticas', () => {
  it('se escriben una por línea, sin espacios en los extremos y sin líneas en blanco', () => {
    expect(lineasABuenasPracticas('  No alimentar a los animales \r\n\n   \nLlevar la basura\n')).toEqual([
      'No alimentar a los animales',
      'Llevar la basura',
    ]);
  });

  it('un texto vacío no tiene buenas prácticas', () => {
    expect(lineasABuenasPracticas('')).toEqual([]);
    expect(lineasABuenasPracticas('  \n  ')).toEqual([]);
  });

  it('para el formulario se vuelven a poner una por línea', () => {
    expect(buenasPracticasALineas(['a', 'b'])).toBe('a\nb');
    expect(buenasPracticasALineas([])).toBe('');
    expect(buenasPracticasALineas(undefined)).toBe('');
  });

  it('ida y vuelta no cambia las prácticas', () => {
    const practicas = ['No alimentar a los animales', 'Llevar la basura'];

    expect(lineasABuenasPracticas(buenasPracticasALineas(practicas))).toEqual(practicas);
  });
});

describe('construirZona', () => {
  it('arma el cuerpo con los textos sin espacios en los extremos y la ubicación como coordenada', () => {
    expect(
      construirZona({ ...VALIDOS, nombre: '  Isla de Salamanca ', descripcion: ' Vía Parque ', condicionesVisita: ' Solo en la mañana ' }),
    ).toEqual({
      nombre: 'Isla de Salamanca',
      descripcion: 'Vía Parque',
      ubicacion: { latitud: 10.98, longitud: -74.47 },
      areaProtegida: true,
      altaAfluencia: false,
      buenasPracticas: ['No alimentar a los animales', 'Llevar de vuelta la basura'],
      condicionesVisita: 'Solo en la mañana',
    });
  });

  it('las marcas se mandan siempre, también en false, porque editar reemplaza lo que no se envía', () => {
    const cuerpo = construirZona({ ...VALIDOS, areaProtegida: false, altaAfluencia: false });

    expect(cuerpo.areaProtegida).toBe(false);
    expect(cuerpo.altaAfluencia).toBe(false);
  });

  it('las condiciones de visita solo van si se escribieron', () => {
    expect(construirZona({ ...VALIDOS, condicionesVisita: '   ' })).not.toHaveProperty('condicionesVisita');
  });
});

describe('construirEdicion', () => {
  it('es el cuerpo de la zona más si queda activa o inactiva', () => {
    expect(construirEdicion(VALIDOS, true)).toEqual({ ...construirZona(VALIDOS), activa: true });
    expect(construirEdicion(VALIDOS, false).activa).toBe(false);
  });
});

describe('valoresDeZona', () => {
  it('llena el formulario con los datos de la zona', () => {
    expect(valoresDeZona(zona())).toEqual({
      nombre: 'Taganga',
      descripcion: 'Bahía de pescadores',
      latitud: 11.27,
      longitud: -74.19,
      areaProtegida: false,
      altaAfluencia: true,
      buenasPracticas: 'No dejar basura',
      condicionesVisita: 'Llevar agua',
    });
  });

  it('lo que la zona no trae queda vacío', () => {
    const valores = valoresDeZona(zona({ descripcion: undefined, ubicacion: undefined, condicionesVisita: undefined, buenasPracticas: [] }));

    expect(valores.descripcion).toBe('');
    expect(valores.latitud).toBeNull();
    expect(valores.longitud).toBeNull();
    expect(valores.condicionesVisita).toBe('');
    expect(valores.buenasPracticas).toBe('');
  });
});

describe('peticionDesdeZona (inactivar y reactivar sin perder datos)', () => {
  it('manda la zona completa con la marca de activa cambiada', () => {
    const cuerpo = peticionDesdeZona(zona(), false);

    expect(cuerpo).toEqual({
      nombre: 'Taganga',
      descripcion: 'Bahía de pescadores',
      ubicacion: { latitud: 11.27, longitud: -74.19 },
      areaProtegida: false,
      altaAfluencia: true,
      buenasPracticas: ['No dejar basura'],
      condicionesVisita: 'Llevar agua',
      activa: false,
    });
  });

  it('conserva las marcas y las buenas prácticas de una zona protegida', () => {
    const cuerpo = peticionDesdeZona(zona({ areaProtegida: true, altaAfluencia: false, buenasPracticas: ['a', 'b'] }), true);

    expect(cuerpo).toMatchObject({ areaProtegida: true, altaAfluencia: false, buenasPracticas: ['a', 'b'], activa: true });
  });

  it('si a la zona le falta la descripción o la ubicación no se puede reenviar tal cual', () => {
    expect(peticionDesdeZona(zona({ descripcion: undefined }), false)).toBeNull();
    expect(peticionDesdeZona(zona({ ubicacion: undefined }), false)).toBeNull();
  });
});

describe('filtrar las zonas', () => {
  const zonas = [zona({ id: 'a' }), zona({ id: 'b', activa: false }), zona({ id: 'c' })];

  it('hay tres filtros', () => {
    expect(FILTROS_ZONAS).toEqual(['todas', 'activas', 'inactivas']);
  });

  it('el filtro de la dirección acepta activas e inactivas y, si no, muestra todas', () => {
    expect(filtroDeZonas('activas')).toBe('activas');
    expect(filtroDeZonas('inactivas')).toBe('inactivas');
    for (const valor of [null, '', 'todas', 'x', 'ACTIVAS']) {
      expect(filtroDeZonas(valor)).toBe('todas');
    }
  });

  it('filtra por estado y conserva el orden', () => {
    expect(filtrarZonas(zonas, 'todas').map((z) => z.id)).toEqual(['a', 'b', 'c']);
    expect(filtrarZonas(zonas, 'activas').map((z) => z.id)).toEqual(['a', 'c']);
    expect(filtrarZonas(zonas, 'inactivas').map((z) => z.id)).toEqual(['b']);
  });

  it('no modifica la lista original', () => {
    const copia = [...zonas];

    filtrarZonas(zonas, 'todas');

    expect(zonas).toEqual(copia);
  });
});

describe('ordenarZonasPorNombre', () => {
  it('ordena por nombre sin distinguir mayúsculas ni tildes', () => {
    const orden = ordenarZonasPorNombre([
      zona({ id: '1', nombre: 'taganga' }),
      zona({ id: '2', nombre: 'Ciénaga' }),
      zona({ id: '3', nombre: 'Buritaca' }),
      zona({ id: '4', nombre: 'Álvarez' }),
    ]);

    expect(orden.map((z) => z.nombre)).toEqual(['Álvarez', 'Buritaca', 'Ciénaga', 'taganga']);
  });

  it('no modifica la lista original', () => {
    const lista = [zona({ id: '1', nombre: 'b' }), zona({ id: '2', nombre: 'a' })];
    const copia = [...lista];

    ordenarZonasPorNombre(lista);

    expect(lista).toEqual(copia);
  });
});
