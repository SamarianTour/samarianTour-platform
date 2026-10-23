import { CodigoError } from '../api/modelos';

/**
 * Un error de la API ya traducido para el usuario. El interceptor de errores convierte cada respuesta
 * application/problem+json en uno de estos; el código nunca se muestra, solo decide el mensaje.
 */
export class ErrorApi extends Error {
  constructor(
    /** Estado HTTP; 0 si no hubo respuesta. */
    readonly estado: number,
    readonly codigo: CodigoError | undefined,
    /** Texto para mostrar al usuario. */
    readonly mensaje: string,
    /** Campo afectado, cuando la API lo dice (DATO_INVALIDO). */
    readonly campo?: string,
    /** Lo que la API dice del campo, por ejemplo "debe ser mayor que cero". */
    readonly detalleCampo?: string,
  ) {
    super(mensaje);
    this.name = 'ErrorApi';
  }

  get noEncontrado(): boolean {
    return this.estado === 404;
  }
}

/** Mensaje para el usuario de cada código de la API (DC-15). */
export const MENSAJES: Record<CodigoError, string> = {
  NO_AUTENTICADO: 'Elige un usuario demo para continuar.',
  ACCESO_DENEGADO: 'Tu cuenta no tiene permiso para entrar a esta sección.',
  RNT_INACTIVO: 'Tu Registro Nacional de Turismo no está activo, así que no puedes publicar ni editar ofertas.',
  TIPO_OFERTA_NO_PERMITIDO: 'Tu tipo de prestador no puede publicar este tipo de oferta.',
  PRESTADOR_NO_AUTORIZADO: 'Esta oferta es de otro prestador.',
  DATO_INVALIDO: 'Hay un dato que no cumple las reglas. Revísalo y vuelve a intentarlo.',
  ZONA_NO_DISPONIBLE: 'La zona elegida ya no está disponible. Elige otra.',
  OFERTA_NO_ENCONTRADA: 'Esta oferta ya no está disponible.',
  OFERTA_NO_EDITABLE: 'Esta oferta está retirada y ya no se puede editar.',
  CONFLICTO_CONCURRENCIA: 'Alguien más modificó esta oferta, vuelve a intentarlo.',
  OPINION_NO_HABILITADA: 'Solo puedes opinar sobre ofertas que hayas pagado.',
  OPINION_DUPLICADA: 'Ya opinaste sobre esta oferta.',
  OPINION_NO_ENCONTRADA: 'Esta opinión ya no está disponible.',
  PERFIL_NO_ENCONTRADO: 'Todavía no tienes perfil público. Guarda primero tu descripción.',
  FOTO_NO_ENCONTRADA: 'Esta foto ya no está disponible.',
};

export const MENSAJE_SIN_CONEXION = 'No pudimos conectar con el servidor. Revisa tu conexión y vuelve a intentarlo.';
export const MENSAJE_GENERICO = 'Algo salió mal. Vuelve a intentarlo en un momento.';
