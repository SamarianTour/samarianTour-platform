import { ContenidoVersion, OfertaPendiente, Zona } from '../../core/api/modelos';
import { establecerIdioma } from '../../core/idioma/idioma';
import {
  MOTIVO_MAXIMO,
  camposCambiados,
  decisionDeOferta,
  decisionDeOpinion,
  filasDeVersion,
  puedeAprobar,
  tipoDeCola,
  validarDecision,
} from './moderacion';

const ZONAS = [
  { id: 'z1', nombre: 'Taganga', altaAfluencia: false, areaProtegida: false, activa: true, buenasPracticas: [] },
] as Zona[];

function contenido(extra: Partial<ContenidoVersion> = {}): ContenidoVersion {
  return {
    nombre: 'Caminata a Marinka',
    descripcion: 'Una caminata por la Sierra.',
    precioReferencia: 90000,
    unidadPrecio: 'por persona',
    zonaId: 'z1',
    capacidadMaxima: 10,
    fotos: [],
    ...extra,
  };
}

describe('tipoDeCola', () => {
  it('?tipo=opiniones abre la cola de opiniones', () => {
    expect(tipoDeCola('opiniones')).toBe('opiniones');
  });

  it.each([null, '', 'ofertas', 'OPINIONES', 'x'])('con %j abre la de ofertas', (valor) => {
    expect(tipoDeCola(valor)).toBe('ofertas');
  });
});

describe('validarDecision', () => {
  it('aprobar no necesita motivo', () => {
    expect(validarDecision('APROBAR', '')).toBeNull();
    expect(validarDecision('APROBAR', 'cualquier cosa')).toBeNull();
  });

  it('rechazar exige un motivo', () => {
    expect(validarDecision('RECHAZAR', '')).toBe('motivoRequerido');
    expect(validarDecision('RECHAZAR', '   ')).toBe('motivoRequerido');
    expect(validarDecision('RECHAZAR', 'Falta el punto de encuentro')).toBeNull();
  });

  it('el motivo admite hasta 1000 caracteres', () => {
    expect(MOTIVO_MAXIMO).toBe(1000);
    expect(validarDecision('RECHAZAR', 'a'.repeat(1000))).toBeNull();
    expect(validarDecision('RECHAZAR', 'a'.repeat(1001))).toBe('motivoLargo');
  });
});

describe('armar la decisión', () => {
  it('una oferta lleva el número de la versión revisada y, al rechazar, el motivo recortado', () => {
    expect(decisionDeOferta(2, 'RECHAZAR', '  Falta el punto de encuentro  ')).toEqual({
      numeroVersion: 2,
      decision: 'RECHAZAR',
      motivo: 'Falta el punto de encuentro',
    });
  });

  it('aprobar una oferta no manda motivo, aunque se haya escrito uno antes', () => {
    expect(decisionDeOferta(1, 'APROBAR', 'borrador')).toEqual({ numeroVersion: 1, decision: 'APROBAR' });
  });

  it('una opinión no lleva número de versión', () => {
    expect(decisionDeOpinion('RECHAZAR', ' Lenguaje ofensivo ')).toEqual({
      decision: 'RECHAZAR',
      motivo: 'Lenguaje ofensivo',
    });
    expect(decisionDeOpinion('APROBAR', 'x')).toEqual({ decision: 'APROBAR' });
  });
});

describe('puedeAprobar', () => {
  it('solo si el prestador tiene el RNT activo (DN-35)', () => {
    expect(puedeAprobar({ rntPrestadorActivo: true } as OfertaPendiente)).toBe(true);
    expect(puedeAprobar({ rntPrestadorActivo: false } as OfertaPendiente)).toBe(false);
  });
});

describe('camposCambiados', () => {
  it('una oferta nueva (sin versión publicada) no tiene nada que comparar', () => {
    expect(camposCambiados(contenido(), undefined).size).toBe(0);
    expect(camposCambiados(contenido(), null).size).toBe(0);
  });

  it('marca solo los campos que cambiaron en la edición', () => {
    const cambios = camposCambiados(
      contenido({ nombre: 'Caminata a Marinka (nueva)', precioReferencia: 95000 }),
      contenido(),
    );

    expect([...cambios].sort()).toEqual(['nombre', 'precioReferencia']);
  });

  it('detecta un campo que la versión publicada no tenía, o que dejó de tener', () => {
    const cambios = camposCambiados(contenido({ horarios: '8 a 5' }), contenido({ comoLlegar: 'Por la vía' }));

    expect([...cambios].sort()).toEqual(['comoLlegar', 'horarios']);
  });

  it('las fotos se comparan por su identificador y su orden', () => {
    const foto = (id: string) => ({ id, url: `/${id}`, urlMiniatura: `/m/${id}` });
    const igual = camposCambiados(contenido({ fotos: [foto('a'), foto('b')] }), contenido({ fotos: [foto('a'), foto('b')] }));
    const otroOrden = camposCambiados(contenido({ fotos: [foto('b'), foto('a')] }), contenido({ fotos: [foto('a'), foto('b')] }));

    expect(igual.has('fotos')).toBe(false);
    expect(otroOrden.has('fotos')).toBe(true);
  });

  it('dos versiones iguales no tienen cambios', () => {
    expect(camposCambiados(contenido(), contenido()).size).toBe(0);
  });
});

