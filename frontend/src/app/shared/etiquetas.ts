import {
  EstadoModeracion,
  ModalidadRecorrido,
  NivelEsfuerzo,
  TipoActividad,
  TipoOferta,
  TipoPrestador,
} from '../core/api/modelos';

/**
 * Textos que ve el usuario para los valores del contrato. Cada tabla exige todos los valores de su tipo:
 * si el contrato agrega uno, el build falla aquí hasta ponerle nombre.
 */
export const TIPO_OFERTA: Record<TipoOferta, string> = {
  ACTIVIDAD: 'Actividad',
  PAQUETE: 'Paquete',
  ALOJAMIENTO: 'Alojamiento',
  RESTAURANTE: 'Restaurante',
};

export const TIPO_ACTIVIDAD: Record<TipoActividad, string> = {
  SENDERISMO: 'Senderismo',
  BUCEO_SNORKEL: 'Buceo y snorkel',
  DEPORTES_ACUATICOS: 'Deportes acuáticos',
  NAUTICO: 'Náutico',
  AVISTAMIENTO_FAUNA: 'Avistamiento de fauna',
  CULTURAL_HISTORICO: 'Cultural e histórico',
  ETNOTURISMO: 'Etnoturismo',
  GASTRONOMICO: 'Gastronómico',
  CICLISMO: 'Ciclismo',
  AVENTURA: 'Aventura',
  PLAYA_RELAX: 'Playa y descanso',
  OTRO: 'Otro',
};

export const NIVEL_ESFUERZO: Record<NivelEsfuerzo, string> = {
  BAJO: 'Bajo',
  MODERADO: 'Moderado',
  ALTA_EXIGENCIA: 'Alta exigencia',
};

export const MODALIDAD: Record<ModalidadRecorrido, string> = {
  CAMINATA: 'Caminata',
  BICICLETA: 'Bicicleta',
  KAYAK: 'Kayak',
  OTRA: 'Otra',
};

export const ESTADO_MODERACION: Record<EstadoModeracion, string> = {
  EN_MODERACION: 'En moderación',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
};

export const TIPO_PRESTADOR: Record<TipoPrestador, string> = {
  AGENCIA_OPERADORA: 'Agencia operadora',
  GUIA_INDEPENDIENTE: 'Guía independiente',
  ALOJAMIENTO: 'Alojamiento',
  RESTAURANTE: 'Restaurante',
};

/**
 * Unidad del precio de referencia por tipo de oferta (DC-22).
 * [SUPUESTO] La búsqueda y la ficha la traen en `unidadPrecio`, pero la lista de ofertas propias no:
 * allí se toma de esta tabla.
 */
export const UNIDAD_PRECIO: Record<TipoOferta, string> = {
  ACTIVIDAD: 'por persona',
  PAQUETE: 'por persona',
  ALOJAMIENTO: 'por noche y unidad',
  RESTAURANTE: 'promedio por persona',
};

/** Qué tipos de oferta puede publicar cada tipo de prestador (DC-10). La API lo vuelve a verificar. */
export const TIPOS_QUE_PUBLICA: Record<TipoPrestador, readonly TipoOferta[]> = {
  AGENCIA_OPERADORA: ['ACTIVIDAD', 'PAQUETE'],
  GUIA_INDEPENDIENTE: ['ACTIVIDAD', 'PAQUETE'],
  ALOJAMIENTO: ['ALOJAMIENTO'],
  RESTAURANTE: ['RESTAURANTE'],
};
