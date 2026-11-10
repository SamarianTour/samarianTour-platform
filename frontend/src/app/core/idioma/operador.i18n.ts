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
  },
);
