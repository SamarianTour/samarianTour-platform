import { OfertaPrestador } from '../../../core/api/modelos';
import { PrestadorPropio } from '../../../core/api/modelos-nucleo';
import {
  ESTADOS_DEL_DASHBOARD,
  avisoDeRnt,
  contarPorEstado,
  situacionRnt,
  situacionWhatsapp,
} from './inicio';

function prestador(extra: Partial<PrestadorPropio> = {}): PrestadorPropio {
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

function oferta(extra: Partial<OfertaPrestador> = {}): OfertaPrestador {
  return { id: 'o1', tipo: 'ACTIVIDAD', retirada: false, ...extra };
}

const VERSION = {
  numero: 1,
  estadoModeracion: 'APROBADA',
  enviadaEn: '2026-11-01T10:00:00Z',
  nombre: 'Caminata',
  precioReferencia: 1,
  unidadPrecio: 'por persona',
} as const;

describe('el RNT en el dashboard', () => {
  it('sin registro la situación es "sin registrar"', () => {
    expect(situacionRnt(null)).toBe('SIN_REGISTRAR');
  });

  it('con registro es el estado que dice el núcleo', () => {
    expect(situacionRnt(prestador({ estadoRnt: 'INACTIVO' }))).toBe('INACTIVO');
    expect(situacionRnt(prestador({ estadoRnt: 'VERIFICACION_PENDIENTE' }))).toBe('VERIFICACION_PENDIENTE');
  });

  it('un RNT activo no necesita aviso', () => {
    expect(avisoDeRnt('ACTIVO')).toBeNull();
  });

  it.each(['SIN_REGISTRAR', 'INACTIVO', 'NO_ENCONTRADO'] as const)(
    'con el RNT %s el aviso es de advertencia',
    (situacion) => {
      expect(avisoDeRnt(situacion)?.tono).toBe('aviso');
    },
  );

  it('con la verificación pendiente el aviso es informativo: no hay nada que corregir', () => {
    expect(avisoDeRnt('VERIFICACION_PENDIENTE')?.tono).toBe('info');
  });

  it('cada situación sin RNT activo tiene su propio aviso', () => {
    const titulos = (['SIN_REGISTRAR', 'INACTIVO', 'NO_ENCONTRADO', 'VERIFICACION_PENDIENTE'] as const).map(
      (situacion) => avisoDeRnt(situacion)?.titulo,
    );

    expect(new Set(titulos).size).toBe(4);
  });
});

describe('el WhatsApp en el dashboard', () => {
  it('sin registro del RNT todavía no se puede configurar', () => {
    expect(situacionWhatsapp(null)).toBe('NO_DISPONIBLE');
  });

  it('sin número está sin configurar', () => {
    expect(situacionWhatsapp(prestador())).toBe('SIN_CONFIGURAR');
  });

  it('con número y sin verificar falta el código', () => {
    expect(situacionWhatsapp(prestador({ whatsapp: '+573001110202', whatsappVerificado: false }))).toBe('SIN_VERIFICAR');
    expect(situacionWhatsapp(prestador({ whatsapp: '+573001110202' }))).toBe('SIN_VERIFICAR');
  });

  it('con número verificado está verificado', () => {
    expect(situacionWhatsapp(prestador({ whatsapp: '+573001110202', whatsappVerificado: true }))).toBe('VERIFICADO');
  });
});

describe('las ofertas por estado', () => {
  it('sin ofertas todos los estados salen en cero', () => {
    const conteo = contarPorEstado([]);

    expect(Object.keys(conteo).sort()).toEqual([...ESTADOS_DEL_DASHBOARD].sort());
    expect(Object.values(conteo).every((n) => n === 0)).toBe(true);
  });

  it('cuenta cada oferta en el estado que calcula la lista', () => {
    const conteo = contarPorEstado([
      oferta({ id: '1', publicada: VERSION }),
      oferta({ id: '2', publicada: VERSION }),
      oferta({ id: '3', publicada: VERSION, pendiente: { ...VERSION, estadoModeracion: 'EN_MODERACION' } }),
      oferta({ id: '4', pendiente: { ...VERSION, estadoModeracion: 'EN_MODERACION' } }),
      oferta({ id: '5', pendiente: { ...VERSION, estadoModeracion: 'RECHAZADA', motivoRechazo: 'x' } }),
      oferta({ id: '6', retirada: true, publicada: VERSION }),
    ]);

    expect(conteo).toEqual({
      PUBLICADA: 2,
      PUBLICADA_CON_CAMBIOS: 1,
      EN_MODERACION: 1,
      RECHAZADA: 1,
      RETIRADA: 1,
    });
  });
});
