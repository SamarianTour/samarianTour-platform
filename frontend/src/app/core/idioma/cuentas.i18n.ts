import { diccionario } from './diccionario';

/**
 * Textos de las pantallas de cuentas (P06): iniciar sesión, registro, recuperar y restablecer la contraseña, mis datos y
 * derechos, verificar el WhatsApp, eliminar la cuenta, la política de datos y "no tienes permiso".
 *
 * Los textos que la API entrega ya escritos (el texto de la política, los compromisos del prestador, el motivo de un dato
 * inválido) no se traducen aquí: llegan en español.
 */
export const CUENTAS = diccionario(
  {
    // Campos y mensajes de validación que comparten varias pantallas.
    'cuentas.campo.correo': 'Correo electrónico',
    'cuentas.campo.contrasena': 'Contraseña',
    'cuentas.campo.contrasenaNueva': 'Contraseña nueva',
    'cuentas.campo.repetir': 'Repite la contraseña',
    'cuentas.campo.nombre': 'Nombre',
    'cuentas.campo.whatsapp': 'WhatsApp',
    'cuentas.campo.asterisco': 'asterisco',
    'cuentas.campo.ayudaContrasena':
      'Mínimo {minimo} caracteres, con al menos una letra y un número.',
    'cuentas.error.correoInvalido': 'Escribe un correo válido, por ejemplo nombre@correo.co.',
    'cuentas.error.escribeContrasena': 'Escribe una contraseña.',
    'cuentas.error.contrasenaLargo':
      'La contraseña debe tener entre {minimo} y {maximo} caracteres.',
    'cuentas.error.contrasenaLetraNumero':
      'La contraseña debe tener al menos una letra y un número.',
    'cuentas.error.repiteContrasena': 'Repite la contraseña.',
    'cuentas.error.noCoinciden': 'Las contraseñas no coinciden.',
    'cuentas.error.escribeWhatsapp': 'Escribe tu número de WhatsApp.',
    'cuentas.error.whatsappInvalido':
      'Escribe el número con el indicativo del país, por ejemplo +573001234567.',
    'cuentas.error.nombreLargo': 'El nombre puede tener hasta 100 caracteres.',

    // P06.2 Iniciar sesión
    'cuentas.login.titulo': 'Iniciar sesión',
    'cuentas.login.subtitulo': 'Entra para gestionar tus solicitudes, opiniones y datos.',
    'cuentas.login.recordar': 'Recordarme',
    'cuentas.login.olvido': '¿Olvidaste tu contraseña?',
    'cuentas.login.entrar': 'Entrar',
    'cuentas.login.entrando': 'Entrando…',
    'cuentas.login.sinCuenta': '¿No tienes cuenta?',
    'cuentas.login.crear': 'Crea una',
    'cuentas.login.errorCorreo': 'Escribe tu correo.',
    'cuentas.login.errorContrasena': 'Escribe tu contraseña.',

    // P06.3 Recuperar el acceso
    'cuentas.recuperar.titulo': 'Recuperar el acceso',
    'cuentas.recuperar.subtitulo':
      'Escribe el correo de tu cuenta y te enviamos un enlace para elegir una contraseña nueva.',
    'cuentas.recuperar.confirmacionAntes': 'Si hay una cuenta activa con',
    'cuentas.recuperar.confirmacionDespues':
      ', te enviamos un enlace para elegir una contraseña nueva.',
    'cuentas.recuperar.nota1': 'El enlace vence a los 30 minutos y sirve una sola vez.',
    'cuentas.recuperar.nota2': 'Si no lo ves, revisa la carpeta de correo no deseado.',
    'cuentas.recuperar.nota3': 'Si pides otro enlace, el anterior deja de servir.',
    'cuentas.recuperar.volver': 'Volver a iniciar sesión',
    'cuentas.recuperar.pedirOtro': 'Pedir otro enlace',
    'cuentas.recuperar.errorCorreo': 'Escribe el correo de tu cuenta.',
    'cuentas.recuperar.enviando': 'Enviando…',
    'cuentas.recuperar.enviar': 'Enviar el enlace',
    'cuentas.recuperar.recuerdas': '¿Te acordaste?',
    'cuentas.recuperar.inicia': 'Inicia sesión',

    // Restablecer la contraseña (enlace del correo)
    'cuentas.restablecer.titulo': 'Elige una contraseña nueva',
    'cuentas.restablecer.subtitulo':
      'Escríbela dos veces. Después tendrás que iniciar sesión con ella.',
    'cuentas.restablecer.sinCodigo':
      'Este enlace no es válido: le falta el código del correo. Pide uno nuevo.',
    'cuentas.restablecer.vencido':
      'El enlace venció o ya se usó. Los enlaces valen 30 minutos y sirven una sola vez.',
    'cuentas.restablecer.pedirNuevo': 'Pedir un enlace nuevo',
    'cuentas.restablecer.guardando': 'Guardando…',
    'cuentas.restablecer.cambiar': 'Cambiar contraseña',
    'cuentas.restablecer.volver': 'Volver a iniciar sesión',
    'cuentas.restablecer.aviso': 'Cambiaste tu contraseña. Inicia sesión con la nueva.',

    // P06.1 Registro
    'cuentas.registro.titulo': 'Crear una cuenta',
    'cuentas.registro.subtitulo':
      'Regístrate como turista para solicitar actividades, o como prestador para publicar las tuyas.',
    'cuentas.registro.obligatoriosAntes': 'Los campos con',
    'cuentas.registro.obligatoriosDespues': 'son obligatorios.',
    'cuentas.registro.tipoCuenta': '¿Qué tipo de cuenta necesitas?',
    'cuentas.registro.descTurista': 'Quiero descubrir y solicitar planes en Santa Marta.',
    'cuentas.registro.descPrestador':
      'Ofrezco actividades, hospedaje o restaurante y quiero publicarlos.',
    'cuentas.registro.nombreCompleto': 'Nombre completo',
    'cuentas.registro.nombreComercial': 'Nombre comercial del negocio',
    'cuentas.registro.ayudaNombreComercial': 'Como aparece en tu Registro Nacional de Turismo.',
    'cuentas.registro.recuperar': 'Recuperar contraseña',
    'cuentas.registro.ayudaWhatsapp':
      'Con el indicativo del país, por ejemplo +573001234567. Es el único canal por el que te avisamos de tus solicitudes; te enviaremos un código para verificarlo.',
    'cuentas.registro.consentimiento':
      'Autorizo el tratamiento de mis datos personales conforme a la Política de Tratamiento de Datos y a la Ley 1581 de 2012. Entiendo mis derechos como titular y que puedo consultarlos, actualizarlos o solicitar su supresión.',
    'cuentas.registro.leerPolitica': 'Leer la política (versión {version})',
    'cuentas.registro.cargandoPolitica': 'Cargando la política…',
    'cuentas.registro.creando': 'Creando tu cuenta…',
    'cuentas.registro.crear': 'Crear cuenta',
    'cuentas.registro.yaTienes': '¿Ya tienes cuenta?',
    'cuentas.registro.inicia': 'Inicia sesión',
    'cuentas.registro.sinPolitica':
      'No pudimos cargar la política de datos que debes aceptar. Vuelve a intentarlo antes de crear la cuenta.',
    'cuentas.registro.errorNombreTurista': 'Escribe tu nombre completo.',
    'cuentas.registro.errorNombrePrestador': 'Escribe el nombre comercial de tu negocio.',
    'cuentas.registro.errorCorreo': 'Escribe tu correo.',
    'cuentas.registro.errorCorreoLargo': 'El correo puede tener hasta 254 caracteres.',
    'cuentas.registro.errorAcepta':
      'Debes aceptar la política de tratamiento de datos para crear tu cuenta.',
    'cuentas.registro.avisoTurista':
      'Cuenta creada. Te enviamos un código por WhatsApp para verificar tu número.',
    'cuentas.registro.avisoPrestador': 'Cuenta creada. Bienvenido a Samarian Tour.',
    'cuentas.registro.avisoSinSesion': 'Cuenta creada. Inicia sesión para continuar.',

    // Texto de la política de datos (diálogo)
    'cuentas.politica.titulo': 'Política de tratamiento de datos',
    'cuentas.politica.version': 'Versión {version}',
    'cuentas.politica.region': 'Texto de la política',
    'cuentas.politica.cargando': 'Cargando el texto…',

    // P06.4 Mis datos y derechos
    'cuentas.misDatos.rotulo': 'Mi cuenta',
    'cuentas.misDatos.titulo': 'Mis datos y derechos del titular',
    'cuentas.misDatos.intro':
      'Consulta y controla tu información personal. Samarian Tour trata tus datos conforme a la Ley 1581 de 2012 de protección de datos.',
    'cuentas.misDatos.seccion': 'Mis datos',
    'cuentas.misDatos.editar': 'Editar mis datos',
    'cuentas.misDatos.correo': 'Correo',
    'cuentas.misDatos.correoFijo': 'El correo no se puede cambiar.',
    'cuentas.misDatos.verificado': 'Verificado',
    'cuentas.misDatos.sinVerificar': 'Sin verificar',
    'cuentas.misDatos.tipoCuenta': 'Tipo de cuenta',
    'cuentas.misDatos.ayudaWhatsapp':
      'Con el indicativo del país, por ejemplo +573001234567. Si lo cambias, te enviamos un código para verificar el número nuevo.',
    'cuentas.misDatos.guardando': 'Guardando…',
    'cuentas.misDatos.guardar': 'Guardar cambios',
    'cuentas.misDatos.errorNombre': 'Escribe tu nombre.',
    'cuentas.misDatos.guardado': 'Guardamos tus datos.',
    'cuentas.misDatos.guardadoConCodigo':
      'Guardamos tus datos. Te enviamos un código por WhatsApp para verificar tu número nuevo.',
    'cuentas.misDatos.conflicto': 'Tus datos cambiaron mientras los editabas. Vuelve a intentarlo.',
    'cuentas.misDatos.cargando': 'Cargando tus datos…',
    'cuentas.misDatos.derechosTitulo': 'Tus derechos',
    'cuentas.misDatos.derechosIntro':
      'Como titular de tus datos puedes ejercer estos derechos cuando quieras. Estos son los que ya puedes usar desde la aplicación y los que todavía no.',
    'cuentas.misDatos.acceso': 'Acceso',
    'cuentas.misDatos.accesoDesc': 'Conocer qué datos tenemos sobre ti y cómo los usamos.',
    'cuentas.misDatos.verDatos': 'Ver mis datos',
    'cuentas.misDatos.rectificacion': 'Rectificación',
    'cuentas.misDatos.rectificacionDesc': 'Corregir datos inexactos o incompletos.',
    'cuentas.misDatos.actualizar': 'Actualizar',
    'cuentas.misDatos.supresion': 'Supresión',
    'cuentas.misDatos.supresionDesc': 'Pedir que eliminemos tus datos cuando proceda.',
    'cuentas.misDatos.solicitar': 'Solicitar',
    'cuentas.misDatos.cuentaInterna': 'Tu cuenta es interna: la gestiona un administrador.',
    'cuentas.misDatos.oposicion': 'Oposición',
    'cuentas.misDatos.oposicionDesc': 'Oponerte a ciertos usos de tus datos.',
    'cuentas.misDatos.portabilidad': 'Portabilidad',
    'cuentas.misDatos.portabilidadDesc': 'Recibir una copia de tus datos en un archivo.',
    'cuentas.misDatos.revocar': 'Revocar la autorización',
    'cuentas.misDatos.revocarDesc':
      'Retirar el permiso que diste para el tratamiento de tus datos.',
    'cuentas.misDatos.noDisponible': 'Todavía no se puede ejercer desde la aplicación.',
    'cuentas.misDatos.vigenteAntes': 'Versión vigente:',
    'cuentas.misDatos.vigenteDespues': ', publicada el {fecha}.',
    'cuentas.misDatos.leerPolitica': 'Leer la política',
    'cuentas.misDatos.sinPolitica': 'No pudimos cargar la política en este momento.',

    // Verificar el WhatsApp
    'cuentas.verificar.titulo': 'Verifica tu WhatsApp',
    'cuentas.verificar.enviadoAl': 'Te enviamos un código de 6 dígitos al',
    'cuentas.verificar.enviadoSinNumero': 'Te enviamos un código de 6 dígitos a tu WhatsApp.',
    'cuentas.verificar.aviso':
      'Mientras no lo verifiques no podrás enviar solicitudes. El código vale 30 minutos.',
    'cuentas.verificar.reenviado': 'Te enviamos un código nuevo. El anterior ya no sirve.',
    'cuentas.verificar.etiqueta': 'Código de 6 dígitos',
    'cuentas.verificar.verificando': 'Verificando…',
    'cuentas.verificar.verificar': 'Verificar',
    'cuentas.verificar.enviando': 'Enviando…',
    'cuentas.verificar.otro': 'Enviar otro código',
    'cuentas.verificar.errorVacio': 'Escribe el código que te llegó.',
    'cuentas.verificar.errorFormato': 'El código tiene 6 dígitos, solo números.',
    'cuentas.verificar.agotado':
      'El código no es correcto y ya no admite más intentos. Pide uno nuevo.',
    'cuentas.verificar.intentos': {
      uno: 'El código no es correcto. Te queda {n} intento.',
      otros: 'El código no es correcto. Te quedan {n} intentos.',
    },
    'cuentas.verificar.verificado': 'Verificaste tu WhatsApp',

    // Eliminar la cuenta
    'cuentas.eliminar.titulo': 'Eliminar mi cuenta',
    'cuentas.eliminar.prestador1':
      'Borraremos tus datos personales, tu WhatsApp y tu habilitación, y cerraremos tus sesiones. Tus ofertas se retiran para siempre y se borran tu perfil y tus fotos del catálogo. Tus solicitudes abiertas sin pagar se cierran.',
    'cuentas.eliminar.prestador2':
      'El RNT, la razón social, tus cuentas de cobro y tus dispersiones se conservan como soporte contable, pero el RNT queda libre para registrarse en otra cuenta. Si todavía tienes servicios pagados por cumplir o dinero por recibir, primero tienes que resolverlos.',
    'cuentas.eliminar.turista1':
      'Borraremos tu nombre, tu correo, tu contraseña y tu WhatsApp, cerraremos tus sesiones y quitaremos tus opiniones del catálogo. Tus solicitudes se conservan solo con un identificador, sin tus datos.',
    'cuentas.eliminar.turista2':
      'Las actividades ya realizadas pueden conservarse de forma anonimizada por obligaciones legales y contables. Con el mismo correo podrás crear una cuenta nueva.',
    'cuentas.eliminar.advertencia': 'Esta acción es inmediata y no se puede deshacer.',
    'cuentas.eliminar.dialogoTitulo': '¿Eliminar tu cuenta?',
    'cuentas.eliminar.dialogoAntes':
      'Perderás el acceso y tus datos personales se borrarán ahora mismo.',
    'cuentas.eliminar.dialogoNegrita': 'No se puede deshacer.',
    'cuentas.eliminar.dialogoDespues': 'Escribe tu contraseña para confirmar.',
    'cuentas.eliminar.tuContrasena': 'Tu contraseña',
    'cuentas.eliminar.errorContrasena': 'Escribe tu contraseña para confirmar.',
    'cuentas.eliminar.eliminando': 'Eliminando…',
    'cuentas.eliminar.interna':
      'Tu cuenta es interna: la gestiona un administrador. Desde aquí no se puede eliminar.',
    'cuentas.eliminar.aviso':
      'Eliminamos tus datos personales. Gracias por haber usado Samarian Tour.',

    // No tienes permiso
    'cuentas.sinPermiso.titulo': 'No tienes permiso para ver esta página',
    'cuentas.sinPermiso.entraste': 'Entraste como',
    'cuentas.sinPermiso.explicacion':
      ', y esta sección es de otro tipo de cuenta. Si crees que es un error, inicia sesión con la cuenta correcta.',
    'cuentas.sinPermiso.irInicio': 'Ir a mi inicio',
    'cuentas.sinPermiso.otraCuenta': 'Entrar con otra cuenta',
  },
  {
    'cuentas.campo.correo': 'Email',
    'cuentas.campo.contrasena': 'Password',
    'cuentas.campo.contrasenaNueva': 'New password',
    'cuentas.campo.repetir': 'Repeat the password',
    'cuentas.campo.nombre': 'Name',
    'cuentas.campo.whatsapp': 'WhatsApp',
    'cuentas.campo.asterisco': 'asterisk',
    'cuentas.campo.ayudaContrasena':
      'At least {minimo} characters, with at least one letter and one number.',
    'cuentas.error.correoInvalido': 'Enter a valid email, for example name@email.com.',
    'cuentas.error.escribeContrasena': 'Enter a password.',
    'cuentas.error.contrasenaLargo':
      'The password must be between {minimo} and {maximo} characters long.',
    'cuentas.error.contrasenaLetraNumero':
      'The password must have at least one letter and one number.',
    'cuentas.error.repiteContrasena': 'Repeat the password.',
    'cuentas.error.noCoinciden': "The passwords don't match.",
    'cuentas.error.escribeWhatsapp': 'Enter your WhatsApp number.',
    'cuentas.error.whatsappInvalido':
      'Enter the number with the country code, for example +573001234567.',
    'cuentas.error.nombreLargo': 'The name can be up to 100 characters long.',

    'cuentas.login.titulo': 'Log in',
    'cuentas.login.subtitulo': 'Log in to manage your requests, reviews and data.',
    'cuentas.login.recordar': 'Remember me',
    'cuentas.login.olvido': 'Forgot your password?',
    'cuentas.login.entrar': 'Log in',
    'cuentas.login.entrando': 'Logging in…',
    'cuentas.login.sinCuenta': "Don't have an account?",
    'cuentas.login.crear': 'Create one',
    'cuentas.login.errorCorreo': 'Enter your email.',
    'cuentas.login.errorContrasena': 'Enter your password.',

    'cuentas.recuperar.titulo': 'Recover access',
    'cuentas.recuperar.subtitulo':
      "Enter your account email and we'll send you a link to choose a new password.",
    'cuentas.recuperar.confirmacionAntes': 'If there is an active account with',
    'cuentas.recuperar.confirmacionDespues': ", we've sent you a link to choose a new password.",
    'cuentas.recuperar.nota1': 'The link expires after 30 minutes and works only once.',
    'cuentas.recuperar.nota2': "If you don't see it, check your spam folder.",
    'cuentas.recuperar.nota3': 'If you request another link, the previous one stops working.',
    'cuentas.recuperar.volver': 'Back to log in',
    'cuentas.recuperar.pedirOtro': 'Request another link',
    'cuentas.recuperar.errorCorreo': 'Enter your account email.',
    'cuentas.recuperar.enviando': 'Sending…',
    'cuentas.recuperar.enviar': 'Send the link',
    'cuentas.recuperar.recuerdas': 'Remembered it?',
    'cuentas.recuperar.inicia': 'Log in',

    'cuentas.restablecer.titulo': 'Choose a new password',
    'cuentas.restablecer.subtitulo': 'Type it twice. Then you will need to log in with it.',
    'cuentas.restablecer.sinCodigo':
      "This link isn't valid: it's missing the code from the email. Request a new one.",
    'cuentas.restablecer.vencido':
      'The link expired or was already used. Links are valid for 30 minutes and work only once.',
    'cuentas.restablecer.pedirNuevo': 'Request a new link',
    'cuentas.restablecer.guardando': 'Saving…',
    'cuentas.restablecer.cambiar': 'Change password',
    'cuentas.restablecer.volver': 'Back to log in',
    'cuentas.restablecer.aviso': 'You changed your password. Log in with the new one.',

    'cuentas.registro.titulo': 'Create an account',
    'cuentas.registro.subtitulo':
      'Sign up as a tourist to request activities, or as a provider to publish yours.',
    'cuentas.registro.obligatoriosAntes': 'Fields marked with an',
    'cuentas.registro.obligatoriosDespues': 'are required.',
    'cuentas.registro.tipoCuenta': 'What type of account do you need?',
    'cuentas.registro.descTurista': 'I want to discover and request plans in Santa Marta.',
    'cuentas.registro.descPrestador':
      'I offer activities, accommodation or a restaurant and want to publish them.',
    'cuentas.registro.nombreCompleto': 'Full name',
    'cuentas.registro.nombreComercial': 'Business name',
    'cuentas.registro.ayudaNombreComercial':
      'As it appears in your RNT (National Tourism Registry).',
    'cuentas.registro.recuperar': 'Reset password',
    'cuentas.registro.ayudaWhatsapp':
      'Include the country code, for example +573001234567. It is the only channel we use to notify you about your requests; we will send you a code to verify it.',
    'cuentas.registro.consentimiento':
      'I authorize the processing of my personal data in accordance with the Data Processing Policy and Law 1581 of 2012. I understand my rights as a data subject and that I can access, update or request deletion of my data.',
    'cuentas.registro.leerPolitica': 'Read the policy (version {version})',
    'cuentas.registro.cargandoPolitica': 'Loading the policy…',
    'cuentas.registro.creando': 'Creating your account…',
    'cuentas.registro.crear': 'Create account',
    'cuentas.registro.yaTienes': 'Already have an account?',
    'cuentas.registro.inicia': 'Log in',
    'cuentas.registro.sinPolitica':
      "We couldn't load the data processing policy you need to accept. Try again before creating your account.",
    'cuentas.registro.errorNombreTurista': 'Enter your full name.',
    'cuentas.registro.errorNombrePrestador': 'Enter your business name.',
    'cuentas.registro.errorCorreo': 'Enter your email.',
    'cuentas.registro.errorCorreoLargo': 'The email can be up to 254 characters long.',
    'cuentas.registro.errorAcepta':
      'You must accept the data processing policy to create your account.',
    'cuentas.registro.avisoTurista':
      'Account created. We sent you a WhatsApp code to verify your number.',
    'cuentas.registro.avisoPrestador': 'Account created. Welcome to Samarian Tour.',
    'cuentas.registro.avisoSinSesion': 'Account created. Log in to continue.',

    'cuentas.politica.titulo': 'Data processing policy',
    'cuentas.politica.version': 'Version {version}',
    'cuentas.politica.region': 'Policy text',
    'cuentas.politica.cargando': 'Loading the text…',

    'cuentas.misDatos.rotulo': 'My account',
    'cuentas.misDatos.titulo': 'My data and data subject rights',
    'cuentas.misDatos.intro':
      'View and control your personal information. Samarian Tour processes your data in accordance with Law 1581 of 2012 on data protection.',
    'cuentas.misDatos.seccion': 'My data',
    'cuentas.misDatos.editar': 'Edit my data',
    'cuentas.misDatos.correo': 'Email',
    'cuentas.misDatos.correoFijo': "Your email can't be changed.",
    'cuentas.misDatos.verificado': 'Verified',
    'cuentas.misDatos.sinVerificar': 'Not verified',
    'cuentas.misDatos.tipoCuenta': 'Account type',
    'cuentas.misDatos.ayudaWhatsapp':
      'Include the country code, for example +573001234567. If you change it, we will send you a code to verify the new number.',
    'cuentas.misDatos.guardando': 'Saving…',
    'cuentas.misDatos.guardar': 'Save changes',
    'cuentas.misDatos.errorNombre': 'Enter your name.',
    'cuentas.misDatos.guardado': 'We saved your data.',
    'cuentas.misDatos.guardadoConCodigo':
      'We saved your data. We sent you a WhatsApp code to verify your new number.',
    'cuentas.misDatos.conflicto': 'Your data changed while you were editing it. Please try again.',
    'cuentas.misDatos.cargando': 'Loading your data…',
    'cuentas.misDatos.derechosTitulo': 'Your rights',
    'cuentas.misDatos.derechosIntro':
      'As the owner of your data you can exercise these rights whenever you want. These are the ones you can already use in the app and the ones you cannot yet.',
    'cuentas.misDatos.acceso': 'Access',
    'cuentas.misDatos.accesoDesc': 'Find out what data we have about you and how we use it.',
    'cuentas.misDatos.verDatos': 'View my data',
    'cuentas.misDatos.rectificacion': 'Rectification',
    'cuentas.misDatos.rectificacionDesc': 'Correct inaccurate or incomplete data.',
    'cuentas.misDatos.actualizar': 'Update',
    'cuentas.misDatos.supresion': 'Deletion',
    'cuentas.misDatos.supresionDesc': 'Ask us to delete your data when applicable.',
    'cuentas.misDatos.solicitar': 'Request',
    'cuentas.misDatos.cuentaInterna': 'Your account is internal: an administrator manages it.',
    'cuentas.misDatos.oposicion': 'Objection',
    'cuentas.misDatos.oposicionDesc': 'Object to certain uses of your data.',
    'cuentas.misDatos.portabilidad': 'Portability',
    'cuentas.misDatos.portabilidadDesc': 'Receive a copy of your data in a file.',
    'cuentas.misDatos.revocar': 'Withdraw consent',
    'cuentas.misDatos.revocarDesc':
      'Withdraw the permission you gave for the processing of your data.',
    'cuentas.misDatos.noDisponible': 'This right cannot be exercised from the app yet.',
    'cuentas.misDatos.vigenteAntes': 'Current version:',
    'cuentas.misDatos.vigenteDespues': ', published on {fecha}.',
    'cuentas.misDatos.leerPolitica': 'Read the policy',
    'cuentas.misDatos.sinPolitica': "We couldn't load the policy right now.",

    'cuentas.verificar.titulo': 'Verify your WhatsApp',
    'cuentas.verificar.enviadoAl': 'We sent a 6-digit code to',
    'cuentas.verificar.enviadoSinNumero': 'We sent a 6-digit code to your WhatsApp.',
    'cuentas.verificar.aviso':
      "You won't be able to send requests until you verify it. The code is valid for 30 minutes.",
    'cuentas.verificar.reenviado': 'We sent you a new code. The previous one no longer works.',
    'cuentas.verificar.etiqueta': '6-digit code',
    'cuentas.verificar.verificando': 'Verifying…',
    'cuentas.verificar.verificar': 'Verify',
    'cuentas.verificar.enviando': 'Sending…',
    'cuentas.verificar.otro': 'Send another code',
    'cuentas.verificar.errorVacio': 'Enter the code you received.',
    'cuentas.verificar.errorFormato': 'The code has 6 digits, numbers only.',
    'cuentas.verificar.agotado':
      "The code isn't correct and no more attempts are allowed. Request a new one.",
    'cuentas.verificar.intentos': {
      uno: "The code isn't correct. You have {n} attempt left.",
      otros: "The code isn't correct. You have {n} attempts left.",
    },
    'cuentas.verificar.verificado': 'You verified your WhatsApp',

    'cuentas.eliminar.titulo': 'Delete my account',
    'cuentas.eliminar.prestador1':
      "We will delete your personal data, your WhatsApp and your authorization, and close your sessions. Your offers are withdrawn permanently, and your profile and photos are removed from the catalog. Your open requests that haven't been paid are closed.",
    'cuentas.eliminar.prestador2':
      'The RNT, the business name, your billing accounts and your payouts are kept as accounting records, but the RNT is freed up to be registered on another account. If you still have paid services to deliver or money to receive, you must resolve them first.',
    'cuentas.eliminar.turista1':
      'We will delete your name, email, password and WhatsApp, close your sessions and remove your reviews from the catalog. Your requests are kept only with an identifier, without your data.',
    'cuentas.eliminar.turista2':
      'Activities already completed may be kept in anonymized form because of legal and accounting obligations. You will be able to create a new account with the same email.',
    'cuentas.eliminar.advertencia': 'This action is immediate and cannot be undone.',
    'cuentas.eliminar.dialogoTitulo': 'Delete your account?',
    'cuentas.eliminar.dialogoAntes':
      'You will lose access and your personal data will be deleted right now.',
    'cuentas.eliminar.dialogoNegrita': 'This cannot be undone.',
    'cuentas.eliminar.dialogoDespues': 'Enter your password to confirm.',
    'cuentas.eliminar.tuContrasena': 'Your password',
    'cuentas.eliminar.errorContrasena': 'Enter your password to confirm.',
    'cuentas.eliminar.eliminando': 'Deleting…',
    'cuentas.eliminar.interna':
      'Your account is internal: an administrator manages it. It cannot be deleted from here.',
    'cuentas.eliminar.aviso': 'We deleted your personal data. Thank you for using Samarian Tour.',

    'cuentas.sinPermiso.titulo': "You don't have permission to view this page",
    'cuentas.sinPermiso.entraste': 'You logged in as',
    'cuentas.sinPermiso.explicacion':
      ', and this section belongs to a different account type. If you think this is a mistake, log in with the correct account.',
    'cuentas.sinPermiso.irInicio': 'Go to my home',
    'cuentas.sinPermiso.otraCuenta': 'Log in with another account',
  },
);
