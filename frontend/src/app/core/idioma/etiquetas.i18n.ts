import { diccionario } from './diccionario';

/**
 * Los nombres de los valores de los contratos (roles, tipos de oferta, estados…): `etiquetas.<tabla>.<VALOR>`. Las
 * tablas que los usan están en `shared/etiquetas.ts`. Los términos siguen el glosario.
 */
export const ETIQUETAS = diccionario(
  {
    'etiquetas.rol.TURISTA': 'Turista',
    'etiquetas.rol.PRESTADOR': 'Prestador',
    'etiquetas.rol.ADMINISTRADOR': 'Administrador',
    'etiquetas.rol.OPERADOR_DISPONIBILIDAD': 'Operador de disponibilidad',
    'etiquetas.rol.GESTOR_DESTINO': 'Gestor del destino',

    'etiquetas.tipoOferta.ACTIVIDAD': 'Actividad',
    'etiquetas.tipoOferta.PAQUETE': 'Paquete',
    'etiquetas.tipoOferta.ALOJAMIENTO': 'Alojamiento',
    'etiquetas.tipoOferta.RESTAURANTE': 'Restaurante',

    'etiquetas.tipoActividad.SENDERISMO': 'Senderismo',
    'etiquetas.tipoActividad.BUCEO_SNORKEL': 'Buceo y snorkel',
    'etiquetas.tipoActividad.DEPORTES_ACUATICOS': 'Deportes acuáticos',
    'etiquetas.tipoActividad.NAUTICO': 'Náutico',
    'etiquetas.tipoActividad.AVISTAMIENTO_FAUNA': 'Avistamiento de fauna',
    'etiquetas.tipoActividad.CULTURAL_HISTORICO': 'Cultural e histórico',
    'etiquetas.tipoActividad.ETNOTURISMO': 'Etnoturismo',
    'etiquetas.tipoActividad.GASTRONOMICO': 'Gastronómico',
    'etiquetas.tipoActividad.CICLISMO': 'Ciclismo',
    'etiquetas.tipoActividad.AVENTURA': 'Aventura',
    'etiquetas.tipoActividad.PLAYA_RELAX': 'Playa y descanso',
    'etiquetas.tipoActividad.OTRO': 'Otro',

    'etiquetas.nivelEsfuerzo.BAJO': 'Bajo',
    'etiquetas.nivelEsfuerzo.MODERADO': 'Moderado',
    'etiquetas.nivelEsfuerzo.ALTA_EXIGENCIA': 'Alta exigencia',

    'etiquetas.modalidad.CAMINATA': 'Caminata',
    'etiquetas.modalidad.BICICLETA': 'Bicicleta',
    'etiquetas.modalidad.KAYAK': 'Kayak',
    'etiquetas.modalidad.OTRA': 'Otra',

    'etiquetas.estadoModeracion.EN_MODERACION': 'En moderación',
    'etiquetas.estadoModeracion.APROBADA': 'Aprobada',
    'etiquetas.estadoModeracion.RECHAZADA': 'Rechazada',

    'etiquetas.tipoPrestador.AGENCIA_OPERADORA': 'Agencia operadora',
    'etiquetas.tipoPrestador.GUIA_INDEPENDIENTE': 'Guía independiente',
    'etiquetas.tipoPrestador.ALOJAMIENTO': 'Alojamiento',
    'etiquetas.tipoPrestador.RESTAURANTE': 'Restaurante',

    'etiquetas.unidadPrecio.ACTIVIDAD': 'por persona',
    'etiquetas.unidadPrecio.PAQUETE': 'por persona',
    'etiquetas.unidadPrecio.ALOJAMIENTO': 'por noche y unidad',
    'etiquetas.unidadPrecio.RESTAURANTE': 'promedio por persona',

    'etiquetas.estadoOpinion.EN_MODERACION': 'En moderación',
    'etiquetas.estadoOpinion.APROBADA': 'Publicada',
    'etiquetas.estadoOpinion.RECHAZADA': 'Rechazada',

    'etiquetas.calificacion.1': 'Malo',
    'etiquetas.calificacion.2': 'Regular',
    'etiquetas.calificacion.3': 'Bueno',
    'etiquetas.calificacion.4': 'Muy bueno',
    'etiquetas.calificacion.5': 'Excelente',

    'etiquetas.estadoOferta.PUBLICADA': 'Publicada',
    'etiquetas.estadoOferta.EN_MODERACION': 'En moderación',
    'etiquetas.estadoOferta.RECHAZADA': 'Rechazada',
    'etiquetas.estadoOferta.PUBLICADA_CON_CAMBIOS': 'Publicada con cambios en revisión',
    'etiquetas.estadoOferta.RETIRADA': 'Retirada',
  },
  {
    'etiquetas.rol.TURISTA': 'Tourist',
    'etiquetas.rol.PRESTADOR': 'Provider',
    'etiquetas.rol.ADMINISTRADOR': 'Administrator',
    'etiquetas.rol.OPERADOR_DISPONIBILIDAD': 'Availability operator',
    'etiquetas.rol.GESTOR_DESTINO': 'Destination manager',

    'etiquetas.tipoOferta.ACTIVIDAD': 'Activity',
    'etiquetas.tipoOferta.PAQUETE': 'Package',
    'etiquetas.tipoOferta.ALOJAMIENTO': 'Accommodation',
    'etiquetas.tipoOferta.RESTAURANTE': 'Restaurant',

    'etiquetas.tipoActividad.SENDERISMO': 'Hiking',
    'etiquetas.tipoActividad.BUCEO_SNORKEL': 'Diving and snorkeling',
    'etiquetas.tipoActividad.DEPORTES_ACUATICOS': 'Water sports',
    'etiquetas.tipoActividad.NAUTICO': 'Boating',
    'etiquetas.tipoActividad.AVISTAMIENTO_FAUNA': 'Wildlife watching',
    'etiquetas.tipoActividad.CULTURAL_HISTORICO': 'Cultural and historical',
    'etiquetas.tipoActividad.ETNOTURISMO': 'Ethnotourism',
    'etiquetas.tipoActividad.GASTRONOMICO': 'Culinary',
    'etiquetas.tipoActividad.CICLISMO': 'Cycling',
    'etiquetas.tipoActividad.AVENTURA': 'Adventure',
    'etiquetas.tipoActividad.PLAYA_RELAX': 'Beach and relaxation',
    'etiquetas.tipoActividad.OTRO': 'Other',

    'etiquetas.nivelEsfuerzo.BAJO': 'Low',
    'etiquetas.nivelEsfuerzo.MODERADO': 'Moderate',
    'etiquetas.nivelEsfuerzo.ALTA_EXIGENCIA': 'Demanding',

    'etiquetas.modalidad.CAMINATA': 'Walking',
    'etiquetas.modalidad.BICICLETA': 'Bicycle',
    'etiquetas.modalidad.KAYAK': 'Kayak',
    'etiquetas.modalidad.OTRA': 'Other',

    'etiquetas.estadoModeracion.EN_MODERACION': 'In moderation',
    'etiquetas.estadoModeracion.APROBADA': 'Approved',
    'etiquetas.estadoModeracion.RECHAZADA': 'Rejected',

    'etiquetas.tipoPrestador.AGENCIA_OPERADORA': 'Tour operator agency',
    'etiquetas.tipoPrestador.GUIA_INDEPENDIENTE': 'Independent guide',
    'etiquetas.tipoPrestador.ALOJAMIENTO': 'Accommodation',
    'etiquetas.tipoPrestador.RESTAURANTE': 'Restaurant',

    'etiquetas.unidadPrecio.ACTIVIDAD': 'per person',
    'etiquetas.unidadPrecio.PAQUETE': 'per person',
    'etiquetas.unidadPrecio.ALOJAMIENTO': 'per night and unit',
    'etiquetas.unidadPrecio.RESTAURANTE': 'average per person',

    'etiquetas.estadoOpinion.EN_MODERACION': 'In moderation',
    'etiquetas.estadoOpinion.APROBADA': 'Published',
    'etiquetas.estadoOpinion.RECHAZADA': 'Rejected',

    'etiquetas.calificacion.1': 'Poor',
    'etiquetas.calificacion.2': 'Fair',
    'etiquetas.calificacion.3': 'Good',
    'etiquetas.calificacion.4': 'Very good',
    'etiquetas.calificacion.5': 'Excellent',

    'etiquetas.estadoOferta.PUBLICADA': 'Published',
    'etiquetas.estadoOferta.EN_MODERACION': 'In moderation',
    'etiquetas.estadoOferta.RECHAZADA': 'Rejected',
    'etiquetas.estadoOferta.PUBLICADA_CON_CAMBIOS': 'Published, changes under review',
    'etiquetas.estadoOferta.RETIRADA': 'Withdrawn',
  },
);
