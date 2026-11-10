import { diccionario } from './diccionario';

/**
 * Textos del pago de una solicitud (P04.1, P04.2): el estado de cada intento y por qué terminó así, y cuántos intentos le
 * quedan al turista. El pago ocurre solo en la pasarela externa: aquí no hay textos para pedir datos de tarjeta. Los textos
 * de las pantallas se suman aquí.
 */
export const PAGOS = diccionario(
  {
    // Estado de cada intento (EstadoIntento) y qué significa para el turista
    'pagos.intento.estado.EN_CURSO': 'En curso',
    'pagos.intento.estado.APROBADO': 'Aprobado',
    'pagos.intento.estado.RECHAZADO': 'Rechazado',
    'pagos.intento.estado.ERROR': 'Con error',
    'pagos.intento.estado.ABANDONADO': 'Abandonado',
    'pagos.intento.motivo.EN_CURSO': 'Esperando la confirmación de la pasarela.',
    'pagos.intento.motivo.APROBADO': 'La pasarela aprobó el pago.',
    'pagos.intento.motivo.RECHAZADO': 'La pasarela rechazó la transacción. No se te cobró nada.',
    'pagos.intento.motivo.ERROR':
      'Hubo un error al procesar el pago. Puedes intentarlo de nuevo. Si ves un cobro en tu banco, la confirmación puede tardar unos minutos.',
    'pagos.intento.motivo.ABANDONADO':
      'No se completó el pago en 30 minutos. No cuenta como un intento.',

    // Intentos que quedan (RF-58: tres intentos con resultado de la pasarela)
    'pagos.intentos.restantes': {
      uno: 'Te queda {n} intento de pago',
      otros: 'Te quedan {n} intentos de pago',
    },
    'pagos.intentos.ninguno': 'No te quedan intentos de pago.',
    'pagos.intentos.ultimo':
      'Este es tu último intento: si el pago se rechaza, la solicitud vencerá.',
  },
  {
    'pagos.intento.estado.EN_CURSO': 'In progress',
    'pagos.intento.estado.APROBADO': 'Approved',
    'pagos.intento.estado.RECHAZADO': 'Rejected',
    'pagos.intento.estado.ERROR': 'Failed',
    'pagos.intento.estado.ABANDONADO': 'Abandoned',
    'pagos.intento.motivo.EN_CURSO': 'Waiting for the payment gateway to confirm.',
    'pagos.intento.motivo.APROBADO': 'The payment gateway approved the payment.',
    'pagos.intento.motivo.RECHAZADO':
      'The payment gateway rejected the transaction. You were not charged.',
    'pagos.intento.motivo.ERROR':
      'There was an error processing the payment. You can try again. If you see a charge from your bank, the confirmation may take a few minutes.',
    'pagos.intento.motivo.ABANDONADO':
      'The payment was not completed within 30 minutes. It does not count as an attempt.',

    'pagos.intentos.restantes': {
      uno: 'You have {n} payment attempt left',
      otros: 'You have {n} payment attempts left',
    },
    'pagos.intentos.ninguno': 'You have no payment attempts left.',
    'pagos.intentos.ultimo':
      'This is your last attempt: if the payment is rejected, the request will expire.',
  },
);
