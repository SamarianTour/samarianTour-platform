import { CheckoutPago, EstadoIntento, EstadoSolicitud, IntentoPago } from '../../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../../core/idioma/tabla-de-textos';
import { traducir } from '../../../core/idioma/traducir';
import { Plazo } from './plazo';

/** Los intentos de pago con resultado de la pasarela que tiene una solicitud (RF-58). Al tercer rechazo vence. */
export const INTENTOS_MAXIMOS = 3;

/** El nombre de cada estado de un intento, en el idioma vigente. */
export const ETIQUETA_INTENTO = tablaDeTextos<EstadoIntento>({
  EN_CURSO: 'pagos.intento.estado.EN_CURSO',
  APROBADO: 'pagos.intento.estado.APROBADO',
  RECHAZADO: 'pagos.intento.estado.RECHAZADO',
  ERROR: 'pagos.intento.estado.ERROR',
  ABANDONADO: 'pagos.intento.estado.ABANDONADO',
});

/**
 * Por qué un intento terminó así. [SUPUESTO] El contrato no entrega un motivo en texto, solo el estado del intento, así que
 * el motivo es una explicación fija de cada estado.
 */
export const MOTIVO_INTENTO = tablaDeTextos<EstadoIntento>({
  EN_CURSO: 'pagos.intento.motivo.EN_CURSO',
  APROBADO: 'pagos.intento.motivo.APROBADO',
  RECHAZADO: 'pagos.intento.motivo.RECHAZADO',
  ERROR: 'pagos.intento.motivo.ERROR',
  ABANDONADO: 'pagos.intento.motivo.ABANDONADO',
});

/** El color de la etiqueta de cada estado: una clase de las etiquetas globales (siempre con texto, no solo color). */
export const CLASE_DE_INTENTO: Readonly<Record<EstadoIntento, string>> = {
  EN_CURSO: 'etiqueta--info',
  APROBADO: 'etiqueta--exito',
  RECHAZADO: 'etiqueta--peligro',
  ERROR: 'etiqueta--peligro',
  ABANDONADO: 'etiqueta--neutra',
};

/**
 * Si el intento cuenta para el límite de tres: lo rechazó la pasarela o terminó en error (RF-58). Uno abandonado (sin
 * resultado a los 30 min) o en curso no cuenta, y uno aprobado cierra el pago.
 */
function cuentaComoRechazo(intento: IntentoPago): boolean {
  return intento.estado === 'RECHAZADO' || intento.estado === 'ERROR';
}

/** Los intentos que ya usó el turista contra el límite de tres. */
export function intentosUsados(intentos: readonly IntentoPago[]): number {
  return intentos.filter(cuentaComoRechazo).length;
}

/** Los intentos de pago que le quedan, sin bajar de cero. */
export function intentosRestantes(intentos: readonly IntentoPago[]): number {
  return Math.max(0, INTENTOS_MAXIMOS - intentosUsados(intentos));
}

/** El intento que sigue abierto en la pasarela (el más reciente en curso), o null. */
export function intentoEnCurso(intentos: readonly IntentoPago[]): IntentoPago | null {
  return [...intentos].reverse().find((intento) => intento.estado === 'EN_CURSO') ?? null;
}

/** El intento que la pasarela aprobó, o null. Su `transaccionId` es la referencia del pago (RF-52). */
export function intentoAprobado(intentos: readonly IntentoPago[]): IntentoPago | null {
  return intentos.find((intento) => intento.estado === 'APROBADO') ?? null;
}

/**
 * Si se puede abrir un pago ahora: la solicitud está confirmada o con el pago fallido, le quedan intentos y el plazo no
 * terminó. El servidor vuelve a comprobarlo (`SOLICITUD_NO_COBRABLE`, `INTENTOS_AGOTADOS`, `PLAZO_VENCIDO`). Con un intento
 * en curso también se puede: abrir el pago de nuevo devuelve ese mismo intento, no abre otro.
 */
export function puedePagar(
  estado: EstadoSolicitud,
  intentos: readonly IntentoPago[],
  plazo: Plazo,
): boolean {
  if (estado !== 'CONFIRMADA' && estado !== 'PAGO_FALLIDO') {
    return false;
  }
  return plazo.nivel !== 'vencido' && intentosRestantes(intentos) > 0;
}

