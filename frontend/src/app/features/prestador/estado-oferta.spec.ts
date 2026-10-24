import { EstadoModeracion, OfertaPrestador, VersionOferta } from '../../core/api/modelos';
import { estadoDe, motivoDeRechazo, sePuedeEditar, versionVigente } from './estado-oferta';

function version(numero: number, estadoModeracion: EstadoModeracion, motivoRechazo?: string): VersionOferta {
  return {
    numero,
    estadoModeracion,
    enviadaEn: '2026-10-01T10:00:00Z',
    nombre: `Versión ${numero}`,
    precioReferencia: 100000 * numero,
    unidadPrecio: 'por persona',
    ...(motivoRechazo && { motivoRechazo }),
  };
}

function oferta(partes: Partial<OfertaPrestador>): OfertaPrestador {
  return { id: 'b00000000000000000000001', tipo: 'ACTIVIDAD', retirada: false, ...partes };
}

describe('estadoDe', () => {
  it('es "Publicada" con versión publicada y sin pendiente', () => {
    expect(estadoDe(oferta({ publicada: version(1, 'APROBADA') }))).toBe('PUBLICADA');
  });

  it('es "En moderación" cuando la primera versión espera revisión', () => {
    expect(estadoDe(oferta({ pendiente: version(1, 'EN_MODERACION') }))).toBe('EN_MODERACION');
  });

  it('es "Rechazada" cuando la versión pendiente fue rechazada', () => {
    expect(estadoDe(oferta({ pendiente: version(1, 'RECHAZADA', 'Faltan fotos') }))).toBe('RECHAZADA');
  });

  it('es "Publicada con cambios en revisión" con publicada y una edición en moderación', () => {
    const conEdicion = oferta({ publicada: version(1, 'APROBADA'), pendiente: version(2, 'EN_MODERACION') });

    expect(estadoDe(conEdicion)).toBe('PUBLICADA_CON_CAMBIOS');
  });

  it('es "Rechazada" si la edición de una oferta publicada fue rechazada', () => {
    const edicionRechazada = oferta({
      publicada: version(1, 'APROBADA'),
      pendiente: version(2, 'RECHAZADA', 'Precio sin justificar'),
    });

    expect(estadoDe(edicionRechazada)).toBe('RECHAZADA');
  });

  it('es "Retirada" sin importar sus versiones', () => {
    expect(estadoDe(oferta({ retirada: true, publicada: version(1, 'APROBADA') }))).toBe('RETIRADA');
    expect(estadoDe(oferta({ retirada: true, pendiente: version(1, 'RECHAZADA', 'x') }))).toBe('RETIRADA');
    expect(
      estadoDe(oferta({ retirada: true, publicada: version(1, 'APROBADA'), pendiente: version(2, 'EN_MODERACION') })),
    ).toBe('RETIRADA');
  });
});

describe('versionVigente', () => {
  it('prefiere la versión pendiente', () => {
    const conEdicion = oferta({ publicada: version(1, 'APROBADA'), pendiente: version(2, 'EN_MODERACION') });

    expect(versionVigente(conEdicion)?.numero).toBe(2);
  });

  it('usa la publicada si no hay pendiente', () => {
    expect(versionVigente(oferta({ publicada: version(1, 'APROBADA') }))?.numero).toBe(1);
  });
});

describe('motivoDeRechazo', () => {
  it('devuelve el motivo solo si la versión pendiente fue rechazada', () => {
    expect(motivoDeRechazo(oferta({ pendiente: version(1, 'RECHAZADA', 'Faltan fotos') }))).toBe('Faltan fotos');
    expect(motivoDeRechazo(oferta({ pendiente: version(1, 'EN_MODERACION') }))).toBeUndefined();
    expect(motivoDeRechazo(oferta({ publicada: version(1, 'APROBADA') }))).toBeUndefined();
  });
});

describe('sePuedeEditar', () => {
  it('permite editar una oferta con versión publicada', () => {
    expect(sePuedeEditar(oferta({ publicada: version(1, 'APROBADA') }))).toBe(true);
    expect(sePuedeEditar(oferta({ publicada: version(1, 'APROBADA'), pendiente: version(2, 'RECHAZADA', 'x') }))).toBe(
      true,
    );
  });

  it('no permite editar una oferta que solo tiene versión pendiente', () => {
    expect(sePuedeEditar(oferta({ pendiente: version(1, 'EN_MODERACION') }))).toBe(false);
  });

  it('no permite editar una oferta retirada', () => {
    expect(sePuedeEditar(oferta({ retirada: true, publicada: version(1, 'APROBADA') }))).toBe(false);
  });
});
