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

    // P04.1 Pagar la solicitud
    'pagos.pagar.titulo': 'Paga para asegurar tu cupo',
    'pagos.pagar.sinCargos':
      'Este total no tiene cargos adicionales para ti: la comisión de la plataforma se descuenta al prestador.',
    'pagos.pagar.externa':
      'Te llevamos a la pasarela de pago externa, donde eliges cómo pagar. Samarian Tour no pide ni guarda los datos de tu tarjeta.',
    'pagos.pagar.boton': 'Pagar {total}',
    'pagos.pagar.abriendo': 'Abriendo la pasarela…',
    'pagos.pagar.seguro': 'Pago procesado por la pasarela externa, con conexión cifrada.',
    'pagos.pagar.enlaceManual': 'Si la pasarela no se abre, continúa aquí.',
    'pagos.pagar.sinUrl':
      'No pudimos abrir la pasarela de pago. Inténtalo de nuevo en unos minutos.',
    'pagos.pagar.tituloContinuar': 'Continúa tu pago',
    'pagos.pagar.textoContinuar':
      'Si cerraste la pasarela antes de terminar, puedes continuar con el mismo pago. No se te cobrará dos veces.',
    'pagos.pagar.botonContinuar': 'Continuar el pago',
    'pagos.lista.pagar': 'Pagar',
    'pagos.lista.reintentar': 'Reintentar pago',

    // P04.2 Intentos y reintento
    'pagos.intentos.titulo': 'Intentos de pago',
    'pagos.intentos.numero': 'Intento {n}',
    'pagos.intentos.iniciado': 'Iniciado {fecha}',
    'pagos.intentos.resuelto': 'Resuelto {fecha}',
    'pagos.intentos.referencia': 'Referencia {codigo}',
    'pagos.reintentar.titulo': 'Tu pago no se completó',
    'pagos.reintentar.texto': 'Puedes intentarlo de nuevo hasta el {fecha}.',
    'pagos.reintentar.textoSinFecha': 'Puedes intentarlo de nuevo.',
    'pagos.reintentar.boton': 'Reintentar pago',
    'pagos.reintentar.sinReintento':
      'Ya no puedes reintentar el pago de esta solicitud. Puedes pedir disponibilidad de nuevo.',

    // Al volver de la pasarela: el estado del pago lo decide solo el servidor (RF-50, RF-51)
    'pagos.confirmando.titulo': 'Confirmando tu pago',
    'pagos.confirmando.texto':
      'Estamos esperando que la pasarela confirme tu pago. Esta pantalla se actualiza sola; no hace falta volver a pagar.',
    'pagos.confirmando.tardanza':
      'Si ves un cobro en tu banco, la confirmación puede tardar unos minutos.',

    // Pago confirmado (P04.1)
    'pagos.confirmado.titulo': 'Pago confirmado',
    'pagos.confirmado.texto': 'La pasarela confirmó tu pago. Tu cupo está asegurado.',
    'pagos.confirmado.pagado': 'Total pagado',
    'pagos.confirmado.referencia': 'Referencia de la transacción',
    'pagos.confirmado.fecha': 'Fecha del pago',
    'pagos.confirmado.seguir': 'Seguir explorando',
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

    'pagos.pagar.titulo': 'Pay to secure your spot',
    'pagos.pagar.sinCargos':
      'This total has no additional charges for you: the platform fee is deducted from the provider.',
    'pagos.pagar.externa':
      'We take you to the external payment gateway, where you choose how to pay. Samarian Tour does not ask for or store your card details.',
    'pagos.pagar.boton': 'Pay {total}',
    'pagos.pagar.abriendo': 'Opening the payment gateway…',
    'pagos.pagar.seguro': 'Payment processed by the external gateway, over an encrypted connection.',
    'pagos.pagar.enlaceManual': 'If the payment gateway does not open, continue here.',
    'pagos.pagar.sinUrl': "We couldn't open the payment gateway. Try again in a few minutes.",
    'pagos.pagar.tituloContinuar': 'Continue your payment',
    'pagos.pagar.textoContinuar':
      'If you closed the payment gateway before finishing, you can continue with the same payment. You will not be charged twice.',
    'pagos.pagar.botonContinuar': 'Continue payment',
    'pagos.lista.pagar': 'Pay',
    'pagos.lista.reintentar': 'Retry payment',

    'pagos.intentos.titulo': 'Payment attempts',
    'pagos.intentos.numero': 'Attempt {n}',
    'pagos.intentos.iniciado': 'Started {fecha}',
    'pagos.intentos.resuelto': 'Resolved {fecha}',
    'pagos.intentos.referencia': 'Reference {codigo}',
    'pagos.reintentar.titulo': 'Your payment was not completed',
    'pagos.reintentar.texto': 'You can try again until {fecha}.',
    'pagos.reintentar.textoSinFecha': 'You can try again.',
    'pagos.reintentar.boton': 'Retry payment',
    'pagos.reintentar.sinReintento':
      'You can no longer retry the payment for this request. You can request availability again.',

    'pagos.confirmando.titulo': 'Confirming your payment',
    'pagos.confirmando.texto':
      'We are waiting for the payment gateway to confirm your payment. This screen updates by itself; you do not need to pay again.',
    'pagos.confirmando.tardanza':
      'If you see a charge from your bank, the confirmation may take a few minutes.',

    'pagos.confirmado.titulo': 'Payment confirmed',
    'pagos.confirmado.texto': 'The payment gateway confirmed your payment. Your spot is secured.',
    'pagos.confirmado.pagado': 'Total paid',
    'pagos.confirmado.referencia': 'Transaction reference',
    'pagos.confirmado.fecha': 'Payment date',
    'pagos.confirmado.seguir': 'Keep exploring',
  },
);
