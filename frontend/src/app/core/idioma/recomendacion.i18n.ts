import { diccionario } from './diccionario';

/**
 * Textos de la recomendación (P08): el asistente de viaje con sus recomendaciones explicadas (P08.1), las alertas del destino
 * (P08.2) y las alternativas a una oferta de una zona con alta afluencia (P08.3). Los nombres de las pantallas están en `app.i18n.ts`
 * (`app.titulo.*`). Las explicaciones, los títulos y las descripciones de las alertas los escribe el servicio en el idioma pedido y se
 * muestran tal cual.
 */
export const RECOMENDACION = diccionario(
  {
    // Asistente de viaje (P08.1)
    'recomendacion.chat.titulo': 'Asistente de viaje',
    'recomendacion.chat.subtitulo': 'Te sugiere ofertas verificadas según lo que buscas.',
    'recomendacion.chat.conversacion': 'Conversación',
    'recomendacion.chat.bienvenida':
      '¡Hola! Cuéntame qué te gustaría vivir en Santa Marta y te sugiero de 3 a 5 ofertas verificadas, explicando por qué. Puedes tocar una idea o escribir lo que buscas.',
    'recomendacion.chat.ideas': 'Ideas para empezar',
    'recomendacion.idea.mar': 'Planes de bajo esfuerzo cerca del mar',
    'recomendacion.idea.minca': 'Qué hacer en Minca en 1 día',
    'recomendacion.idea.ninos': 'Opciones para ir con niños',
    'recomendacion.idea.buceo': 'Experiencias de buceo',
    'recomendacion.mensaje.tu': 'Tú',
    'recomendacion.mensaje.asistente': 'Asistente',
    'recomendacion.alertas.enlace': 'Ver alertas del destino',

    // Alertas del destino (P08.2)
    'recomendacion.alertas.titulo': 'Alertas del destino',
    'recomendacion.alertas.subtitulo': 'Condiciones activas en cada zona, con quién las emite y cuándo se actualizaron.',
    'recomendacion.alertas.cargando': 'Consultando las alertas…',
    'recomendacion.alertas.actualizado': 'Actualizado:',
    'recomendacion.alertas.total': { uno: '{n} alerta activa', otros: '{n} alertas activas' },
    'recomendacion.alertas.zonaSinNombre': 'Zona sin nombre',
    'recomendacion.alertas.sin': 'Sin alertas activas',
    'recomendacion.alertas.sinAyuda': 'Ahora mismo no hay avisos vigentes en ninguna zona.',
    'recomendacion.alertas.pie':
      'Las alertas se consultan a su fuente cada pocas horas. Antes de salir, confirma con las autoridades locales. Las marcadas "(simulado)" son de demostración.',
    'recomendacion.alertas.volver': 'Volver a recomendaciones',
    'recomendacion.alertas.noDisponible.servicio':
      'Las alertas del destino no están disponibles en este momento.',
    'recomendacion.alertas.noDisponible.tiempo': 'Las alertas están tardando más de lo normal y dejamos de esperar.',
    'recomendacion.alertas.noDisponible.conexion': 'Parece que no hay conexión a internet.',
    'recomendacion.alertas.noDisponible.ayuda':
      'Puedes intentarlo de nuevo. El resto de la plataforma sigue funcionando.',
    'recomendacion.alertasZona.sinAyuda': 'No hay avisos vigentes para esta zona.',
    'recomendacion.alertasZona.verTodas': 'Ver las alertas de todas las zonas',
    'recomendacion.alerta.nivel': 'Nivel',
    'recomendacion.alerta.nivel.AMARILLA': 'Amarilla',
    'recomendacion.alerta.nivel.NARANJA': 'Naranja',
    'recomendacion.alerta.nivel.ROJA': 'Roja',
    'recomendacion.alerta.tipo.HIDROLOGICA': 'Hidrológica',
    'recomendacion.alerta.tipo.LLUVIAS': 'Lluvias',
    'recomendacion.alerta.tipo.DESLIZAMIENTO': 'Deslizamiento',
    'recomendacion.alerta.tipo.OLEAJE': 'Oleaje',
    'recomendacion.alerta.tipo.CIERRE': 'Cierre',
    'recomendacion.alerta.tipo.OTRA': 'Otra',
    'recomendacion.alerta.fuente': 'Fuente:',
    'recomendacion.alerta.emitida': 'Emitida:',
    'recomendacion.alerta.vigenteHasta': 'Vigente hasta:',

    // El formulario
    'recomendacion.form.etiqueta': 'Escribe lo que buscas',
    'recomendacion.form.ejemplo': 'Ej. Quiero ver aves en la montaña, sin caminar mucho',
    'recomendacion.form.enviar': 'Enviar',
    'recomendacion.error.vacia': 'Escribe lo que buscas o elige al menos un filtro.',
    'recomendacion.error.textoLargo': 'El texto puede tener hasta 500 caracteres.',
    'recomendacion.error.precioInvalido': 'El precio máximo debe ser mayor que cero.',

    // Los filtros
    'recomendacion.filtro.titulo': 'Afinar la búsqueda',
    'recomendacion.filtro.ayuda':
      'La accesibilidad, el esfuerzo máximo y el precio máximo nunca se relajan: una oferta que no los cumple no se recomienda. El tipo de oferta, el tipo de actividad y la zona dan prioridad.',
    'recomendacion.filtro.tipoOferta': 'Tipo de oferta',
    'recomendacion.filtro.tipoActividad': 'Tipo de actividad',
    'recomendacion.filtro.zona': 'Zona',
    'recomendacion.filtro.cualquiera': 'Cualquiera',
    'recomendacion.filtro.accesible': 'Accesible en silla de ruedas',
    'recomendacion.filtro.esfuerzo': 'Esfuerzo máximo',
    'recomendacion.filtro.precio': 'Precio máximo (pesos)',
    'recomendacion.filtro.quitar': 'Quitar los filtros',
    'recomendacion.filtro.activos': 'Con filtros elegidos',
    'recomendacion.filtro.esfuerzoHasta': 'esfuerzo hasta {nivel}',
    'recomendacion.filtro.precioHasta': 'hasta {precio}',

    // La respuesta
    'recomendacion.buscando': 'Buscando opciones…',
    'recomendacion.buscandoAyuda': 'Puede tardar hasta unos 10 segundos.',
    'recomendacion.respuesta.intro': 'Estas ofertas verificadas pueden encajar. Son sugerencias: tú decides.',
    'recomendacion.respuesta.motor.REGLAS': 'Elegidas con criterios explícitos que puedes ver en cada oferta.',
    'recomendacion.respuesta.motor.IA':
      'Elegidas con ayuda de inteligencia artificial. Cada oferta dice en qué criterios coincide.',
    'recomendacion.respuesta.pocas':
      'Encontramos pocas ofertas que cumplan todo lo que pediste. La accesibilidad, el esfuerzo y el precio no se relajan: prueba quitando alguno de esos filtros para ver más.',
    'recomendacion.respuesta.ninguna':
      'No encontramos ofertas que cumplan lo que pediste. Prueba con otras palabras o quita algún filtro.',
    'recomendacion.respuesta.catalogo': 'Explorar el catálogo',
    'recomendacion.noDisponible.servicio':
      'Ahora mismo no puedo generar sugerencias: el servicio de recomendaciones no está disponible.',
    'recomendacion.noDisponible.tiempo': 'Está tardando más de lo normal y dejé de esperar.',
    'recomendacion.noDisponible.conexion': 'Parece que no hay conexión a internet.',
    'recomendacion.noDisponible.ayuda':
      'Puedes intentarlo de nuevo, o explorar el catálogo y filtrar por zona o tipo de oferta.',
    'recomendacion.noDisponible.reintentar': 'Intentar de nuevo',

    // La tarjeta de una oferta recomendada
    'recomendacion.tarjeta.posicion': 'Opción {n}',
    'recomendacion.tarjeta.criterios': 'Criterios que cumple',
    'recomendacion.criterio.TIPO_OFERTA': 'Tipo de oferta',
    'recomendacion.criterio.TIPO_ACTIVIDAD': 'Tipo de actividad',
    'recomendacion.criterio.ZONA': 'Zona',
    'recomendacion.criterio.ACCESIBILIDAD': 'Accesible',
    'recomendacion.criterio.NIVEL_ESFUERZO': 'Esfuerzo',
    'recomendacion.criterio.PRECIO': 'Precio',

    // Avisos al pie
    'recomendacion.aviso.sugerencias':
      'Son sugerencias automáticas, no publicidad. No hay ofertas patrocinadas ni destacadas: tú decides.',
    'recomendacion.aviso.privacidad':
      'No escribas datos personales (nombre, teléfono, correo). La conversación solo vive en esta página: si la recargas, se borra.',
  },
  {
    // Travel assistant (P08.1)
    'recomendacion.chat.titulo': 'Travel assistant',
    'recomendacion.chat.subtitulo': 'Suggests verified offers based on what you are looking for.',
    'recomendacion.chat.conversacion': 'Conversation',
    'recomendacion.chat.bienvenida':
      "Hi! Tell me what you would like to do in Santa Marta and I'll suggest 3 to 5 verified offers, explaining why. You can tap an idea or type what you are looking for.",
    'recomendacion.chat.ideas': 'Ideas to get started',
    'recomendacion.idea.mar': 'Low-effort plans near the sea',
    'recomendacion.idea.minca': 'What to do in Minca in 1 day',
    'recomendacion.idea.ninos': 'Options for going with kids',
    'recomendacion.idea.buceo': 'Diving experiences',
    'recomendacion.mensaje.tu': 'You',
    'recomendacion.mensaje.asistente': 'Assistant',
    'recomendacion.alertas.enlace': 'See destination alerts',

    // Destination alerts (P08.2)
    'recomendacion.alertas.titulo': 'Destination alerts',
    'recomendacion.alertas.subtitulo': 'Active conditions in each area, who issues them and when they were last updated.',
    'recomendacion.alertas.cargando': 'Checking the alerts…',
    'recomendacion.alertas.actualizado': 'Last updated:',
    'recomendacion.alertas.total': { uno: '{n} active alert', otros: '{n} active alerts' },
    'recomendacion.alertas.zonaSinNombre': 'Unnamed area',
    'recomendacion.alertas.sin': 'No active alerts',
    'recomendacion.alertas.sinAyuda': 'Right now there are no active notices in any area.',
    'recomendacion.alertas.pie':
      'Alerts are checked against their source every few hours. Before heading out, confirm with the local authorities. Those marked "(simulado)" are for demonstration.',
    'recomendacion.alertas.volver': 'Back to recommendations',
    'recomendacion.alertas.noDisponible.servicio': 'Destination alerts are not available right now.',
    'recomendacion.alertas.noDisponible.tiempo': 'The alerts are taking longer than usual, so we stopped waiting.',
    'recomendacion.alertas.noDisponible.conexion': 'You seem to be offline.',
    'recomendacion.alertas.noDisponible.ayuda': 'You can try again. The rest of the platform keeps working.',
    'recomendacion.alertasZona.sinAyuda': 'There are no active notices for this area.',
    'recomendacion.alertasZona.verTodas': 'See alerts for all areas',
    'recomendacion.alerta.nivel': 'Level',
    'recomendacion.alerta.nivel.AMARILLA': 'Yellow',
    'recomendacion.alerta.nivel.NARANJA': 'Orange',
    'recomendacion.alerta.nivel.ROJA': 'Red',
    'recomendacion.alerta.tipo.HIDROLOGICA': 'Hydrological',
    'recomendacion.alerta.tipo.LLUVIAS': 'Rain',
    'recomendacion.alerta.tipo.DESLIZAMIENTO': 'Landslide',
    'recomendacion.alerta.tipo.OLEAJE': 'Heavy swell',
    'recomendacion.alerta.tipo.CIERRE': 'Closure',
    'recomendacion.alerta.tipo.OTRA': 'Other',
    'recomendacion.alerta.fuente': 'Source:',
    'recomendacion.alerta.emitida': 'Issued:',
    'recomendacion.alerta.vigenteHasta': 'Valid until:',

    // The form
    'recomendacion.form.etiqueta': 'Type what you are looking for',
    'recomendacion.form.ejemplo': 'E.g. I want to see birds in the mountains, without walking much',
    'recomendacion.form.enviar': 'Send',
    'recomendacion.error.vacia': 'Type what you are looking for or choose at least one filter.',
    'recomendacion.error.textoLargo': 'The text can have up to 500 characters.',
    'recomendacion.error.precioInvalido': 'The maximum price must be greater than zero.',

    // The filters
    'recomendacion.filtro.titulo': 'Refine the search',
    'recomendacion.filtro.ayuda':
      'Accessibility, maximum effort and maximum price are never relaxed: an offer that does not meet them is not recommended. Offer type, activity type and area give priority.',
    'recomendacion.filtro.tipoOferta': 'Offer type',
    'recomendacion.filtro.tipoActividad': 'Activity type',
    'recomendacion.filtro.zona': 'Area',
    'recomendacion.filtro.cualquiera': 'Any',
    'recomendacion.filtro.accesible': 'Wheelchair accessible',
    'recomendacion.filtro.esfuerzo': 'Maximum effort',
    'recomendacion.filtro.precio': 'Maximum price (COP)',
    'recomendacion.filtro.quitar': 'Clear the filters',
    'recomendacion.filtro.activos': 'Filters chosen',
    'recomendacion.filtro.esfuerzoHasta': 'effort up to {nivel}',
    'recomendacion.filtro.precioHasta': 'up to {precio}',

    // The answer
    'recomendacion.buscando': 'Looking for options…',
    'recomendacion.buscandoAyuda': 'It can take up to about 10 seconds.',
    'recomendacion.respuesta.intro': 'These verified offers may be a good fit. They are suggestions: you decide.',
    'recomendacion.respuesta.motor.REGLAS': 'Chosen with explicit criteria you can see on each offer.',
    'recomendacion.respuesta.motor.IA':
      'Chosen with the help of artificial intelligence. Each offer says which criteria it matches.',
    'recomendacion.respuesta.pocas':
      'We found few offers that meet everything you asked for. Accessibility, effort and price are never relaxed: try removing one of those filters to see more.',
    'recomendacion.respuesta.ninguna':
      'We found no offers that meet what you asked for. Try other words or remove a filter.',
    'recomendacion.respuesta.catalogo': 'Browse the catalog',
    'recomendacion.noDisponible.servicio':
      'Right now I cannot generate suggestions: the recommendation service is not available.',
    'recomendacion.noDisponible.tiempo': 'It is taking longer than usual, so I stopped waiting.',
    'recomendacion.noDisponible.conexion': 'You seem to be offline.',
    'recomendacion.noDisponible.ayuda':
      'You can try again, or browse the catalog and filter by area or offer type.',
    'recomendacion.noDisponible.reintentar': 'Try again',

    // The card of a recommended offer
    'recomendacion.tarjeta.posicion': 'Option {n}',
    'recomendacion.tarjeta.criterios': 'Criteria it meets',
    'recomendacion.criterio.TIPO_OFERTA': 'Offer type',
    'recomendacion.criterio.TIPO_ACTIVIDAD': 'Activity type',
    'recomendacion.criterio.ZONA': 'Area',
    'recomendacion.criterio.ACCESIBILIDAD': 'Accessible',
    'recomendacion.criterio.NIVEL_ESFUERZO': 'Effort',
    'recomendacion.criterio.PRECIO': 'Price',

    // Notes at the bottom
    'recomendacion.aviso.sugerencias':
      'These are automatic suggestions, not advertising. There are no sponsored or featured offers: you decide.',
    'recomendacion.aviso.privacidad':
      'Do not type personal data (name, phone, email). The conversation only lives on this page: if you reload it, it is erased.',
  },
);
