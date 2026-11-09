import { CodigoError } from '../api/modelos';

/** Propiedades que el núcleo suma al problem+json en tres códigos (nucleo.openapi.yaml, esquema Problema). */
export interface ExtrasError {
  /** Con CUENTA_BLOQUEADA: hasta cuándo no se puede iniciar sesión (RF-05). Fecha ISO. */
  readonly bloqueadaHasta?: string;
  /** Con CODIGO_INCORRECTO: cuántos intentos quedan antes de que el código se invalide. */
  readonly intentosRestantes?: number;
  /** Con PRESTADOR_CON_COMPROMISOS: lo que el prestador debe cumplir antes de eliminar sus datos. */
  readonly compromisos?: readonly string[];
}

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
    /** Datos que el núcleo agrega a algunos errores: hasta cuándo dura un bloqueo, intentos restantes, compromisos. */
    readonly extras?: ExtrasError,
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
  NO_AUTENTICADO: 'Inicia sesión para continuar.',
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
  ZONA_NO_ENCONTRADA: 'Esta zona ya no está disponible.',
  ZONA_DUPLICADA: 'Ya existe una zona con ese nombre.',

  // Cuentas y sesión (núcleo, módulo 1)
  CORREO_YA_REGISTRADO: 'Ya hay una cuenta con ese correo. Inicia sesión o recupera tu contraseña.',
  POLITICA_NO_VIGENTE: 'La política de datos cambió mientras te registrabas. Léela de nuevo y acéptala.',
  CUENTA_NO_ENCONTRADA: 'No encontramos tu cuenta.',
  CODIGO_INCORRECTO: 'El código no es correcto.',
  CODIGO_NO_VIGENTE: 'El código o el enlace venció o ya se usó. Pide uno nuevo.',
  CREDENCIALES_INVALIDAS: 'El correo o la contraseña no son correctos.',
  CUENTA_BLOQUEADA: 'Tu cuenta está bloqueada por intentos fallidos. Inténtalo de nuevo más tarde.',
  CUENTA_DESACTIVADA: 'Un administrador desactivó tu cuenta, por lo que no puedes iniciar sesión.',
  SESION_INVALIDA: 'Tu sesión venció o se cerró. Inicia sesión de nuevo.',
  CORREO_NO_DISPONIBLE: 'No pudimos enviar el correo en este momento. Vuelve a intentarlo en unos minutos.',
  CONTRASENA_INCORRECTA: 'La contraseña no es correcta.',

  // Prestadores (módulo 7)
  PRESTADOR_YA_REGISTRADO: 'Ya registraste tu RNT. Para cambiarlo, edita el que tienes.',
  PRESTADOR_NO_REGISTRADO: 'Todavía no registraste tu RNT.',
  RNT_YA_REGISTRADO: 'Otro prestador ya registró ese número de RNT.',
  TIPO_NO_COINCIDE_CON_RNT: 'El tipo de prestador no corresponde a la categoría de tu RNT.',
  WHATSAPP_NO_CONFIGURADO: 'Todavía no configuraste tu WhatsApp.',
  CUENTA_COBRO_NO_REGISTRADA: 'Todavía no registraste tu cuenta de cobro.',
  PRESTADOR_CON_COMPROMISOS: 'Antes de eliminar tus datos debes cumplir los compromisos que tienes pendientes.',

  // Administración (módulo 8)
  PRESTADOR_NO_HABILITADO: 'El prestador no tiene el RNT activo, así que no se puede aprobar esta oferta.',
  OPERADOR_NO_VALIDO: 'La cuenta elegida no es la de un operador de disponibilidad activo.',
  OPERADOR_CON_PRESTADORES: 'Este operador todavía tiene prestadores asignados. Reasígnalos antes de desactivarlo.',
  CUENTA_PROPIA: 'No puedes desactivar tu propia cuenta.',
  NO_ESTA_EN_MODERACION: 'Esta versión ya no está en moderación.',
  VERSION_NO_VIGENTE: 'El prestador editó la oferta mientras la revisabas. Revisa la versión nueva.',
  MODERACION_YA_DECIDIDA: 'Ya se tomó la decisión contraria sobre este elemento.',
  CATALOGO_NO_DISPONIBLE: 'El catálogo no responde en este momento. Vuelve a intentarlo en unos minutos.',

  // Solicitudes (módulo 4)
  SOLICITUD_NO_ENCONTRADA: 'Esta solicitud ya no está disponible.',
  TRANSICION_INVALIDA: 'La solicitud cambió de estado mientras la atendías. Recarga para ver el estado actual.',
  PLAZO_VENCIDO: 'El plazo para esta acción ya terminó.',
  SIN_TIEMPO_PARA_CONFIRMAR:
    'No alcanza el tiempo para confirmar antes del servicio. Solo puedes marcarla como no disponible.',
  SOLICITUD_DUPLICADA: 'Ya tienes una solicitud abierta para esta oferta en esa fecha y hora.',
  PRESTADOR_SIN_OPERADOR: 'Este prestador todavía no tiene un operador asignado, así que no recibe solicitudes por ahora.',
  WHATSAPP_NO_VERIFICADO: 'Verifica tu WhatsApp para poder enviar solicitudes.',
  CLAVE_IDEMPOTENCIA_REUTILIZADA: 'Esta operación ya se había enviado con otros datos. Vuelve a empezar.',

  // Pagos (módulo 5) y bandeja de avisos
  SOLICITUD_NO_COBRABLE: 'Solo se puede pagar una solicitud confirmada o con el pago fallido.',
  INTENTOS_AGOTADOS: 'Ya usaste los tres intentos de pago de esta solicitud.',
  EVENTO_NO_AUTENTICO: 'El evento del pago no es auténtico.',
  EVENTO_NO_ENCONTRADO: 'Este evento ya no está disponible.',
  EVENTO_NO_FALLIDO: 'Este evento no está en estado fallido.',
  AVISO_NO_ENCONTRADO: 'Este aviso ya no está disponible.',
  AVISO_NO_RECHAZADO: 'Este aviso no está rechazado.',

  // Recomendación
  RECOMENDACION_NO_DISPONIBLE:
    'No pudimos generar recomendaciones en este momento. Puedes seguir explorando el catálogo.',
  ALERTAS_NO_DISPONIBLES: 'No pudimos consultar las alertas del destino en este momento.',
};

export const MENSAJE_SIN_CONEXION = 'No pudimos conectar con el servidor. Revisa tu conexión y vuelve a intentarlo.';
export const MENSAJE_GENERICO = 'Algo salió mal. Vuelve a intentarlo en un momento.';
