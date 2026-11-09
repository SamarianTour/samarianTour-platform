import type { CodigoError } from '../api/modelos';
import { diccionario } from './diccionario';

/**
 * Los mensajes de error de la API (DC-15), uno por código: `errores.codigo.<CODIGO>`. El código nunca se muestra, solo
 * decide el mensaje. Un código nuevo en los contratos necesita su mensaje aquí, en español y en inglés: si falta, el
 * build falla.
 */
export const ERRORES = diccionario(
  {
    'errores.generico': 'Algo salió mal. Vuelve a intentarlo en un momento.',
    'errores.sinConexion':
      'No pudimos conectar con el servidor. Revisa tu conexión y vuelve a intentarlo.',

    // Bloqueo de la cuenta por intentos fallidos (RF-05).
    'errores.bloqueo.general':
      'Tu cuenta está bloqueada por intentos fallidos. Inténtalo de nuevo en unos minutos.',
    'errores.bloqueo.hoy':
      'Tu cuenta está bloqueada por intentos fallidos. Podrás intentarlo de nuevo {espera}, a las {hora}.',
    'errores.bloqueo.otroDia':
      'Tu cuenta está bloqueada por intentos fallidos. Podrás intentarlo de nuevo {espera}, el {hora}.',
    'errores.bloqueo.espera': { uno: 'en {n} minuto', otros: 'en {n} minutos' },

    'errores.codigo.NO_AUTENTICADO': 'Inicia sesión para continuar.',
    'errores.codigo.ACCESO_DENEGADO': 'Tu cuenta no tiene permiso para entrar a esta sección.',
    'errores.codigo.RNT_INACTIVO':
      'Tu Registro Nacional de Turismo no está activo, así que no puedes publicar ni editar ofertas.',
    'errores.codigo.TIPO_OFERTA_NO_PERMITIDO':
      'Tu tipo de prestador no puede publicar este tipo de oferta.',
    'errores.codigo.PRESTADOR_NO_AUTORIZADO': 'Esta oferta es de otro prestador.',
    'errores.codigo.DATO_INVALIDO':
      'Hay un dato que no cumple las reglas. Revísalo y vuelve a intentarlo.',
    'errores.codigo.ZONA_NO_DISPONIBLE': 'La zona elegida ya no está disponible. Elige otra.',
    'errores.codigo.OFERTA_NO_ENCONTRADA': 'Esta oferta ya no está disponible.',
    'errores.codigo.OFERTA_NO_EDITABLE': 'Esta oferta está retirada y ya no se puede editar.',
    'errores.codigo.CONFLICTO_CONCURRENCIA':
      'Alguien más modificó esta oferta, vuelve a intentarlo.',
    'errores.codigo.OPINION_NO_HABILITADA': 'Solo puedes opinar sobre ofertas que hayas pagado.',
    'errores.codigo.OPINION_DUPLICADA': 'Ya opinaste sobre esta oferta.',
    'errores.codigo.OPINION_NO_ENCONTRADA': 'Esta opinión ya no está disponible.',
    'errores.codigo.PERFIL_NO_ENCONTRADO':
      'Todavía no tienes perfil público. Guarda primero tu descripción.',
    'errores.codigo.FOTO_NO_ENCONTRADA': 'Esta foto ya no está disponible.',
    'errores.codigo.ZONA_NO_ENCONTRADA': 'Esta zona ya no está disponible.',
    'errores.codigo.ZONA_DUPLICADA': 'Ya existe una zona con ese nombre.',

    // Cuentas y sesión (núcleo, módulo 1)
    'errores.codigo.CORREO_YA_REGISTRADO':
      'Ya hay una cuenta con ese correo. Inicia sesión o recupera tu contraseña.',
    'errores.codigo.POLITICA_NO_VIGENTE':
      'La política de datos cambió mientras te registrabas. Léela de nuevo y acéptala.',
    'errores.codigo.CUENTA_NO_ENCONTRADA': 'No encontramos tu cuenta.',
    'errores.codigo.CODIGO_INCORRECTO': 'El código no es correcto.',
    'errores.codigo.CODIGO_NO_VIGENTE': 'El código o el enlace venció o ya se usó. Pide uno nuevo.',
    'errores.codigo.CREDENCIALES_INVALIDAS': 'El correo o la contraseña no son correctos.',
    'errores.codigo.CUENTA_BLOQUEADA':
      'Tu cuenta está bloqueada por intentos fallidos. Inténtalo de nuevo más tarde.',
    'errores.codigo.CUENTA_DESACTIVADA':
      'Un administrador desactivó tu cuenta, por lo que no puedes iniciar sesión.',
    'errores.codigo.SESION_INVALIDA': 'Tu sesión venció o se cerró. Inicia sesión de nuevo.',
    'errores.codigo.CORREO_NO_DISPONIBLE':
      'No pudimos enviar el correo en este momento. Vuelve a intentarlo en unos minutos.',
    'errores.codigo.CONTRASENA_INCORRECTA': 'La contraseña no es correcta.',

    // Prestadores (módulo 7)
    'errores.codigo.PRESTADOR_YA_REGISTRADO':
      'Ya registraste tu RNT. Para cambiarlo, edita el que tienes.',
    'errores.codigo.PRESTADOR_NO_REGISTRADO': 'Todavía no registraste tu RNT.',
    'errores.codigo.RNT_YA_REGISTRADO': 'Otro prestador ya registró ese número de RNT.',
    'errores.codigo.TIPO_NO_COINCIDE_CON_RNT':
      'El tipo de prestador no corresponde a la categoría de tu RNT.',
    'errores.codigo.WHATSAPP_NO_CONFIGURADO': 'Todavía no configuraste tu WhatsApp.',
    'errores.codigo.CUENTA_COBRO_NO_REGISTRADA': 'Todavía no registraste tu cuenta de cobro.',
    'errores.codigo.PRESTADOR_CON_COMPROMISOS':
      'Antes de eliminar tus datos debes cumplir los compromisos que tienes pendientes.',

    // Administración (módulo 8)
    'errores.codigo.PRESTADOR_NO_HABILITADO':
      'El prestador no tiene el RNT activo, así que no se puede aprobar esta oferta.',
    'errores.codigo.OPERADOR_NO_VALIDO':
      'La cuenta elegida no es la de un operador de disponibilidad activo.',
    'errores.codigo.OPERADOR_CON_PRESTADORES':
      'Este operador todavía tiene prestadores asignados. Reasígnalos antes de desactivarlo.',
    'errores.codigo.CUENTA_PROPIA': 'No puedes desactivar tu propia cuenta.',
    'errores.codigo.NO_ESTA_EN_MODERACION': 'Esta versión ya no está en moderación.',
    'errores.codigo.VERSION_NO_VIGENTE':
      'El prestador editó la oferta mientras la revisabas. Revisa la versión nueva.',
    'errores.codigo.MODERACION_YA_DECIDIDA':
      'Ya se tomó la decisión contraria sobre este elemento.',
    'errores.codigo.CATALOGO_NO_DISPONIBLE':
      'El catálogo no responde en este momento. Vuelve a intentarlo en unos minutos.',

    // Solicitudes (módulo 4)
    'errores.codigo.SOLICITUD_NO_ENCONTRADA': 'Esta solicitud ya no está disponible.',
    'errores.codigo.TRANSICION_INVALIDA':
      'La solicitud cambió de estado mientras la atendías. Recarga para ver el estado actual.',
    'errores.codigo.PLAZO_VENCIDO': 'El plazo para esta acción ya terminó.',
    'errores.codigo.SIN_TIEMPO_PARA_CONFIRMAR':
      'No alcanza el tiempo para confirmar antes del servicio. Solo puedes marcarla como no disponible.',
    'errores.codigo.SOLICITUD_DUPLICADA':
      'Ya tienes una solicitud abierta para esta oferta en esa fecha y hora.',
    'errores.codigo.PRESTADOR_SIN_OPERADOR':
      'Este prestador todavía no tiene un operador asignado, así que no recibe solicitudes por ahora.',
    'errores.codigo.WHATSAPP_NO_VERIFICADO': 'Verifica tu WhatsApp para poder enviar solicitudes.',
    'errores.codigo.CLAVE_IDEMPOTENCIA_REUTILIZADA':
      'Esta operación ya se había enviado con otros datos. Vuelve a empezar.',

    // Pagos (módulo 5) y bandeja de avisos
    'errores.codigo.SOLICITUD_NO_COBRABLE':
      'Solo se puede pagar una solicitud confirmada o con el pago fallido.',
    'errores.codigo.INTENTOS_AGOTADOS': 'Ya usaste los tres intentos de pago de esta solicitud.',
    'errores.codigo.EVENTO_NO_AUTENTICO': 'El evento del pago no es auténtico.',
    'errores.codigo.EVENTO_NO_ENCONTRADO': 'Este evento ya no está disponible.',
    'errores.codigo.EVENTO_NO_FALLIDO': 'Este evento no está en estado fallido.',
    'errores.codigo.AVISO_NO_ENCONTRADO': 'Este aviso ya no está disponible.',
    'errores.codigo.AVISO_NO_RECHAZADO': 'Este aviso no está rechazado.',

    // Recomendación
    'errores.codigo.RECOMENDACION_NO_DISPONIBLE':
      'No pudimos generar recomendaciones en este momento. Puedes seguir explorando el catálogo.',
    'errores.codigo.ALERTAS_NO_DISPONIBLES':
      'No pudimos consultar las alertas del destino en este momento.',
  },
  {
    'errores.generico': 'Something went wrong. Please try again in a moment.',
    'errores.sinConexion':
      "We couldn't connect to the server. Check your connection and try again.",

    'errores.bloqueo.general':
      'Your account is locked after too many failed attempts. Try again in a few minutes.',
    'errores.bloqueo.hoy':
      'Your account is locked after too many failed attempts. You can try again {espera}, at {hora}.',
    'errores.bloqueo.otroDia':
      'Your account is locked after too many failed attempts. You can try again {espera}, on {hora}.',
    'errores.bloqueo.espera': { uno: 'in {n} minute', otros: 'in {n} minutes' },

    'errores.codigo.NO_AUTENTICADO': 'Log in to continue.',
    'errores.codigo.ACCESO_DENEGADO': "Your account doesn't have permission to enter this section.",
    'errores.codigo.RNT_INACTIVO':
      "Your RNT (National Tourism Registry) isn't active, so you can't publish or edit offers.",
    'errores.codigo.TIPO_OFERTA_NO_PERMITIDO':
      "Your provider type can't publish this type of offer.",
    'errores.codigo.PRESTADOR_NO_AUTORIZADO': 'This offer belongs to another provider.',
    'errores.codigo.DATO_INVALIDO':
      "Some of the information doesn't meet the rules. Check it and try again.",
    'errores.codigo.ZONA_NO_DISPONIBLE':
      'The area you chose is no longer available. Choose another one.',
    'errores.codigo.OFERTA_NO_ENCONTRADA': 'This offer is no longer available.',
    'errores.codigo.OFERTA_NO_EDITABLE':
      'This offer has been withdrawn and can no longer be edited.',
    'errores.codigo.CONFLICTO_CONCURRENCIA': 'Someone else changed this offer. Please try again.',
    'errores.codigo.OPINION_NO_HABILITADA': 'You can only review offers you have paid for.',
    'errores.codigo.OPINION_DUPLICADA': "You've already reviewed this offer.",
    'errores.codigo.OPINION_NO_ENCONTRADA': 'This review is no longer available.',
    'errores.codigo.PERFIL_NO_ENCONTRADO':
      "You don't have a public profile yet. Save your description first.",
    'errores.codigo.FOTO_NO_ENCONTRADA': 'This photo is no longer available.',
    'errores.codigo.ZONA_NO_ENCONTRADA': 'This area is no longer available.',
    'errores.codigo.ZONA_DUPLICADA': 'An area with that name already exists.',

    'errores.codigo.CORREO_YA_REGISTRADO':
      'There is already an account with that email. Log in or reset your password.',
    'errores.codigo.POLITICA_NO_VIGENTE':
      'The data processing policy changed while you were signing up. Read it again and accept it.',
    'errores.codigo.CUENTA_NO_ENCONTRADA': "We couldn't find your account.",
    'errores.codigo.CODIGO_INCORRECTO': "That code isn't correct.",
    'errores.codigo.CODIGO_NO_VIGENTE':
      'The code or link has expired or was already used. Request a new one.',
    'errores.codigo.CREDENCIALES_INVALIDAS': "The email or password isn't correct.",
    'errores.codigo.CUENTA_BLOQUEADA':
      'Your account is locked after too many failed attempts. Try again later.',
    'errores.codigo.CUENTA_DESACTIVADA':
      'An administrator deactivated your account, so you cannot log in.',
    'errores.codigo.SESION_INVALIDA': 'Your session expired or was closed. Log in again.',
    'errores.codigo.CORREO_NO_DISPONIBLE':
      "We couldn't send the email right now. Try again in a few minutes.",
    'errores.codigo.CONTRASENA_INCORRECTA': "That password isn't correct.",

    'errores.codigo.PRESTADOR_YA_REGISTRADO':
      "You've already registered your RNT. To change it, edit the one you have.",
    'errores.codigo.PRESTADOR_NO_REGISTRADO': "You haven't registered your RNT yet.",
    'errores.codigo.RNT_YA_REGISTRADO': 'Another provider has already registered that RNT number.',
    'errores.codigo.TIPO_NO_COINCIDE_CON_RNT':
      "The provider type doesn't match the category of your RNT.",
    'errores.codigo.WHATSAPP_NO_CONFIGURADO': "You haven't set up your WhatsApp yet.",
    'errores.codigo.CUENTA_COBRO_NO_REGISTRADA': "You haven't registered your billing account yet.",
    'errores.codigo.PRESTADOR_CON_COMPROMISOS':
      'Before deleting your data, you must fulfill your pending commitments.',

    'errores.codigo.PRESTADOR_NO_HABILITADO':
      "The provider's RNT isn't active, so this offer can't be approved.",
    'errores.codigo.OPERADOR_NO_VALIDO':
      "The account you chose isn't an active availability operator.",
    'errores.codigo.OPERADOR_CON_PRESTADORES':
      'This operator still has providers assigned. Reassign them before deactivating the operator.',
    'errores.codigo.CUENTA_PROPIA': "You can't deactivate your own account.",
    'errores.codigo.NO_ESTA_EN_MODERACION': 'This version is no longer in moderation.',
    'errores.codigo.VERSION_NO_VIGENTE':
      'The provider edited the offer while you were reviewing it. Review the new version.',
    'errores.codigo.MODERACION_YA_DECIDIDA':
      'The opposite decision has already been made on this item.',
    'errores.codigo.CATALOGO_NO_DISPONIBLE':
      "The catalog isn't responding right now. Try again in a few minutes.",

    'errores.codigo.SOLICITUD_NO_ENCONTRADA': 'This request is no longer available.',
    'errores.codigo.TRANSICION_INVALIDA':
      'The request changed status while you were handling it. Reload to see its current status.',
    'errores.codigo.PLAZO_VENCIDO': 'The deadline for this action has passed.',
    'errores.codigo.SIN_TIEMPO_PARA_CONFIRMAR':
      "There isn't enough time to confirm before the service. You can only mark it as unavailable.",
    'errores.codigo.SOLICITUD_DUPLICADA':
      'You already have an open request for this offer on that date and time.',
    'errores.codigo.PRESTADOR_SIN_OPERADOR':
      "This provider doesn't have an operator assigned yet, so it can't receive requests for now.",
    'errores.codigo.WHATSAPP_NO_VERIFICADO': 'Verify your WhatsApp to be able to send requests.',
    'errores.codigo.CLAVE_IDEMPOTENCIA_REUTILIZADA':
      'This operation was already submitted with different data. Start over.',

    'errores.codigo.SOLICITUD_NO_COBRABLE':
      'Only a confirmed request, or one with a failed payment, can be paid.',
    'errores.codigo.INTENTOS_AGOTADOS': "You've used all three payment attempts for this request.",
    'errores.codigo.EVENTO_NO_AUTENTICO': "The payment event isn't authentic.",
    'errores.codigo.EVENTO_NO_ENCONTRADO': 'This event is no longer available.',
    'errores.codigo.EVENTO_NO_FALLIDO': "This event isn't in a failed state.",
    'errores.codigo.AVISO_NO_ENCONTRADO': 'This notice is no longer available.',
    'errores.codigo.AVISO_NO_RECHAZADO': "This notice isn't rejected.",

    'errores.codigo.RECOMENDACION_NO_DISPONIBLE':
      "We couldn't generate recommendations right now. You can keep exploring the catalog.",
    'errores.codigo.ALERTAS_NO_DISPONIBLES': "We couldn't check the destination alerts right now.",
  },
);

/** Todo código de error del contrato tiene su mensaje: si el contrato agrega uno, el build falla hasta escribirlo. */
ERRORES.es satisfies Record<`errores.codigo.${CodigoError}`, unknown>;
