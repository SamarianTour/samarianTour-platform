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
  },
);