/** Lo que se le dice al turista sobre los intentos que le quedan. */
export interface AvisoDeIntentos {
  readonly restantes: number;
  /** "Te quedan 2 intentos de pago", o que no le queda ninguno. */
  readonly texto: string;
  /** Con un solo intento restante, el aviso de que si se rechaza la solicitud vencerá (RF-58); si no, null. */
  readonly ultimo: string | null;
}

/** El aviso de intentos: cuántos quedan y, antes del último, que un rechazo vence la solicitud. */
export function avisoDeIntentos(intentos: readonly IntentoPago[]): AvisoDeIntentos {
  const restantes = intentosRestantes(intentos);
  return {
    restantes,
    texto:
      restantes > 0
        ? traducir('pagos.intentos.restantes', { n: restantes })
        : traducir('pagos.intentos.ninguno'),
    ultimo: restantes === 1 ? traducir('pagos.intentos.ultimo') : null,
  };
}

/** Los centavos de peso que maneja la pasarela, en pesos. */
export function centavosAPesos(centavos: number): number {
  return centavos / 100;
}

/**
 * La dirección del checkout, si es segura para abrirla: absoluta, `http` o `https` y sin usuario ni contraseña. Viene del
 * núcleo, pero se revisa antes de mandar al turista allí (la pasarela simulada del prototipo usa `http://localhost`). Devuelve
 * null para cualquier otra cosa, como `javascript:`, `data:` o una dirección sin protocolo.
 */
export function urlDeCheckoutSegura(checkout: Pick<CheckoutPago, 'url'> | null | undefined): string | null {
  const texto = checkout?.url?.trim();
  if (!texto) {
    return null;
  }
  try {
    const url = new URL(texto);
    const esWeb = url.protocol === 'https:' || url.protocol === 'http:';
    return esWeb && !url.username && !url.password ? url.toString() : null;
  } catch {
    return null;
  }
}

/** Cada cuánto se reconsulta mientras se espera la confirmación del pago, al principio (RF-50). */
export const ESPERA_RAPIDA_MS = 3_000;
/** Cada cuánto se reconsulta después de la ventana rápida, mientras el intento siga en curso. */
export const ESPERA_LENTA_MS = 15_000;
/** Durante cuánto tiempo se reconsulta rápido desde que empezó la espera. */
export const VENTANA_RAPIDA_MS = 120_000;

/**
 * Cuánto esperar antes de la siguiente reconsulta, según cuánto lleva esperando el turista: cada 3 segundos los primeros 2
 * minutos y después cada 15 segundos. El estado del pago lo decide solo el servidor (RF-50, RF-51), así que se pregunta hasta
 * que cambie; el núcleo abandona el intento a los 30 minutos si la pasarela no responde.
 */
export function esperaDeReconsulta(transcurridoMs: number): number {
  return transcurridoMs < VENTANA_RAPIDA_MS ? ESPERA_RAPIDA_MS : ESPERA_LENTA_MS;
}

/**
 * Si hay un pago esperando la confirmación del servidor: la solicitud sigue sin pagarse (confirmada o con el pago fallido) y
 * hay un intento en curso. Con la solicitud ya pagada, vencida o cerrada no se espera nada.
 */
export function estaConfirmandoPago(
  estado: EstadoSolicitud,
  intentos: readonly IntentoPago[],
): boolean {
  return (estado === 'CONFIRMADA' || estado === 'PAGO_FALLIDO') && intentoEnCurso(intentos) !== null;
}

/**
 * Si los intentos de pago de la solicitud importan en pantalla: mientras se puede pagar (confirmada o con el pago fallido,
 * para saber cuántos intentos quedan y si hay uno esperando) y una vez pagada (para mostrar la referencia de la transacción,
 * RF-52). En los demás estados nunca hubo, o ya no importan, así que no se piden.
 */
export function mostrarIntentos(estado: EstadoSolicitud): boolean {
  return estado === 'CONFIRMADA' || estado === 'PAGO_FALLIDO' || estado === 'PAGADA';
}
