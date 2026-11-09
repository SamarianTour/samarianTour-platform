import {
  EstadoModeracion,
  EstadoOpinion,
  ModalidadRecorrido,
  NivelEsfuerzo,
  TipoActividad,
  TipoOferta,
  TipoPrestador,
} from '../core/api/modelos';
import { Rol } from '../core/api/modelos-nucleo';
import { ETIQUETAS } from '../core/idioma/etiquetas.i18n';
import { tablaDeTextos } from '../core/idioma/tabla-de-textos';
import type { ClaveTexto } from '../core/idioma/textos';
import { traducir } from '../core/idioma/traducir';

/**
 * Textos que ve el usuario para los valores del contrato, en el idioma vigente (los diccionarios están en
 * `core/idioma/etiquetas.i18n.ts`). Cada tabla exige todos los valores de su tipo: si el contrato agrega uno, el build
 * falla aquí hasta ponerle nombre. Se usan como siempre (`ROL[rol]`) y siguen el idioma al cambiarlo.
 */
export const ROL = tablaDeTextos<Rol>({
  TURISTA: 'etiquetas.rol.TURISTA',
  PRESTADOR: 'etiquetas.rol.PRESTADOR',
  ADMINISTRADOR: 'etiquetas.rol.ADMINISTRADOR',
  OPERADOR_DISPONIBILIDAD: 'etiquetas.rol.OPERADOR_DISPONIBILIDAD',
  GESTOR_DESTINO: 'etiquetas.rol.GESTOR_DESTINO',
});

export const TIPO_OFERTA = tablaDeTextos<TipoOferta>({
  ACTIVIDAD: 'etiquetas.tipoOferta.ACTIVIDAD',
  PAQUETE: 'etiquetas.tipoOferta.PAQUETE',
  ALOJAMIENTO: 'etiquetas.tipoOferta.ALOJAMIENTO',
  RESTAURANTE: 'etiquetas.tipoOferta.RESTAURANTE',
});

export const TIPO_ACTIVIDAD = tablaDeTextos<TipoActividad>({
  SENDERISMO: 'etiquetas.tipoActividad.SENDERISMO',
  BUCEO_SNORKEL: 'etiquetas.tipoActividad.BUCEO_SNORKEL',
  DEPORTES_ACUATICOS: 'etiquetas.tipoActividad.DEPORTES_ACUATICOS',
  NAUTICO: 'etiquetas.tipoActividad.NAUTICO',
  AVISTAMIENTO_FAUNA: 'etiquetas.tipoActividad.AVISTAMIENTO_FAUNA',
  CULTURAL_HISTORICO: 'etiquetas.tipoActividad.CULTURAL_HISTORICO',
  ETNOTURISMO: 'etiquetas.tipoActividad.ETNOTURISMO',
  GASTRONOMICO: 'etiquetas.tipoActividad.GASTRONOMICO',
  CICLISMO: 'etiquetas.tipoActividad.CICLISMO',
  AVENTURA: 'etiquetas.tipoActividad.AVENTURA',
  PLAYA_RELAX: 'etiquetas.tipoActividad.PLAYA_RELAX',
  OTRO: 'etiquetas.tipoActividad.OTRO',
});

export const NIVEL_ESFUERZO = tablaDeTextos<NivelEsfuerzo>({
  BAJO: 'etiquetas.nivelEsfuerzo.BAJO',
  MODERADO: 'etiquetas.nivelEsfuerzo.MODERADO',
  ALTA_EXIGENCIA: 'etiquetas.nivelEsfuerzo.ALTA_EXIGENCIA',
});

export const MODALIDAD = tablaDeTextos<ModalidadRecorrido>({
  CAMINATA: 'etiquetas.modalidad.CAMINATA',
  BICICLETA: 'etiquetas.modalidad.BICICLETA',
  KAYAK: 'etiquetas.modalidad.KAYAK',
  OTRA: 'etiquetas.modalidad.OTRA',
});

