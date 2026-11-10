import { OfertaPrestador } from '../../../core/api/modelos';
import { EstadoRnt, PrestadorPropio } from '../../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../../core/idioma/tabla-de-textos';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { EstadoOferta, estadoDe } from '../estado-oferta';

// --- El RNT ---

/** Dónde va el RNT del prestador: el estado que dice el núcleo, o que todavía no lo registró. */
export type SituacionRnt = 'SIN_REGISTRAR' | EstadoRnt;

export function situacionRnt(prestador: PrestadorPropio | null): SituacionRnt {
  return prestador ? prestador.estadoRnt : 'SIN_REGISTRAR';
}

/** El nombre de cada estado del RNT: los mismos textos que ve el administrador. */
export const ETIQUETA_ESTADO_RNT = tablaDeTextos<EstadoRnt>({
  ACTIVO: 'admin.rnt.estado.ACTIVO',
  INACTIVO: 'admin.rnt.estado.INACTIVO',
  NO_ENCONTRADO: 'admin.rnt.estado.NO_ENCONTRADO',
  VERIFICACION_PENDIENTE: 'admin.rnt.estado.VERIFICACION_PENDIENTE',
});

export const CLAVE_SITUACION_RNT: Readonly<Record<SituacionRnt, ClaveTexto>> = {
  SIN_REGISTRAR: 'prestador.inicio.registro.sinRegistrar',
  ACTIVO: 'admin.rnt.estado.ACTIVO',
  INACTIVO: 'admin.rnt.estado.INACTIVO',
  NO_ENCONTRADO: 'admin.rnt.estado.NO_ENCONTRADO',
  VERIFICACION_PENDIENTE: 'admin.rnt.estado.VERIFICACION_PENDIENTE',
};

/** El color de la etiqueta de cada situación: una clase de las etiquetas globales. */
export const CLASE_SITUACION_RNT: Readonly<Record<SituacionRnt, string>> = {
  SIN_REGISTRAR: 'etiqueta--neutra',
  ACTIVO: 'etiqueta--exito',
  INACTIVO: 'etiqueta--peligro',
  NO_ENCONTRADO: 'etiqueta--peligro',
  VERIFICACION_PENDIENTE: 'etiqueta--aviso',
};

/** Lo que el dashboard le explica al prestador cuando su RNT no está activo. */
export interface AvisoRnt {
  readonly tono: 'aviso' | 'info';
  readonly titulo: ClaveTexto;
  readonly texto: ClaveTexto;
}

/**
 * El aviso del dashboard según el RNT, o `null` si está activo y no hay nada que explicar. Sin RNT activo las ofertas no se muestran
 * en el catálogo (RF-77), y el aviso lo dice con claridad y lleva a la sección RNT. Con la verificación pendiente el registro oficial
 * no respondió a tiempo: no hay nada que corregir y se revalida en la siguiente sincronización.
 */
export function avisoDeRnt(situacion: SituacionRnt): AvisoRnt | null {
  switch (situacion) {
    case 'SIN_REGISTRAR':
      return {
        tono: 'aviso',
        titulo: 'prestador.inicio.aviso.sinRegistrar.titulo',
        texto: 'prestador.inicio.aviso.sinRegistrar.texto',
      };
    case 'INACTIVO':
      return {
        tono: 'aviso',
        titulo: 'prestador.inicio.aviso.inactivo.titulo',
        texto: 'prestador.inicio.aviso.inactivo.texto',
      };
    case 'NO_ENCONTRADO':
      return {
        tono: 'aviso',
        titulo: 'prestador.inicio.aviso.noEncontrado.titulo',
        texto: 'prestador.inicio.aviso.noEncontrado.texto',
      };
    case 'VERIFICACION_PENDIENTE':
      return {
        tono: 'info',
        titulo: 'prestador.inicio.aviso.pendiente.titulo',
        texto: 'prestador.inicio.aviso.pendiente.texto',
      };
    case 'ACTIVO':
      return null;
  }
}

// --- El WhatsApp de contacto ---

export type SituacionWhatsapp = 'NO_DISPONIBLE' | 'SIN_CONFIGURAR' | 'SIN_VERIFICAR' | 'VERIFICADO';

/** Sin RNT registrado el núcleo no tiene al prestador, así que todavía no se puede configurar el WhatsApp. */
export function situacionWhatsapp(prestador: PrestadorPropio | null): SituacionWhatsapp {
  if (!prestador) {
    return 'NO_DISPONIBLE';
  }
  if (!prestador.whatsapp) {
    return 'SIN_CONFIGURAR';
  }
  return prestador.whatsappVerificado ? 'VERIFICADO' : 'SIN_VERIFICAR';
}

export const CLAVE_SITUACION_WHATSAPP: Readonly<Record<SituacionWhatsapp, ClaveTexto>> = {
  NO_DISPONIBLE: 'prestador.inicio.whatsapp.noDisponible',
  SIN_CONFIGURAR: 'prestador.inicio.whatsapp.sinConfigurar',
  SIN_VERIFICAR: 'prestador.inicio.whatsapp.sinVerificar',
  VERIFICADO: 'prestador.inicio.whatsapp.verificado',
};

export const CLASE_SITUACION_WHATSAPP: Readonly<Record<SituacionWhatsapp, string>> = {
  NO_DISPONIBLE: 'etiqueta--neutra',
  SIN_CONFIGURAR: 'etiqueta--aviso',
  SIN_VERIFICAR: 'etiqueta--aviso',
  VERIFICADO: 'etiqueta--exito',
};

// --- Las ofertas ---

/** Los estados de una oferta en el orden en que el dashboard los cuenta. */
export const ESTADOS_DEL_DASHBOARD: readonly EstadoOferta[] = [
  'PUBLICADA',
  'PUBLICADA_CON_CAMBIOS',
  'EN_MODERACION',
  'RECHAZADA',
  'RETIRADA',
];

/** Cuántas ofertas hay en cada estado (con el estado que calcula `estadoDe`). Todos los estados salen, también en cero. */
export function contarPorEstado(ofertas: readonly OfertaPrestador[]): Record<EstadoOferta, number> {
  const conteo: Record<EstadoOferta, number> = {
    PUBLICADA: 0,
    PUBLICADA_CON_CAMBIOS: 0,
    EN_MODERACION: 0,
    RECHAZADA: 0,
    RETIRADA: 0,
  };
  for (const oferta of ofertas) {
    conteo[estadoDe(oferta)] += 1;
  }
  return conteo;
}
