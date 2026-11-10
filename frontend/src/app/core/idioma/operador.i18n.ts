import { diccionario } from './diccionario';

/**
 * Textos del panel del operador de disponibilidad (P02.4, P02.5): su menú lateral, la cola, el historial, atender una
 * solicitud y sus avisos. Los textos de cada pantalla se suman aquí.
 */
export const OPERADOR = diccionario(
  {
    // Panel (menú lateral)
    'operador.panel.control': 'Panel del operador',
    'operador.panel.cola': 'Cola',
    'operador.panel.historial': 'Historial',
    'operador.panel.avisos': 'Avisos',

    // Tabla de solicitudes (la cola y el historial)
    'operador.tabla.captionCola': 'Solicitudes pendientes, las más urgentes primero',
    'operador.tabla.captionHistorial': 'Solicitudes tramitadas, las más recientes primero',
    'operador.tabla.urgencia': 'Urgencia',
    'operador.tabla.estado': 'Estado',
    'operador.tabla.oferta': 'Oferta',
    'operador.tabla.servicio': 'Fecha del servicio',
    'operador.tabla.cantidad': 'Cantidad',
    'operador.tabla.recibida': 'Recibida',
    'operador.tabla.plazo': 'Plazo',
    'operador.tabla.accion': 'Acción',
    'operador.tabla.restante': 'Vence en {tiempo}',
    'operador.tabla.plazoVencido': 'Plazo vencido',
    'operador.tabla.atender': 'Atender',
    'operador.tabla.atenderA': 'Atender la solicitud de {oferta}',
    'operador.tabla.ver': 'Ver',
    'operador.tabla.verA': 'Ver la solicitud de {oferta}',
    'operador.urgencia.urgente': 'Urgente',
    'operador.urgencia.enPlazo': 'En plazo',
    'operador.urgencia.vencida': 'Vencida',

    // P02.4 Cola
    'operador.cola.titulo': 'Cola de solicitudes',
    'operador.cola.intro':
      'Solicitudes de disponibilidad de tus prestadores que esperan que verifiques el cupo. Las que tienen 6 horas o menos para atenderse van primero y resaltadas.',
    'operador.cola.pendientes': 'Pendientes de tomar',
    'operador.cola.cargando': 'Cargando la cola…',
    'operador.cola.vacioTitulo': 'Todo al día',
    'operador.cola.vacioAyuda':
      'No hay solicitudes pendientes. Las nuevas aparecerán aquí y en tus avisos.',

    // Historial
    'operador.historial.titulo': 'Historial de solicitudes',
    'operador.historial.intro':
      'Las solicitudes que ya tramitaste, de la más reciente a la más antigua. Elige un estado para verlas.',
    'operador.historial.filtrar': 'Filtrar por estado',
    'operador.historial.total': { uno: '{n} solicitud', otros: '{n} solicitudes' },
    'operador.historial.cargando': 'Cargando el historial…',
    'operador.historial.vacioTitulo': 'Sin solicitudes en este estado',
    'operador.historial.vacioAyuda': 'Aquí aparecerán las solicitudes que lleguen a este estado.',

    // Avisos
    'operador.avisos.titulo': 'Avisos',
    'operador.avisos.intro':
      'Cada solicitud nueva que llega a tu cola deja un aviso aquí, del más reciente al más antiguo.',
    'operador.avisos.cargando': 'Cargando los avisos…',
    'operador.avisos.vacioTitulo': 'No tienes avisos',
    'operador.avisos.vacioAyuda': 'Cuando llegue una solicitud nueva, la verás aquí.',
    'operador.avisos.tipo.NUEVA_SOLICITUD': 'Nueva solicitud',
    'operador.avisos.tipo.otro': 'Aviso',
    'operador.avisos.recibido': 'Recibido {fecha}',

    // P02.5 Atender
    'operador.atender.intro':
      'Verifica el cupo con el prestador, por WhatsApp o por llamada, y registra aquí el resultado.',
    'operador.atender.volver': 'Volver a la cola',
    'operador.atender.cargando': 'Cargando la solicitud…',
    'operador.atender.noEncontradaTitulo': 'No encontramos esta solicitud',
    'operador.atender.noEncontradaAyuda': 'No existe o está asignada a otro operador.',

    'operador.atender.datos.titulo': 'Datos de la solicitud',
    'operador.atender.datos.oferta': 'Oferta',
    'operador.atender.datos.referencia': 'Referencia',
    'operador.atender.datos.fecha': 'Fecha pedida',
    'operador.atender.datos.entrada': 'Entrada',
    'operador.atender.datos.salida': 'Salida',
    'operador.atender.datos.horaSalida': 'Hora de salida',
    'operador.atender.datos.horaReserva': 'Hora de la reserva',
    'operador.atender.datos.personas': 'Personas',
    'operador.atender.datos.unidades': 'Unidades',
    'operador.atender.datos.noches': 'Noches',
    'operador.atender.datos.nota': 'Comentarios del turista',
    'operador.atender.datos.recibida': 'Recibida',

    'operador.atender.contacto.titulo': 'Contacta al prestador',
    'operador.atender.contacto.texto':
      'Pregúntale si tiene cupo para esta fecha y para cuántas personas. La conversación ocurre fuera de la plataforma.',
    'operador.atender.contacto.whatsapp': 'Escribir por WhatsApp',
    'operador.atender.contacto.nuevaPestana': '(se abre en otra pestaña)',
    'operador.atender.contacto.sinNumero':
      'Este prestador no tiene un WhatsApp registrado. Contáctalo por otro medio.',

    'operador.atender.plazo.restante': 'Tienes {tiempo} para atenderla.',
    'operador.atender.plazo.vencido':
      'El plazo para atenderla terminó; el sistema la cerrará en breve.',

    'operador.atender.form.titulo': 'Registrar el resultado',
    'operador.atender.form.resultado': 'Resultado de la verificación',
    'operador.atender.form.hayCupo': 'Hay cupo',
    'operador.atender.form.hayCupoAyuda':
      'El turista recibirá las condiciones para confirmar y pagar.',
    'operador.atender.form.noHayCupo': 'No hay cupo',
    'operador.atender.form.noHayCupoAyuda': 'Se avisará al turista; podrá volver a solicitar.',
    'operador.atender.form.sinTiempo':
      'No alcanza el tiempo para confirmar antes del servicio: solo puedes marcarla como no disponible.',
    'operador.atender.form.condiciones': 'Condiciones para el turista',
    'operador.atender.form.precioTotal': 'Precio total (COP)',
    'operador.atender.form.precioAyuda':
      'El total en pesos que pagará el turista, sin cargos adicionales. Escríbelo solo con números.',
    'operador.atender.form.politica': 'Política de cancelación',
    'operador.atender.form.politicaAyuda':
      'Por ejemplo: "Sin reembolso a menos de 24 horas del servicio".',
    'operador.atender.form.aparte':
      'Confirmé con el prestador que el cupo queda apartado durante el plazo',
    'operador.atender.form.notaCondiciones': 'Otras condiciones (opcional)',
    'operador.atender.form.notaCondicionesAyuda':
      'Punto de encuentro, qué incluye, recomendaciones… ({largo} de {maximo})',
    'operador.atender.form.notaMotivo': 'Motivo (opcional)',
    'operador.atender.form.notaMotivoAyuda':
      'Sin el detalle de la negociación con el prestador. ({largo} de {maximo})',
    'operador.atender.form.cancelar': 'Cancelar',
    'operador.atender.form.enviar': 'Enviar resultado',
    'operador.atender.form.enviando': 'Enviando…',

    'operador.atender.referencia.titulo': 'Referencia (solo informativa)',
    'operador.atender.referencia.cargando': 'Consultando el precio de referencia…',
    'operador.atender.referencia.precio': 'Precio en el catálogo: {precio}',
    'operador.atender.referencia.pedido': 'Pidieron: {cantidad}',
    'operador.atender.referencia.estimado': 'Cálculo de referencia: {total}',
    'operador.atender.referencia.aviso':
      'Solo orienta: el total lo escribes tú, según lo que acordaste con el prestador.',
    'operador.atender.referencia.noDisponible':
      'No pudimos consultar el precio de referencia de esta oferta. Puedes continuar sin él.',
    'operador.atender.referencia.noches': { uno: '{n} noche', otros: '{n} noches' },

    'operador.atender.error.precioRequerido': 'Escribe el precio total.',
    'operador.atender.error.precioEntero':
      'Escribe el precio como un número entero, sin puntos ni comas.',
    'operador.atender.error.precioMinimo': 'El precio debe ser mayor que cero.',
    'operador.atender.error.politicaRequerida': 'Escribe la política de cancelación.',
    'operador.atender.error.politicaLarga': 'La política admite hasta {maximo} caracteres.',
    'operador.atender.error.apartePendiente':
      'Confirma con el prestador que el cupo queda apartado antes de enviar.',
    'operador.atender.error.notaLarga': 'Este texto admite hasta {maximo} caracteres.',

    'operador.atender.exito.atendida':
      'Enviaste las condiciones. El turista podrá confirmarlas y pagar.',
    'operador.atender.exito.noDisponible': 'Registraste que no hay cupo. Avisamos al turista.',

    'operador.atender.tramitada.titulo': 'Esta solicitud ya fue tramitada',
    'operador.atender.tramitada.texto':
      'Su estado actual es "{estado}". Lo que registraste ya no se puede cambiar.',
    'operador.atender.registrado.titulo': 'Lo que registraste',
    'operador.atender.registrado.precio': 'Precio total',
    'operador.atender.registrado.politica': 'Política de cancelación',
    'operador.atender.registrado.nota': 'Nota',
    'operador.atender.registrado.aparteHasta': 'Cupo apartado hasta',

    'operador.atender.historial.titulo': 'Historial',
    'operador.atender.actor.TURISTA': 'Turista',
    'operador.atender.actor.OPERADOR': 'Operador',
    'operador.atender.actor.SISTEMA': 'Sistema',
    'operador.atender.actor.PAGOS': 'Pagos',
  },
  {
    'operador.panel.control': 'Operator panel',
    'operador.panel.cola': 'Queue',
    'operador.panel.historial': 'History',
    'operador.panel.avisos': 'Notices',

    'operador.tabla.captionCola': 'Pending requests, most urgent first',
    'operador.tabla.captionHistorial': 'Handled requests, most recent first',
    'operador.tabla.urgencia': 'Urgency',
    'operador.tabla.estado': 'Status',
    'operador.tabla.oferta': 'Offer',
    'operador.tabla.servicio': 'Service date',
    'operador.tabla.cantidad': 'Quantity',
    'operador.tabla.recibida': 'Received',
    'operador.tabla.plazo': 'Deadline',
    'operador.tabla.accion': 'Action',
    'operador.tabla.restante': 'Due in {tiempo}',
    'operador.tabla.plazoVencido': 'Deadline passed',
    'operador.tabla.atender': 'Handle',
    'operador.tabla.atenderA': 'Handle the request for {oferta}',
    'operador.tabla.ver': 'View',
    'operador.tabla.verA': 'View the request for {oferta}',
    'operador.urgencia.urgente': 'Urgent',
    'operador.urgencia.enPlazo': 'On time',
    'operador.urgencia.vencida': 'Overdue',

    'operador.cola.titulo': 'Request queue',
    'operador.cola.intro':
      'Availability requests from your providers waiting for you to check the spot. Those with 6 hours or less left to be handled come first, highlighted.',
    'operador.cola.pendientes': 'Waiting to be handled',
    'operador.cola.cargando': 'Loading the queue…',
    'operador.cola.vacioTitulo': 'All caught up',
    'operador.cola.vacioAyuda':
      'There are no pending requests. New ones will show up here and in your notices.',

    'operador.historial.titulo': 'Request history',
    'operador.historial.intro':
      'The requests you have already handled, from most recent to oldest. Pick a status to see them.',
    'operador.historial.filtrar': 'Filter by status',
    'operador.historial.total': { uno: '{n} request', otros: '{n} requests' },
    'operador.historial.cargando': 'Loading the history…',
    'operador.historial.vacioTitulo': 'No requests with this status',
    'operador.historial.vacioAyuda': 'Requests that reach this status will appear here.',

    'operador.avisos.titulo': 'Notices',
    'operador.avisos.intro':
      'Every new request that reaches your queue leaves a notice here, from most recent to oldest.',
    'operador.avisos.cargando': 'Loading the notices…',
    'operador.avisos.vacioTitulo': 'You have no notices',
    'operador.avisos.vacioAyuda': 'When a new request arrives, you will see it here.',
    'operador.avisos.tipo.NUEVA_SOLICITUD': 'New request',
    'operador.avisos.tipo.otro': 'Notice',
    'operador.avisos.recibido': 'Received {fecha}',

    'operador.atender.intro':
      'Check the spot with the provider, by WhatsApp or phone, and record the result here.',
    'operador.atender.volver': 'Back to the queue',
    'operador.atender.cargando': 'Loading the request…',
    'operador.atender.noEncontradaTitulo': "We couldn't find this request",
    'operador.atender.noEncontradaAyuda': 'It does not exist or it is assigned to another operator.',

    'operador.atender.datos.titulo': 'Request details',
    'operador.atender.datos.oferta': 'Offer',
    'operador.atender.datos.referencia': 'Reference',
    'operador.atender.datos.fecha': 'Requested date',
    'operador.atender.datos.entrada': 'Check-in',
    'operador.atender.datos.salida': 'Check-out',
    'operador.atender.datos.horaSalida': 'Departure time',
    'operador.atender.datos.horaReserva': 'Reservation time',
    'operador.atender.datos.personas': 'People',
    'operador.atender.datos.unidades': 'Units',
    'operador.atender.datos.noches': 'Nights',
    'operador.atender.datos.nota': "Tourist's comments",
    'operador.atender.datos.recibida': 'Received',

    'operador.atender.contacto.titulo': 'Contact the provider',
    'operador.atender.contacto.texto':
      'Ask whether they have room on this date and for how many people. The conversation happens outside the platform.',
    'operador.atender.contacto.whatsapp': 'Message on WhatsApp',
    'operador.atender.contacto.nuevaPestana': '(opens in a new tab)',
    'operador.atender.contacto.sinNumero':
      'This provider has no WhatsApp number registered. Contact them another way.',

    'operador.atender.plazo.restante': 'You have {tiempo} left to handle it.',
    'operador.atender.plazo.vencido':
      'The deadline to handle it has passed; the system will close it shortly.',

    'operador.atender.form.titulo': 'Record the result',
    'operador.atender.form.resultado': 'Result of the check',
    'operador.atender.form.hayCupo': 'Spot available',
    'operador.atender.form.hayCupoAyuda':
      'The tourist will receive the terms to confirm and pay.',
    'operador.atender.form.noHayCupo': 'No spot available',
    'operador.atender.form.noHayCupoAyuda':
      'The tourist will be notified and can request again.',
    'operador.atender.form.sinTiempo':
      'There is not enough time to confirm before the service: you can only mark it as unavailable.',
    'operador.atender.form.condiciones': 'Terms for the tourist',
    'operador.atender.form.precioTotal': 'Total price (COP)',
    'operador.atender.form.precioAyuda':
      'The total in pesos the tourist will pay, with no extra charges. Type numbers only.',
    'operador.atender.form.politica': 'Cancellation policy',
    'operador.atender.form.politicaAyuda':
      'For example: "No refund within 24 hours of the service".',
    'operador.atender.form.aparte':
      'I confirmed with the provider that the spot is held during the deadline',
    'operador.atender.form.notaCondiciones': 'Other terms (optional)',
    'operador.atender.form.notaCondicionesAyuda':
      'Meeting point, what is included, tips… ({largo} of {maximo})',
    'operador.atender.form.notaMotivo': 'Reason (optional)',
    'operador.atender.form.notaMotivoAyuda':
      'Leave out the details of the negotiation with the provider. ({largo} of {maximo})',
    'operador.atender.form.cancelar': 'Cancel',
    'operador.atender.form.enviar': 'Send result',
    'operador.atender.form.enviando': 'Sending…',

    'operador.atender.referencia.titulo': 'Reference (informational only)',
    'operador.atender.referencia.cargando': 'Looking up the reference price…',
    'operador.atender.referencia.precio': 'Catalog price: {precio}',
    'operador.atender.referencia.pedido': 'Requested: {cantidad}',
    'operador.atender.referencia.estimado': 'Reference estimate: {total}',
    'operador.atender.referencia.aviso':
      'For guidance only: you enter the total, based on what you agreed with the provider.',
    'operador.atender.referencia.noDisponible':
      "We couldn't load the reference price of this offer. You can continue without it.",
    'operador.atender.referencia.noches': { uno: '{n} night', otros: '{n} nights' },

    'operador.atender.error.precioRequerido': 'Enter the total price.',
    'operador.atender.error.precioEntero':
      'Enter the price as a whole number, without dots or commas.',
    'operador.atender.error.precioMinimo': 'The price must be greater than zero.',
    'operador.atender.error.politicaRequerida': 'Enter the cancellation policy.',
    'operador.atender.error.politicaLarga': 'The policy allows up to {maximo} characters.',
    'operador.atender.error.apartePendiente':
      'Confirm with the provider that the spot is held before sending.',
    'operador.atender.error.notaLarga': 'This text allows up to {maximo} characters.',

    'operador.atender.exito.atendida':
      'You sent the terms. The tourist can now confirm them and pay.',
    'operador.atender.exito.noDisponible':
      'You recorded that there is no spot. We notified the tourist.',

    'operador.atender.tramitada.titulo': 'This request has already been handled',
    'operador.atender.tramitada.texto':
      'Its current status is "{estado}". What you recorded can no longer be changed.',
    'operador.atender.registrado.titulo': 'What you recorded',
    'operador.atender.registrado.precio': 'Total price',
    'operador.atender.registrado.politica': 'Cancellation policy',
    'operador.atender.registrado.nota': 'Note',
    'operador.atender.registrado.aparteHasta': 'Spot held until',

    'operador.atender.historial.titulo': 'History',
    'operador.atender.actor.TURISTA': 'Tourist',
    'operador.atender.actor.OPERADOR': 'Operator',
    'operador.atender.actor.SISTEMA': 'System',
    'operador.atender.actor.PAGOS': 'Payments',
  },
);
