import { diccionario } from './diccionario';

/**
 * Textos de las solicitudes de disponibilidad del turista (P02): el nombre de cada uno de los nueve estados, lo que
 * significa para quien la envió y quién hizo cada cambio del historial. Los textos de las pantallas se suman aquí.
 */
export const SOLICITUDES = diccionario(
  {
    'solicitudes.estado.PENDIENTE': 'Pendiente',
    'solicitudes.estado.ATENDIDA': 'Disponible, confirma',
    'solicitudes.estado.CONFIRMADA': 'Por pagar',
    'solicitudes.estado.NO_DISPONIBLE': 'No disponible',
    'solicitudes.estado.SIN_RESPUESTA': 'Sin respuesta',
    'solicitudes.estado.DECLINADA': 'Declinada',
    'solicitudes.estado.PAGO_FALLIDO': 'Pago fallido',
    'solicitudes.estado.PAGADA': 'Pagada',
    'solicitudes.estado.VENCIDA': 'Vencida',

    'solicitudes.estadoDesc.PENDIENTE':
      'Esperando que un operador verifique el cupo con el prestador.',
    'solicitudes.estadoDesc.ATENDIDA':
      'Hay cupo. Revisa las condiciones y confirma o declina antes del plazo.',
    'solicitudes.estadoDesc.CONFIRMADA':
      'Aceptaste las condiciones. Falta pagar para asegurar el cupo.',
    'solicitudes.estadoDesc.NO_DISPONIBLE': 'No hay cupo para la fecha pedida.',
    'solicitudes.estadoDesc.SIN_RESPUESTA': 'El prestador no respondió a tiempo.',
    'solicitudes.estadoDesc.DECLINADA': 'No aceptaste las condiciones.',
    'solicitudes.estadoDesc.PAGO_FALLIDO': 'El pago no se completó. Puedes intentarlo de nuevo.',
    'solicitudes.estadoDesc.PAGADA': 'Todo listo: está confirmada y pagada.',
    'solicitudes.estadoDesc.VENCIDA': 'Pasó el plazo para confirmar o pagar.',

    'solicitudes.actor.TURISTA': 'Tú',
    'solicitudes.actor.OPERADOR': 'Operador',
    'solicitudes.actor.SISTEMA': 'Sistema',
    'solicitudes.actor.PAGOS': 'Pagos',

    // Pasos del flujo (solicitar → confirmar → pagar → listo)
    'solicitudes.pasos.etiqueta': 'Pasos de la solicitud',
    'solicitudes.pasos.solicitar': 'Solicitar',
    'solicitudes.pasos.confirmar': 'Confirmar',
    'solicitudes.pasos.pagar': 'Pagar',
    'solicitudes.pasos.listo': 'Listo',
    'solicitudes.pasos.actual': '(paso actual)',
    'solicitudes.pasos.hecho': '(completado)',

    // Botón en la ficha de la oferta
    'solicitudes.solicitar': 'Solicitar disponibilidad',
    'solicitudes.pedirSesion': 'Te pediremos iniciar sesión.',
    'solicitudes.soloTuristas': 'Solo los turistas pueden solicitar disponibilidad.',

    // P02.1 Formulario de solicitud
    'solicitudes.form.titulo': 'Solicita disponibilidad',
    'solicitudes.form.intro':
      'Cuéntanos cuándo y cuántos. Un operador verificará el cupo con el prestador y te enviará las condiciones. No es una reserva y todavía no se paga nada.',
    'solicitudes.form.fechaDeseada': 'Fecha deseada',
    'solicitudes.form.fechaEntrada': 'Fecha de entrada',
    'solicitudes.form.fechaSalida': 'Fecha de salida',
    'solicitudes.form.ayudaFecha': 'Con 2 días de anticipación: desde el {fecha}.',
    'solicitudes.form.horaSalida': 'Hora de salida',
    'solicitudes.form.eligeHora': 'Elige una hora',
    'solicitudes.form.horaReserva': 'Hora de la reserva',
    'solicitudes.form.ayudaHorario': 'Horario del restaurante: {horarios}',
    'solicitudes.form.personas': 'Número de personas',
    'solicitudes.form.huespedes': 'Huéspedes',
    'solicitudes.form.ayudaPersonas': 'Hasta {maximo} personas.',
    'solicitudes.form.unidades': 'Número de unidades',
    'solicitudes.form.ayudaUnidades': 'Cuántas unidades necesitas (por ejemplo, habitaciones).',
    'solicitudes.form.nota': 'Comentarios para el prestador (opcional)',
    'solicitudes.form.ayudaNota':
      'Alergias, accesibilidad, grupo con niños… ({largo} de {maximo} caracteres)',
    'solicitudes.form.procesoTitulo': 'Así sigue el proceso',
    'solicitudes.form.procesoTexto':
      'Solicitud → el operador confirma el cupo → recibes las condiciones → confirmas y pagas en una pasarela segura. La comisión de la plataforma nunca se te suma: el precio es el acordado.',
    'solicitudes.form.enviar': 'Enviar solicitud',
    'solicitudes.form.enviando': 'Enviando…',
    'solicitudes.form.volver': 'Volver',
    'solicitudes.form.enviada':
      'Solicitud enviada. Un operador la revisará y te avisaremos por WhatsApp.',
    'solicitudes.form.whatsappTitulo': 'Verifica tu WhatsApp para enviar solicitudes',
    'solicitudes.form.whatsappTexto':
      'Te enviamos un código cuando te registraste. Es el único canal por el que te avisamos de tus solicitudes.',
    'solicitudes.form.whatsappEnlace': 'Verificar mi WhatsApp',
    'solicitudes.form.verMisSolicitudes': 'Ver mis solicitudes',
    'solicitudes.form.sinPrecio': 'Completa el formulario para ver el precio',

    // Resumen de la oferta en el formulario
    'solicitudes.resumen.precio': 'Precio',
    'solicitudes.resumen.personas': 'Personas',
    'solicitudes.resumen.noches': 'Noches',
    'solicitudes.resumen.unidades': 'Unidades',
    'solicitudes.resumen.fecha': 'Fecha',
    'solicitudes.resumen.referencia': 'Precio de referencia',
    'solicitudes.resumen.nota': 'El precio final lo confirma el operador.',

    // Errores de cada campo
    'solicitudes.error.fechaRequerido': 'Elige una fecha.',
    'solicitudes.error.fechaFormato': 'Escribe una fecha válida.',
    'solicitudes.error.fechaPasada':
      'La fecha debe ser desde el {fecha}: la solicitud se hace con 2 días de anticipación.',
    'solicitudes.error.salidaAntes': 'La salida debe ser después de la entrada.',
    'solicitudes.error.horaRequerida': 'Elige una hora.',
    'solicitudes.error.horaFormato': 'Escribe una hora válida, por ejemplo 19:30.',
    'solicitudes.error.horaNoOfrecida': 'Elige una de las horas de salida de la oferta.',
    'solicitudes.error.cantidadRequerida': 'Escribe un número.',
    'solicitudes.error.cantidadEntera': 'Escribe un número entero.',
    'solicitudes.error.cantidadMinima': 'Debe ser al menos 1.',
    'solicitudes.error.personasMaximo': 'Esta oferta admite hasta {maximo} personas.',
    'solicitudes.error.notaLarga': 'La nota puede tener hasta {maximo} caracteres.',

    // Plazos (cuánto queda para actuar)
    'solicitudes.plazo.dias': '{dias} d {horas} h',
    'solicitudes.plazo.horas': '{horas} h {minutos} min',
    'solicitudes.plazo.minutos': '{minutos} min',
    'solicitudes.plazo.pendiente': 'El operador tiene {tiempo} para responder.',
    'solicitudes.plazo.atendida': 'Te quedan {tiempo} para confirmar o declinar.',
    'solicitudes.plazo.pagar': 'Te quedan {tiempo} para pagar.',
    'solicitudes.plazo.vencido': 'El plazo terminó. Estamos actualizando el estado.',

    // P02.2 Mis solicitudes (lista)
    'solicitudes.lista.intro':
      'Sigue el estado de cada solicitud. Recuerda: una solicitud no es una reserva hasta que confirmas y pagas.',
    'solicitudes.lista.filtrar': 'Filtrar solicitudes',
    'solicitudes.lista.filtro.todas': 'Todas',
    'solicitudes.lista.filtro.activas': 'Activas',
    'solicitudes.lista.filtro.finalizadas': 'Finalizadas',
    'solicitudes.lista.cargando': 'Cargando tus solicitudes…',
    'solicitudes.lista.vacioTitulo': 'Todavía no has enviado solicitudes',
    'solicitudes.lista.vacioAyuda': 'Elige una oferta del catálogo y pide disponibilidad.',
    'solicitudes.lista.sinCoincidencias': 'No tienes solicitudes en este filtro.',
    'solicitudes.lista.otraPagina': 'Hay más solicitudes en otras páginas.',
    'solicitudes.lista.ref': 'Ref. {codigo}',
    'solicitudes.lista.personas': { uno: '{n} persona', otros: '{n} personas' },
    'solicitudes.lista.unidades': { uno: '{n} unidad', otros: '{n} unidades' },
    'solicitudes.lista.salida': 'salida {fecha}',
    'solicitudes.lista.verDetalle': 'Ver detalle',
    'solicitudes.lista.verCondiciones': 'Ver condiciones',
    'solicitudes.lista.volverASolicitar': 'Volver a solicitar',
    'solicitudes.lista.opinar': 'Dejar opinión',
    'solicitudes.lista.verOferta': 'Ver oferta',

    // P02.2 Detalle de la solicitud
    'solicitudes.detalle.cargando': 'Cargando la solicitud…',
    'solicitudes.detalle.noEncontrada': 'No encontramos esta solicitud',
    'solicitudes.detalle.noEncontradaAyuda':
      'Puede que no sea tuya o que ya no exista. Revisa la lista de tus solicitudes.',
    'solicitudes.detalle.datos': 'Tu solicitud',
    'solicitudes.detalle.nota': 'Tus comentarios',
    'solicitudes.detalle.enviada': 'Enviada',
    'solicitudes.detalle.actualizada': 'Última actualización',
    'solicitudes.detalle.referencia': 'Referencia',
    'solicitudes.detalle.condiciones': 'Condiciones',
    'solicitudes.detalle.precioBase': 'Total a pagar',
    'solicitudes.detalle.politica': 'Política de cancelación',
    'solicitudes.detalle.notaOperador': 'Nota del operador',
    'solicitudes.detalle.apartadoHasta': 'Cupo apartado hasta',
    'solicitudes.detalle.historial': 'Historial',
    'solicitudes.detalle.motivo': 'Motivo: {motivo}',
    'solicitudes.detalle.errorActualizar': 'No pudimos actualizar el estado.',

    // P02.3 Confirmar o declinar las condiciones
    'solicitudes.decision.titulo': 'Tu decisión',
    'solicitudes.decision.texto':
      'Si confirmas, la solicitud queda confirmada y podrás pagar dentro del plazo. Si declinas, el cupo queda libre y no se cobra nada.',
    'solicitudes.decision.hasta':
      'Tienes hasta el {fecha} para decidir. Después, la solicitud vence.',
    'solicitudes.decision.confirmar': 'Confirmar condiciones',
    'solicitudes.decision.confirmando': 'Confirmando…',
    'solicitudes.decision.declinar': 'Declinar',
    'solicitudes.decision.declinando': 'Declinando…',
    'solicitudes.decision.dialogoTitulo': '¿Declinar esta solicitud?',
    'solicitudes.decision.dialogoMensaje':
      'No se generará ningún cobro. El cupo quedará libre y podrás volver a solicitar la oferta más adelante.',
    'solicitudes.decision.confirmada': 'Confirmaste las condiciones.',
    'solicitudes.decision.declinada': 'Declinaste la solicitud. El cupo quedó libre.',
  },
  {
    'solicitudes.estado.PENDIENTE': 'Pending',
    'solicitudes.estado.ATENDIDA': 'Available, confirm',
    'solicitudes.estado.CONFIRMADA': 'Awaiting payment',
    'solicitudes.estado.NO_DISPONIBLE': 'Unavailable',
    'solicitudes.estado.SIN_RESPUESTA': 'No response',
    'solicitudes.estado.DECLINADA': 'Declined',
    'solicitudes.estado.PAGO_FALLIDO': 'Payment failed',
    'solicitudes.estado.PAGADA': 'Paid',
    'solicitudes.estado.VENCIDA': 'Expired',

    'solicitudes.estadoDesc.PENDIENTE':
      'Waiting for an operator to check availability with the provider.',
    'solicitudes.estadoDesc.ATENDIDA':
      'There is availability. Review the terms and confirm or decline before the deadline.',
    'solicitudes.estadoDesc.CONFIRMADA':
      'You accepted the terms. Payment is needed to secure your spot.',
    'solicitudes.estadoDesc.NO_DISPONIBLE': 'There is no availability for the requested date.',
    'solicitudes.estadoDesc.SIN_RESPUESTA': "The provider didn't respond in time.",
    'solicitudes.estadoDesc.DECLINADA': "You didn't accept the terms.",
    'solicitudes.estadoDesc.PAGO_FALLIDO': "The payment wasn't completed. You can try again.",
    'solicitudes.estadoDesc.PAGADA': 'All set: it is confirmed and paid.',
    'solicitudes.estadoDesc.VENCIDA': 'The deadline to confirm or pay has passed.',

    'solicitudes.actor.TURISTA': 'You',
    'solicitudes.actor.OPERADOR': 'Operator',
    'solicitudes.actor.SISTEMA': 'System',
    'solicitudes.actor.PAGOS': 'Payments',

    'solicitudes.pasos.etiqueta': 'Request steps',
    'solicitudes.pasos.solicitar': 'Request',
    'solicitudes.pasos.confirmar': 'Confirm',
    'solicitudes.pasos.pagar': 'Pay',
    'solicitudes.pasos.listo': 'Done',
    'solicitudes.pasos.actual': '(current step)',
    'solicitudes.pasos.hecho': '(completed)',

    'solicitudes.solicitar': 'Request availability',
    'solicitudes.pedirSesion': "We'll ask you to log in.",
    'solicitudes.soloTuristas': 'Only tourists can request availability.',

    'solicitudes.form.titulo': 'Request availability',
    'solicitudes.form.intro':
      'Tell us when and how many. An operator will check availability with the provider and send you the terms. This is not a booking and nothing is charged yet.',
    'solicitudes.form.fechaDeseada': 'Preferred date',
    'solicitudes.form.fechaEntrada': 'Check-in date',
    'solicitudes.form.fechaSalida': 'Check-out date',
    'solicitudes.form.ayudaFecha': 'With 2 days of notice: from {fecha}.',
    'solicitudes.form.horaSalida': 'Departure time',
    'solicitudes.form.eligeHora': 'Choose a time',
    'solicitudes.form.horaReserva': 'Reservation time',
    'solicitudes.form.ayudaHorario': 'Restaurant hours: {horarios}',
    'solicitudes.form.personas': 'Number of people',
    'solicitudes.form.huespedes': 'Guests',
    'solicitudes.form.ayudaPersonas': 'Up to {maximo} people.',
    'solicitudes.form.unidades': 'Number of units',
    'solicitudes.form.ayudaUnidades': 'How many units you need (for example, rooms).',
    'solicitudes.form.nota': 'Comments for the provider (optional)',
    'solicitudes.form.ayudaNota':
      'Allergies, accessibility, group with children… ({largo} of {maximo} characters)',
    'solicitudes.form.procesoTitulo': 'What happens next',
    'solicitudes.form.procesoTexto':
      'Request → the operator checks availability → you receive the terms → you confirm and pay through a secure gateway. The platform commission is never added to your price: the price is the agreed one.',
    'solicitudes.form.enviar': 'Send request',
    'solicitudes.form.enviando': 'Sending…',
    'solicitudes.form.volver': 'Back',
    'solicitudes.form.enviada':
      'Request sent. An operator will review it and we will notify you on WhatsApp.',
    'solicitudes.form.whatsappTitulo': 'Verify your WhatsApp to send requests',
    'solicitudes.form.whatsappTexto':
      'We sent you a code when you signed up. It is the only channel through which we notify you about your requests.',
    'solicitudes.form.whatsappEnlace': 'Verify my WhatsApp',
    'solicitudes.form.verMisSolicitudes': 'View my requests',
    'solicitudes.form.sinPrecio': 'Complete the form to see the price',

    'solicitudes.resumen.precio': 'Price',
    'solicitudes.resumen.personas': 'People',
    'solicitudes.resumen.noches': 'Nights',
    'solicitudes.resumen.unidades': 'Units',
    'solicitudes.resumen.fecha': 'Date',
    'solicitudes.resumen.referencia': 'Reference price',
    'solicitudes.resumen.nota': 'The operator confirms the final price.',

    'solicitudes.error.fechaRequerido': 'Choose a date.',
    'solicitudes.error.fechaFormato': 'Enter a valid date.',
    'solicitudes.error.fechaPasada':
      'The date must be {fecha} or later: requests need 2 days of notice.',
    'solicitudes.error.salidaAntes': 'Check-out must be after check-in.',
    'solicitudes.error.horaRequerida': 'Choose a time.',
    'solicitudes.error.horaFormato': 'Enter a valid time, for example 19:30.',
    'solicitudes.error.horaNoOfrecida': "Choose one of the offer's departure times.",
    'solicitudes.error.cantidadRequerida': 'Enter a number.',
    'solicitudes.error.cantidadEntera': 'Enter a whole number.',
    'solicitudes.error.cantidadMinima': 'Must be at least 1.',
    'solicitudes.error.personasMaximo': 'This offer allows up to {maximo} people.',
    'solicitudes.error.notaLarga': 'The note can be up to {maximo} characters long.',

    'solicitudes.plazo.dias': '{dias} d {horas} h',
    'solicitudes.plazo.horas': '{horas} h {minutos} min',
    'solicitudes.plazo.minutos': '{minutos} min',
    'solicitudes.plazo.pendiente': 'The operator has {tiempo} left to respond.',
    'solicitudes.plazo.atendida': 'You have {tiempo} left to confirm or decline.',
    'solicitudes.plazo.pagar': 'You have {tiempo} left to pay.',
    'solicitudes.plazo.vencido': 'The deadline has passed. We are updating the status.',

    'solicitudes.lista.intro':
      'Follow the status of each request. Remember: a request is not a booking until you confirm and pay.',
    'solicitudes.lista.filtrar': 'Filter requests',
    'solicitudes.lista.filtro.todas': 'All',
    'solicitudes.lista.filtro.activas': 'Active',
    'solicitudes.lista.filtro.finalizadas': 'Finished',
    'solicitudes.lista.cargando': 'Loading your requests…',
    'solicitudes.lista.vacioTitulo': "You haven't sent any requests yet",
    'solicitudes.lista.vacioAyuda': 'Pick an offer from the catalog and request availability.',
    'solicitudes.lista.sinCoincidencias': 'You have no requests in this filter.',
    'solicitudes.lista.otraPagina': 'There are more requests on other pages.',
    'solicitudes.lista.ref': 'Ref. {codigo}',
    'solicitudes.lista.personas': { uno: '{n} person', otros: '{n} people' },
    'solicitudes.lista.unidades': { uno: '{n} unit', otros: '{n} units' },
    'solicitudes.lista.salida': 'check-out {fecha}',
    'solicitudes.lista.verDetalle': 'View details',
    'solicitudes.lista.verCondiciones': 'View terms',
    'solicitudes.lista.volverASolicitar': 'Request again',
    'solicitudes.lista.opinar': 'Leave a review',
    'solicitudes.lista.verOferta': 'View offer',

    'solicitudes.detalle.cargando': 'Loading the request…',
    'solicitudes.detalle.noEncontrada': "We couldn't find this request",
    'solicitudes.detalle.noEncontradaAyuda':
      'It may not be yours, or it may no longer exist. Check your list of requests.',
    'solicitudes.detalle.datos': 'Your request',
    'solicitudes.detalle.nota': 'Your comments',
    'solicitudes.detalle.enviada': 'Sent',
    'solicitudes.detalle.actualizada': 'Last updated',
    'solicitudes.detalle.referencia': 'Reference',
    'solicitudes.detalle.condiciones': 'Terms',
    'solicitudes.detalle.precioBase': 'Total to pay',
    'solicitudes.detalle.politica': 'Cancellation policy',
    'solicitudes.detalle.notaOperador': 'Operator note',
    'solicitudes.detalle.apartadoHasta': 'Spot held until',
    'solicitudes.detalle.historial': 'History',
    'solicitudes.detalle.motivo': 'Reason: {motivo}',
    'solicitudes.detalle.errorActualizar': "We couldn't update the status.",

    'solicitudes.decision.titulo': 'Your decision',
    'solicitudes.decision.texto':
      'If you confirm, the request is confirmed and you will be able to pay within the deadline. If you decline, the spot is released and nothing is charged.',
    'solicitudes.decision.hasta':
      'You have until {fecha} to decide. After that, the request expires.',
    'solicitudes.decision.confirmar': 'Confirm terms',
    'solicitudes.decision.confirmando': 'Confirming…',
    'solicitudes.decision.declinar': 'Decline',
    'solicitudes.decision.declinando': 'Declining…',
    'solicitudes.decision.dialogoTitulo': 'Decline this request?',
    'solicitudes.decision.dialogoMensaje':
      'Nothing will be charged. The spot will be released and you can request the offer again later.',
    'solicitudes.decision.confirmada': 'You confirmed the terms.',
    'solicitudes.decision.declinada': 'You declined the request. The spot was released.',
  },
);