export const ESTADO_MODERACION = tablaDeTextos<EstadoModeracion>({
  EN_MODERACION: 'etiquetas.estadoModeracion.EN_MODERACION',
  APROBADA: 'etiquetas.estadoModeracion.APROBADA',
  RECHAZADA: 'etiquetas.estadoModeracion.RECHAZADA',
});

export const TIPO_PRESTADOR = tablaDeTextos<TipoPrestador>({
  AGENCIA_OPERADORA: 'etiquetas.tipoPrestador.AGENCIA_OPERADORA',
  GUIA_INDEPENDIENTE: 'etiquetas.tipoPrestador.GUIA_INDEPENDIENTE',
  ALOJAMIENTO: 'etiquetas.tipoPrestador.ALOJAMIENTO',
  RESTAURANTE: 'etiquetas.tipoPrestador.RESTAURANTE',
});

/**
 * Unidad del precio de referencia por tipo de oferta (DC-22).
 * [SUPUESTO] La búsqueda y la ficha la traen en `unidadPrecio`, pero la lista de ofertas propias no:
 * allí se toma de esta tabla.
 */
export const UNIDAD_PRECIO = tablaDeTextos<TipoOferta>({
  ACTIVIDAD: 'etiquetas.unidadPrecio.ACTIVIDAD',
  PAQUETE: 'etiquetas.unidadPrecio.PAQUETE',
  ALOJAMIENTO: 'etiquetas.unidadPrecio.ALOJAMIENTO',
  RESTAURANTE: 'etiquetas.unidadPrecio.RESTAURANTE',
});

/** Las unidades que la API entrega en español, en minúscula, y la clave de su texto. */
const UNIDADES_CONOCIDAS = new Map<string, ClaveTexto>(
  Object.entries(ETIQUETAS.es)
    .filter(([clave]) => clave.startsWith('etiquetas.unidadPrecio.'))
    .map(([clave, texto]) => [String(texto).toLowerCase(), clave as ClaveTexto]),
);

/**
 * La unidad del precio como llega de la API ("por persona") en el idioma vigente ("per person"). [SUPUESTO] La API la
 * entrega siempre en español (`unidadPrecio`, DC-22): si es una de las cuatro conocidas se traduce; si es otra, se
 * deja tal cual.
 */
export function unidadDePrecio(unidad: string | null | undefined): string {
  if (!unidad) {
    return '';
  }
  const clave = UNIDADES_CONOCIDAS.get(unidad.trim().toLowerCase());
  return clave ? traducir(clave) : unidad;
}

/** Qué tipos de oferta puede publicar cada tipo de prestador (DC-10). La API lo vuelve a verificar. */
export const TIPOS_QUE_PUBLICA: Record<TipoPrestador, readonly TipoOferta[]> = {
  AGENCIA_OPERADORA: ['ACTIVIDAD', 'PAQUETE'],
  GUIA_INDEPENDIENTE: ['ACTIVIDAD', 'PAQUETE'],
  ALOJAMIENTO: ['ALOJAMIENTO'],
  RESTAURANTE: ['RESTAURANTE'],
};

/** Estado de una opinión como lo ve su autor. */
export const ESTADO_OPINION = tablaDeTextos<EstadoOpinion>({
  EN_MODERACION: 'etiquetas.estadoOpinion.EN_MODERACION',
  APROBADA: 'etiquetas.estadoOpinion.APROBADA',
  RECHAZADA: 'etiquetas.estadoOpinion.RECHAZADA',
});

/** Qué significa cada calificación, para leerla junto a las estrellas. */
export const CALIFICACION = tablaDeTextos<number>({
  1: 'etiquetas.calificacion.1',
  2: 'etiquetas.calificacion.2',
  3: 'etiquetas.calificacion.3',
  4: 'etiquetas.calificacion.4',
  5: 'etiquetas.calificacion.5',
});