describe('filasDeVersion', () => {
  beforeEach(() => establecerIdioma('es', { recordar: false }));

  const valores = (filas: ReturnType<typeof filasDeVersion>) =>
    Object.fromEntries(filas.map((f) => [f.campo, f.valor.replace(/[\u00a0\u202f]/g, ' ')]));

  it('muestra solo lo que la versión trae, en un orden fijo', () => {
    const filas = filasDeVersion(contenido(), undefined, ZONAS);

    expect(filas.map((f) => f.campo)).toEqual([
      'nombre',
      'descripcion',
      'precioReferencia',
      'zonaId',
      'capacidadMaxima',
    ]);
  });

  it('escribe el precio con su unidad y la zona con su nombre', () => {
    const v = valores(filasDeVersion(contenido(), undefined, ZONAS));

    expect(v['precioReferencia']).toBe('$ 90.000 por persona');
    expect(v['zonaId']).toBe('Taganga');
  });

  it('si no encuentra la zona deja su identificador', () => {
    expect(valores(filasDeVersion(contenido({ zonaId: 'zx' }), undefined, ZONAS))['zonaId']).toBe('zx');
  });

  it('para una actividad escribe el tipo, el esfuerzo, las horas, la duración y los idiomas con sus nombres', () => {
    const v = valores(
      filasDeVersion(
        contenido({
          tipoActividad: 'SENDERISMO',
          nivelEsfuerzo: 'BAJO',
          accesibleSillaRuedas: false,
          horariosSalida: ['07:00', '14:30'],
          puntoEncuentro: 'Parque Bolívar',
          duracionMinutos: 150,
          modalidad: 'CAMINATA',
          idiomasAtencion: ['es', 'en', 'fr'],
        }),
        undefined,
        ZONAS,
      ),
    );

    expect(v['horariosSalida']).toBe('07:00, 14:30');
    expect(v['duracionMinutos']).toBe('2 h 30 min');
    expect(v['idiomasAtencion']).toBe('Español, Inglés, fr');
    expect(v['accesibleSillaRuedas']).toBe('No');
    expect(v['puntoEncuentro']).toBe('Parque Bolívar');
  });

  it('"accesible en silla de ruedas" se muestra también cuando es que no', () => {
    const si = valores(filasDeVersion(contenido({ accesibleSillaRuedas: true }), undefined, ZONAS));
    const no = valores(filasDeVersion(contenido({ accesibleSillaRuedas: false }), undefined, ZONAS));

    expect(si['accesibleSillaRuedas']).toBe('Sí');
    expect(no['accesibleSillaRuedas']).toBe('No');
  });

  it('un paquete cuenta sus actividades', () => {
    const v = valores(filasDeVersion(contenido({ actividadIds: ['a1', 'a2', 'a3'] }), undefined, ZONAS));

    expect(v['actividadIds']).toBe('3 actividades');
  });

  it('las líneas largas ocupan todo el ancho', () => {
    const filas = filasDeVersion(contenido({ comoLlegar: 'Por la vía principal' }), undefined, ZONAS);

    expect(filas.find((f) => f.campo === 'descripcion')?.ancho).toBe(true);
    expect(filas.find((f) => f.campo === 'comoLlegar')?.ancho).toBe(true);
    expect(filas.find((f) => f.campo === 'nombre')?.ancho).toBe(false);
  });

  it('en una edición marca las líneas que cambiaron frente a la versión publicada', () => {
    const filas = filasDeVersion(contenido({ precioReferencia: 95000 }), contenido(), ZONAS);

    expect(filas.filter((f) => f.cambiado).map((f) => f.campo)).toEqual(['precioReferencia']);
  });

  it('en una oferta nueva ninguna línea se marca como cambiada', () => {
    expect(filasDeVersion(contenido(), undefined, ZONAS).some((f) => f.cambiado)).toBe(false);
  });

  it('los textos siguen el idioma', () => {
    establecerIdioma('en', { recordar: false });

    const v = valores(
      filasDeVersion(contenido({ accesibleSillaRuedas: true, idiomasAtencion: ['es'] }), undefined, ZONAS),
    );

    expect(v['accesibleSillaRuedas']).toBe('Yes');
    expect(v['precioReferencia']).toBe('COP 90,000 per person');
    expect(v['idiomasAtencion']).toBe('Spanish');
  });
});
