import { diccionario } from './diccionario';

/**
 * Textos del panel del prestador: la barra lateral, "Mis ofertas", el detalle de una oferta y el formulario para publicarla
 * o editarla, con su selector de fotos. Los textos que comparten otras pantallas (nombre, descripción, zona, tipo de
 * actividad…) se reutilizan de sus áreas.
 *
 * Lo que escribe el prestador (nombre, descripción, horarios…) y lo que escribe el administrador (el motivo de un rechazo)
 * se muestra tal cual.
 */
export const PRESTADOR = diccionario(
  {
    // Barra lateral
    'prestador.panel.control': 'Panel de control',
    'prestador.panel.rntPorRegistrar': 'RNT por registrar',
    'prestador.panel.sinRnt':
      'Todavía no registras tu RNT y tu tipo de prestador, así que no puedes publicar ofertas. Hazlo en la sección RNT.',

    // Dashboard (P03.0)
    'prestador.inicio.titulo': 'Hola, {nombre}',
    'prestador.inicio.subtitulo': 'Este es el resumen de tu registro y de tus ofertas.',
    'prestador.inicio.cargando': 'Cargando…',
    'prestador.inicio.irARnt': 'Ir a la sección RNT',
    'prestador.inicio.aviso.sinRegistrar.titulo': 'Registra tu RNT para publicar',
    'prestador.inicio.aviso.sinRegistrar.texto':
      'Mientras no registres tu RNT (Registro Nacional de Turismo) no puedes publicar ofertas, y ninguna se muestra en el catálogo.',
    'prestador.inicio.aviso.inactivo.titulo': 'Tu RNT está inactivo',
    'prestador.inicio.aviso.inactivo.texto':
      'Tus ofertas no se muestran en el catálogo mientras tu RNT esté inactivo. Cuando vuelva a estar activo se verán de nuevo. Revisa el número y su estado en la sección RNT.',
    'prestador.inicio.aviso.noEncontrado.titulo': 'No encontramos tu RNT',
    'prestador.inicio.aviso.noEncontrado.texto':
      'Tus ofertas no se muestran en el catálogo porque no encontramos ese número en el registro oficial. Revisa que esté bien escrito o cámbialo en la sección RNT.',
    'prestador.inicio.aviso.pendiente.titulo': 'Estamos verificando tu RNT',
    'prestador.inicio.aviso.pendiente.texto':
      'El registro oficial no respondió a tiempo. Lo revisaremos de nuevo en la siguiente sincronización; no tienes que hacer nada.',
    'prestador.inicio.registro.titulo': 'Tu registro',
    'prestador.inicio.registro.estado': 'Estado del RNT',
    'prestador.inicio.registro.sinRegistrar': 'Sin registrar',
    'prestador.inicio.registro.numero': 'Número de RNT',
    'prestador.inicio.registro.razonSocial': 'Razón social',
    'prestador.inicio.registro.tipo': 'Tipo de prestador',
    'prestador.inicio.registro.sincronizado': 'Última sincronización',
    'prestador.inicio.registro.nuncaSincronizado': 'Aún no ha respondido',
    'prestador.inicio.registro.whatsapp': 'WhatsApp de contacto',
    'prestador.inicio.whatsapp.verificado': 'Verificado',
    'prestador.inicio.whatsapp.sinVerificar': 'Sin verificar',
    'prestador.inicio.whatsapp.sinConfigurar': 'Sin configurar',
    'prestador.inicio.whatsapp.noDisponible': 'Registra primero tu RNT',
    'prestador.inicio.ofertas.titulo': 'Tus ofertas',
    'prestador.inicio.ofertas.total': { uno: '{n} oferta en total', otros: '{n} ofertas en total' },
    'prestador.inicio.accesos.titulo': 'Configura tu cuenta',
    'prestador.inicio.accesos.rnt':
      'Registra o cambia tu RNT y tu tipo de prestador, y mira cómo quedó la validación.',
    'prestador.inicio.accesos.perfil': 'Cuenta quién eres y muestra fotos de tu negocio a los turistas.',
    'prestador.inicio.accesos.whatsapp':
      'El número donde recibes los avisos de las solicitudes. Nunca se muestra al público.',
    'prestador.inicio.accesos.cuentaCobro':
      'La cuenta donde recibes lo que te corresponde por cada pago. La comisión se descuenta de lo que recibes; nunca se suma al turista.',

    // RNT y tipo de prestador (P03.1)
    'prestador.rnt.intro':
      'Tu RNT (Registro Nacional de Turismo) muestra que eres un prestador formal. Sin un RNT activo no puedes publicar ofertas y las que tengas no se muestran en el catálogo.',
    'prestador.rnt.cargando': 'Cargando tu RNT…',
    'prestador.rnt.estado.titulo': 'Validación de tu RNT',
    'prestador.rnt.activo': 'Tu RNT está activo y vigente. Puedes publicar ofertas.',
    'prestador.rnt.municipio': 'Municipio',
    'prestador.rnt.categoria': 'Categoría',
    'prestador.rnt.categoria.GUIA_INDEPENDIENTE': 'Guía independiente',
    'prestador.rnt.categoria.MICRO_PEQUENO': 'Micro o pequeña empresa',
    'prestador.rnt.categoria.MEDIANO': 'Mediana empresa',
    'prestador.rnt.categoria.GRANDE': 'Gran empresa',
    'prestador.rnt.publicar': 'Publicar ofertas',
    'prestador.rnt.publicarSi': 'Sí, puedes publicar',
    'prestador.rnt.publicarNo': 'Todavía no',
    'prestador.rnt.registrar.titulo': 'Registra tu RNT',
    'prestador.rnt.registrar.texto':
      'Escribe el número de tu certificado y elige tu tipo de prestador. Lo validamos contra el registro oficial; puede tardar unos segundos.',
    'prestador.rnt.registrar.boton': 'Validar y registrar',
    'prestador.rnt.cambiar.titulo': 'Cambiar el número de RNT',
    'prestador.rnt.cambiar.texto':
      'Lo validamos de nuevo contra el registro oficial. Si con el número nuevo cambia si puedes publicar, tus ofertas se mostrarán o se ocultarán en el catálogo.',
    'prestador.rnt.cambiar.boton': 'Validar el número nuevo',
    'prestador.rnt.numeroNuevo': 'Número de RNT nuevo',
    'prestador.rnt.ayudaNumero': 'Solo el número, sin letras ni espacios (hasta 10 dígitos).',
    'prestador.rnt.tipo.titulo': 'Tipo de prestador',
    'prestador.rnt.tipo.texto':
      'El tipo tiene que coincidir con la categoría de tu RNT. Al guardarlo actualizamos tu sesión para que se vea al instante.',
    'prestador.rnt.tipo.boton': 'Guardar el tipo',
    'prestador.rnt.elegirTipo': 'Elige tu tipo de prestador…',
    'prestador.rnt.modalidades': 'Modalidades que ofreces',
    'prestador.rnt.idiomas': 'Idiomas en los que atiendes',
    'prestador.rnt.empleados': 'Número de empleados',
    'prestador.rnt.validando': 'Validando…',
    'prestador.rnt.puedeTardar': 'Estamos consultando el registro oficial. Esto puede tardar unos segundos.',
    'prestador.rnt.tipoGuardado': 'Guardaste tu tipo de prestador.',
    'prestador.rnt.sesionSinActualizar':
      'Guardamos los cambios, pero no pudimos actualizar tu sesión. Cierra la sesión y vuelve a entrar para que se vea tu tipo de prestador.',
    'prestador.rnt.resultado.ACTIVO': 'Validamos tu RNT: está activo. Ya puedes publicar ofertas.',
    'prestador.rnt.resultado.VERIFICACION_PENDIENTE':
      'Guardamos tu RNT, pero el registro oficial no respondió a tiempo. Lo revisaremos en la siguiente sincronización.',
    'prestador.rnt.resultado.NO_ENCONTRADO':
      'Guardamos tu RNT, pero no lo encontramos en el registro oficial. Revisa el número y cámbialo si hace falta.',
    'prestador.rnt.resultado.INACTIVO':
      'Guardamos tu RNT, pero el registro oficial lo muestra inactivo. Tus ofertas no se verán hasta que vuelva a estar activo.',
    'prestador.rnt.error.numeroRequerido': 'Escribe tu número de RNT.',
    'prestador.rnt.error.numeroInvalido': 'El número de RNT solo lleva dígitos (hasta 10).',
    'prestador.rnt.error.tipoRequerido': 'Elige tu tipo de prestador.',
    'prestador.rnt.error.empleadosRequerido': 'Escribe cuántos empleados tienes.',
    'prestador.rnt.error.empleadosRango': 'Escribe un número entero de empleados, de 1 a 100000.',
    'prestador.rnt.error.modalidadesRequerida': 'Elige al menos una modalidad.',
    'prestador.rnt.error.idiomasRequerido': 'Escribe al menos un idioma.',
    'prestador.rnt.error.idiomasInvalidos': 'Cada idioma lleva un código de dos letras, por ejemplo: es, en.',

    // WhatsApp de contacto del prestador (P03.4)
    'prestador.whatsapp.intro':
      'Es tu canal de contacto: por aquí te avisamos cuando una solicitud queda pagada. Nunca se muestra al público ni a los turistas.',
    'prestador.whatsapp.cargando': 'Cargando tu WhatsApp…',
    'prestador.whatsapp.sinRnt.titulo': 'Primero registra tu RNT',
    'prestador.whatsapp.sinRnt.texto':
      'El WhatsApp de contacto se guarda en tu registro de prestador, que se crea al registrar tu RNT.',
    'prestador.whatsapp.numero.titulo': 'Tu número',
    'prestador.whatsapp.verificado':
      'Tu WhatsApp está verificado. Te avisaremos por aquí cuando una solicitud quede pagada.',
    'prestador.whatsapp.sinConfigurar':
      'Todavía no configuraste tu WhatsApp. Sin él no recibes el aviso de una solicitud pagada.',
    'prestador.whatsapp.guardar': 'Guardar número',
    'prestador.whatsapp.guardado': 'Guardaste tu WhatsApp.',
    'prestador.whatsapp.guardadoSinVerificar':
      'Guardaste tu WhatsApp. Te enviamos un código por WhatsApp para verificarlo.',
    'prestador.whatsapp.avisoVerificar':
      'Mientras no lo verifiques no recibirás el aviso de una solicitud pagada. El código vale 30 minutos.',

    // Cuenta de cobro (P03.5)
    'prestador.cobro.intro':
      'Aquí recibes lo que te corresponde por cada pago. La registras una sola vez, no en cada pago.',
    'prestador.cobro.comision.titulo': 'Sobre la comisión',
    'prestador.cobro.comision.texto':
      'La comisión de la plataforma se descuenta de lo que recibes; nunca se le suma al turista.',
    'prestador.cobro.cargando': 'Cargando tu cuenta de cobro…',
    'prestador.cobro.sinRnt.titulo': 'Primero registra tu RNT',
    'prestador.cobro.sinRnt.texto':
      'La cuenta de cobro se guarda en tu registro de prestador, que se crea al registrar tu RNT.',
    'prestador.cobro.vigente.titulo': 'Tu cuenta de cobro vigente',
    'prestador.cobro.vigente.tipo': 'Tipo de cuenta',
    'prestador.cobro.vigente.entidad': 'Banco',
    'prestador.cobro.vigente.numero': 'Número',
    'prestador.cobro.vigente.titular': 'Titular',
    'prestador.cobro.vigente.desde': 'Vigente desde',
    'prestador.cobro.terminaEn': 'Termina en {digitos}',
    'prestador.cobro.sinCuenta':
      'Todavía no registraste tu cuenta de cobro. Sin ella no podemos transferirte lo que te corresponde.',
    'prestador.cobro.registrar.titulo': 'Registra tu cuenta de cobro',
    'prestador.cobro.registrar.texto': 'Elige dónde quieres recibir tus pagos.',
    'prestador.cobro.cambiar.titulo': 'Cambiar la cuenta de cobro',
    'prestador.cobro.cambiar.texto':
      'La cuenta anterior queda en el historial. Por seguridad el número completo no se vuelve a mostrar: escríbelo otra vez para guardar cualquier cambio.',
    'prestador.cobro.tipo.AHORROS': 'Cuenta de ahorros',
    'prestador.cobro.tipo.CORRIENTE': 'Cuenta corriente',
    'prestador.cobro.tipo.NEQUI': 'Nequi',
    'prestador.cobro.tipo.DAVIPLATA': 'Daviplata',
    'prestador.cobro.form.tipo': 'Tipo de cuenta',
    'prestador.cobro.form.elegirTipo': 'Elige el tipo…',
    'prestador.cobro.form.entidad': 'Banco',
    'prestador.cobro.form.ayudaEntidad': 'Elige uno de la lista o escríbelo.',
    'prestador.cobro.form.numeroCuenta': 'Número de cuenta',
    'prestador.cobro.form.celular': 'Número de celular',
    'prestador.cobro.form.ayudaNumeroCuenta': 'De 6 a 20 dígitos, sin letras.',
    'prestador.cobro.form.ayudaCelular': 'Tu celular de 10 dígitos, que empieza por 3.',
    'prestador.cobro.form.ayudaPrivado': 'Lo guardamos cifrado y después solo mostramos los últimos 4 dígitos.',
    'prestador.cobro.form.titular': 'Titular',
    'prestador.cobro.form.ayudaTitular': 'El nombre como aparece en la cuenta.',
    'prestador.cobro.form.guardar': 'Guardar cuenta',
    'prestador.cobro.guardada': 'Guardaste tu cuenta de cobro.',
    'prestador.cobro.error.tipoRequerido': 'Elige el tipo de cuenta.',
    'prestador.cobro.error.entidadRequerida': 'Elige o escribe el banco.',
    'prestador.cobro.error.entidadLarga': 'El banco puede tener hasta 100 caracteres.',
    'prestador.cobro.error.numeroRequerido': 'Escribe el número.',
    'prestador.cobro.error.numeroBancoInvalido': 'El número de cuenta tiene de 6 a 20 dígitos.',
    'prestador.cobro.error.numeroCelularInvalido': 'El celular tiene 10 dígitos y empieza por 3.',
    'prestador.cobro.error.titularRequerido': 'Escribe el nombre del titular.',
    'prestador.cobro.error.titularLargo': 'El nombre puede tener hasta 100 caracteres.',

    // Perfil público del prestador (P03.2)
    'prestador.perfil.intro':
      'Esta información aparece en tu perfil público y ayuda a los turistas a conocerte. Se publica de inmediato, sin moderación.',
    'prestador.perfil.cargando': 'Cargando tu perfil…',
    'prestador.perfil.datos.titulo': 'Datos del perfil',
    'prestador.perfil.sinPerfil':
      'Todavía no tienes perfil público. Escribe la descripción y guárdalo para publicarlo.',
    'prestador.perfil.opcional': '(opcional)',
    'prestador.perfil.nombreComercial': 'Nombre comercial',
    'prestador.perfil.descripcion': 'Descripción',
    'prestador.perfil.ayudaDescripcion': 'Cuenta en pocas líneas quién eres y qué ofreces.',
    'prestador.perfil.horarios': 'Horario de atención',
    'prestador.perfil.ayudaHorarios': 'Por ejemplo: lunes a sábado, de 8:00 a. m. a 6:00 p. m.',
    'prestador.perfil.sitioWeb': 'Sitio web',
    'prestador.perfil.ayudaEnlace': 'Una dirección que empiece por http:// o https://.',
    'prestador.perfil.redes': 'Redes sociales',
    'prestador.perfil.ayudaRedes': 'Una dirección por línea, que empiece por http:// o https://.',
    'prestador.perfil.guardar': 'Guardar perfil',
    'prestador.perfil.creado': 'Creaste tu perfil público. Ya lo ven los turistas.',
    'prestador.perfil.guardado': 'Guardaste tu perfil público.',
    'prestador.perfil.error.descripcionRequerida': 'Escribe la descripción de tu perfil.',
    'prestador.perfil.error.sitioInvalido': 'Escribe una dirección web que empiece por http:// o https://.',
    'prestador.perfil.error.redesInvalidas':
      'Cada red social va en su propia línea y empieza por http:// o https://.',
    'prestador.perfil.fotos.titulo': 'Fotos del perfil',
    'prestador.perfil.fotos.primeroGuarda': 'Guarda primero los datos del perfil para poder agregar fotos.',
    'prestador.perfil.fotos.leyenda': 'Fotos del perfil',
    'prestador.perfil.fotos.ayuda':
      'Puedes tener hasta {maximo} fotos en JPG, PNG o WebP de hasta 5 MB. Cada foto se sube y se quita al instante; no hace falta guardar.',
    'prestador.perfil.fotos.alt': 'Foto {indice} de {total} del perfil',
    'prestador.perfil.fotos.agregada': 'Foto agregada. El perfil tiene {n} de {maximo} fotos.',
    'prestador.perfil.fotos.quitada': 'Foto quitada. El perfil tiene {n} de {maximo} fotos.',
    'prestador.perfil.fotos.quitadaSinFotos': 'Foto quitada. El perfil no tiene fotos.',
    'prestador.perfil.fotos.motivoMaximo': 'el perfil lleva máximo {maximo} fotos',

    // Mis ofertas
    'prestador.ofertas.subtitulo':
      'Cada oferta nueva o editada pasa por moderación antes de verse en el catálogo.',
    'prestador.ofertas.caption': 'Ofertas del prestador, {n} en total',
    'prestador.ofertas.colNombre': 'Nombre de la oferta',
    'prestador.ofertas.colTipo': 'Tipo',
    'prestador.ofertas.colPrecio': 'Precio ref.',
    'prestador.ofertas.colEstado': 'Estado',
    'prestador.ofertas.colAcciones': 'Acciones',
    'prestador.ofertas.motivo': 'Motivo: {motivo}',
    'prestador.ofertas.sinMotivo': 'no se indicó',
    'prestador.ofertas.sigueVisible': 'Tu versión publicada sigue visible en el catálogo.',
    'prestador.ofertas.verDetalle': 'Ver detalle',
    'prestador.ofertas.deLaOferta': 'de {nombre}',
    'prestador.ofertas.editar': 'Editar',
    'prestador.ofertas.retirar': 'Retirar',
    'prestador.ofertas.vacioTitulo': 'Todavía no tienes ofertas',
    'prestador.ofertas.vacioAyuda': 'Publica la primera y la enviaremos a moderación.',
    'prestador.ofertas.cargando': 'Cargando tus ofertas…',
    'prestador.ofertas.sinNombre': 'Oferta sin nombre',
    'prestador.ofertas.retiroTitulo': '¿Retirar esta oferta?',
    'prestador.ofertas.retiroMensaje':
      '«{nombre}» dejará de verse en el catálogo. El retiro es definitivo: después no podrás editarla ni volver a publicarla.',
    'prestador.ofertas.retiroAccion': 'Retirar oferta',
    'prestador.ofertas.retirada': 'Retiramos «{nombre}» del catálogo.',

    // Detalle de una oferta
    'prestador.detalle.noEncontrada': 'No encontramos esta oferta entre las tuyas',
    'prestador.detalle.noEncontradaAyuda':
      'Puede que no sea tuya, o que ya no exista. Revisa la lista de tus ofertas.',
    'prestador.detalle.verMis': 'Ver mis ofertas',
    'prestador.detalle.versionPublicada': 'Versión publicada',
    'prestador.detalle.vacioPublicada': 'Todavía no tiene una versión publicada.',
    'prestador.detalle.versionPendiente': 'Versión pendiente',
    'prestador.detalle.vacioPendiente': 'No tiene cambios en revisión.',
    'prestador.detalle.numero': 'Número de versión',
    'prestador.detalle.moderacion': 'Estado de moderación',
    'prestador.detalle.enviada': 'Enviada a moderación',
    'prestador.detalle.revisada': 'Revisada',
    'prestador.detalle.aunNo': 'Aún no',
    'prestador.detalle.motivoRechazo': 'Motivo del rechazo',
    'prestador.detalle.nombre': 'Nombre',
    'prestador.detalle.precio': 'Precio de referencia',
    'prestador.detalle.capacidad': 'Capacidad máxima',
    'prestador.detalle.descripcion': 'Descripción',
    'prestador.detalle.fotos': 'Fotos',
    'prestador.detalle.fotosN': { uno: '{n} foto', otros: '{n} fotos' },
    'prestador.detalle.sinFotos': 'Sin fotos',
    'prestador.detalle.corregir': 'Corregir la versión rechazada',
    'prestador.detalle.verEnCatalogo': 'Ver en el catálogo',

    // Formulario: encabezado y estados
    'prestador.form.alGuardar': 'Al guardar, la oferta va a moderación.',
    'prestador.form.versionSigue':
      'La versión publicada sigue visible mientras se revisan los cambios.',
    'prestador.form.cargando': 'Cargando el formulario…',
    'prestador.form.volver': 'Volver a mis ofertas',
    'prestador.form.cambiosEnRevision':
      'Esta oferta ya tiene cambios en revisión (versión {numero}). El formulario parte de esa versión; al guardar, la versión nueva reemplaza a la que está pendiente y la publicada sigue visible.',
    'prestador.form.primeraEnRevision':
      'La primera versión de esta oferta (versión {numero}) todavía está en moderación. El formulario parte de ella; al guardar, la versión nueva la reemplaza.',
    'prestador.form.rechazada':
      'Rechazaron la versión {numero} de esta oferta. Motivo: {motivo}. El formulario parte de esa versión: corrígela y envíala de nuevo para que vuelva a moderación.',
    'prestador.form.sinTipo':
      'Todavía no registras tu RNT y tu tipo de prestador, así que no puedes publicar ni editar ofertas. Esa pantalla llegará en una próxima fase.',
    'prestador.form.noPropia': 'No encontramos esta oferta entre las tuyas.',

    // Formulario: datos generales
    'prestador.form.generales': 'Datos generales',
    'prestador.form.unicoTipo': '(el único que publica tu tipo de prestador)',
    'prestador.form.precio': 'Precio de referencia (COP)',
    'prestador.form.ayudaPrecio': 'Se muestra {unidad}. Es informativo.',
    'prestador.form.capacidad': 'Capacidad máxima',
    'prestador.form.ayudaCapacidad': 'Personas por solicitud.',
    'prestador.form.eligeZona': 'Elige una zona',
    'prestador.form.horarios': 'Horarios',
    'prestador.form.ayudaHorarios':
      'Días o franja de operación, por ejemplo “Todos los días, 8:00 a 17:00”.',
    'prestador.form.fotos': 'Fotos',

    // Formulario: actividad
    'prestador.form.actividad': 'Datos de la actividad',
    'prestador.form.ayudaGuia': 'Como guía independiente debes completar los datos del recorrido.',
    'prestador.form.eligeTipo': 'Elige un tipo',
    'prestador.form.eligeNivel': 'Elige un nivel',
    'prestador.form.ayudaHoras': 'Horas en formato de 24 horas, separadas por comas: 07:30, 14:00.',
    'prestador.form.duracion': 'Duración en minutos',
    'prestador.form.modalidad': 'Modalidad del recorrido',
    'prestador.form.sinDefinir': 'Sin definir',
    'prestador.form.idiomas': 'Idiomas de atención',
    'prestador.form.ayudaIdiomas': 'Códigos de dos letras separados por comas: es, en.',
    'prestador.form.actividadSinNombre': 'Actividad sin nombre',

    // Formulario: paquete, alojamiento, ubicación
    'prestador.form.paquete': 'Datos del paquete',
    'prestador.form.ayudaActividades':
      'Elige {minimo} o más de tus actividades. El esfuerzo físico y la accesibilidad del paquete salen de ellas.',
    'prestador.form.sinAprobar': '(aún sin aprobar)',
    'prestador.form.sinActividades':
      'No tienes actividades para incluir. Publica primero al menos {minimo} actividades.',
    'prestador.form.alojamiento': 'Datos del alojamiento',
    'prestador.form.ayudaUnidad': 'Por ejemplo: cabaña, habitación doble, hamaca.',
    'prestador.form.opcional': '(opcional)',
    'prestador.form.ayudaMapa':
      'Haz clic en el mapa para poner el pin donde está la oferta. Para moverlo, haz clic en otro punto.',
    'prestador.form.mapa': 'Mapa para elegir la ubicación de la oferta',
    'prestador.form.latitud': 'Latitud',
    'prestador.form.longitud': 'Longitud',
    'prestador.form.sinUbicacion': 'Sin ubicación',
    'prestador.form.quitarUbicacion': 'Quitar ubicación',
    'prestador.form.comoLlegar': 'Cómo llegar',

    // Formulario: envío y errores
    'prestador.form.revision':
      'Un administrador revisa la oferta antes de que se vea en el catálogo.',
    'prestador.form.enviando': 'Enviando…',
    'prestador.form.enviar': 'Enviar a moderación',
    'prestador.form.enviada': 'Enviamos tu oferta a moderación',
    'prestador.form.enviadaConVersion': 'Enviamos tu oferta a moderación (versión {numero})',
    'prestador.form.faltaFoto': 'Agrega al menos una foto: la primera es la portada.',
    'prestador.form.esperaFotos':
      'Espera a que terminen de subir las fotos y vuelve a enviar la oferta.',
    'prestador.form.conDetalle': '{mensaje} Detalle: {detalle}.',
    'prestador.form.errorObligatorio': 'Este dato es obligatorio.',
    'prestador.form.errorMinimo': 'Debe ser {minimo} o más.',
    'prestador.form.errorEntero': 'Debe ser un número entero.',
    'prestador.form.errorHoras': 'Escribe cada hora con el formato 07:30 y sepáralas con comas.',
    'prestador.form.errorIdiomas':
      'Usa códigos de dos letras en minúscula, como es o en, separados por comas.',
    'prestador.form.errorActividades': 'Elige al menos {minimo} actividades.',
    'prestador.form.errorRevisa': 'Revisa este dato.',

    // Selector de fotos
    'prestador.fotos.leyenda': 'Fotos de la oferta',
    'prestador.fotos.ayuda':
      'Agrega de 1 a {maximo} fotos en JPG, PNG o WebP de hasta 5 MB. La primera es la portada: para cambiarla, usa «Mover antes» en otra foto.',
    'prestador.fotos.agregar': 'Agregar fotos',
    'prestador.fotos.contador': '{n} de {maximo}',
    'prestador.fotos.llegoAlMaximo': 'Llegaste al máximo de {maximo} fotos.',
    'prestador.fotos.portada': 'Portada',
    'prestador.fotos.moverAntes': 'Mover antes',
    'prestador.fotos.moverDespues': 'Mover después',
    'prestador.fotos.quitar': 'Quitar',
    'prestador.fotos.laFoto': 'la foto {n}',
    'prestador.fotos.subiendo': 'Subiendo…',
    'prestador.fotos.reintentar': 'Reintentar',
    'prestador.fotos.laSubida': 'la subida de {nombre}',
    'prestador.fotos.descartar': 'Descartar',
    'prestador.fotos.alt': 'Foto {indice} de {total} de la oferta',
    'prestador.fotos.altPortada': 'Foto {indice} de {total} de la oferta (portada)',
    'prestador.fotos.movida': 'Foto movida a la posición {posicion} de {total}.',
    'prestador.fotos.ahoraPortada': 'Ahora es la portada.',
    'prestador.fotos.quitadaSinFotos': 'Foto quitada. La oferta no tiene fotos.',
    'prestador.fotos.quitada': 'Foto quitada. La oferta tiene {n} de {maximo} fotos.',
    'prestador.fotos.agregada': 'Foto agregada. La oferta tiene {n} de {maximo} fotos.',
    'prestador.fotos.noSubido': 'No se subió «{nombre}»: {motivo}.',
    'prestador.fotos.motivoFormato': 'solo se aceptan fotos JPG, PNG o WebP',
    'prestador.fotos.motivoVacio': 'el archivo está vacío',
    'prestador.fotos.motivoPesa': 'pesa más de 5 MB',
    'prestador.fotos.motivoMaximo': 'una oferta lleva máximo {maximo} fotos',
  },
  {
    'prestador.panel.control': 'Control panel',
    'prestador.panel.rntPorRegistrar': 'RNT not registered yet',
    'prestador.panel.sinRnt':
      "You haven't registered your RNT and provider type yet, so you can't publish offers. Do it in the RNT section.",

    // Dashboard (P03.0)
    'prestador.inicio.titulo': 'Hello, {nombre}',
    'prestador.inicio.subtitulo': 'This is a summary of your registration and your offers.',
    'prestador.inicio.cargando': 'Loading…',
    'prestador.inicio.irARnt': 'Go to the RNT section',
    'prestador.inicio.aviso.sinRegistrar.titulo': 'Register your RNT to publish',
    'prestador.inicio.aviso.sinRegistrar.texto':
      "Until you register your RNT (National Tourism Registry) you can't publish offers, and none are shown in the catalog.",
    'prestador.inicio.aviso.inactivo.titulo': 'Your RNT is inactive',
    'prestador.inicio.aviso.inactivo.texto':
      'Your offers are not shown in the catalog while your RNT is inactive. They will appear again once it is active. Check the number and its status in the RNT section.',
    'prestador.inicio.aviso.noEncontrado.titulo': "We couldn't find your RNT",
    'prestador.inicio.aviso.noEncontrado.texto':
      'Your offers are not shown in the catalog because we could not find that number in the official registry. Check that it is typed correctly or change it in the RNT section.',
    'prestador.inicio.aviso.pendiente.titulo': 'We are verifying your RNT',
    'prestador.inicio.aviso.pendiente.texto':
      "The official registry did not respond in time. We will check again at the next synchronization; you don't need to do anything.",
    'prestador.inicio.registro.titulo': 'Your registration',
    'prestador.inicio.registro.estado': 'RNT status',
    'prestador.inicio.registro.sinRegistrar': 'Not registered',
    'prestador.inicio.registro.numero': 'RNT number',
    'prestador.inicio.registro.razonSocial': 'Business name',
    'prestador.inicio.registro.tipo': 'Provider type',
    'prestador.inicio.registro.sincronizado': 'Last synchronization',
    'prestador.inicio.registro.nuncaSincronizado': 'No response yet',
    'prestador.inicio.registro.whatsapp': 'Contact WhatsApp',
    'prestador.inicio.whatsapp.verificado': 'Verified',
    'prestador.inicio.whatsapp.sinVerificar': 'Not verified',
    'prestador.inicio.whatsapp.sinConfigurar': 'Not set up',
    'prestador.inicio.whatsapp.noDisponible': 'Register your RNT first',
    'prestador.inicio.ofertas.titulo': 'Your offers',
    'prestador.inicio.ofertas.total': { uno: '{n} offer in total', otros: '{n} offers in total' },
    'prestador.inicio.accesos.titulo': 'Set up your account',
    'prestador.inicio.accesos.rnt':
      'Register or change your RNT and your provider type, and see how the validation went.',
    'prestador.inicio.accesos.perfil': 'Tell tourists who you are and show photos of your business.',
    'prestador.inicio.accesos.whatsapp':
      'The number where you receive notices about requests. It is never shown to the public.',
    'prestador.inicio.accesos.cuentaCobro':
      "The account where you receive what you are owed for each payment. The commission is deducted from what you receive and is never added to the tourist's price.",

    // RNT and provider type (P03.1)
    'prestador.rnt.intro':
      "Your RNT (National Tourism Registry) shows that you are a registered provider. Without an active RNT you can't publish offers, and the ones you have are not shown in the catalog.",
    'prestador.rnt.cargando': 'Loading your RNT…',
    'prestador.rnt.estado.titulo': 'RNT validation',
    'prestador.rnt.activo': 'Your RNT is active and valid. You can publish offers.',
    'prestador.rnt.municipio': 'Municipality',
    'prestador.rnt.categoria': 'Category',
    'prestador.rnt.categoria.GUIA_INDEPENDIENTE': 'Independent guide',
    'prestador.rnt.categoria.MICRO_PEQUENO': 'Micro or small business',
    'prestador.rnt.categoria.MEDIANO': 'Medium-sized business',
    'prestador.rnt.categoria.GRANDE': 'Large business',
    'prestador.rnt.publicar': 'Publish offers',
    'prestador.rnt.publicarSi': 'Yes, you can publish',
    'prestador.rnt.publicarNo': 'Not yet',
    'prestador.rnt.registrar.titulo': 'Register your RNT',
    'prestador.rnt.registrar.texto':
      'Enter the number on your certificate and choose your provider type. We check it against the official registry; this can take a few seconds.',
    'prestador.rnt.registrar.boton': 'Validate and register',
    'prestador.rnt.cambiar.titulo': 'Change the RNT number',
    'prestador.rnt.cambiar.texto':
      'We validate it again against the official registry. If the new number changes whether you can publish, your offers will be shown or hidden in the catalog.',
    'prestador.rnt.cambiar.boton': 'Validate the new number',
    'prestador.rnt.numeroNuevo': 'New RNT number',
    'prestador.rnt.ayudaNumero': 'Digits only, no letters or spaces (up to 10 digits).',
    'prestador.rnt.tipo.titulo': 'Provider type',
    'prestador.rnt.tipo.texto':
      'The type has to match the category of your RNT. When you save it we update your session so it shows right away.',
    'prestador.rnt.tipo.boton': 'Save the type',
    'prestador.rnt.elegirTipo': 'Choose your provider type…',
    'prestador.rnt.modalidades': 'Modalities you offer',
    'prestador.rnt.idiomas': 'Languages you speak with guests',
    'prestador.rnt.empleados': 'Number of employees',
    'prestador.rnt.validando': 'Validating…',
    'prestador.rnt.puedeTardar': 'We are checking the official registry. This can take a few seconds.',
    'prestador.rnt.tipoGuardado': 'You saved your provider type.',
    'prestador.rnt.sesionSinActualizar':
      "We saved your changes, but we couldn't update your session. Log out and log back in to see your provider type.",
    'prestador.rnt.resultado.ACTIVO': 'We validated your RNT: it is active. You can now publish offers.',
    'prestador.rnt.resultado.VERIFICACION_PENDIENTE':
      'We saved your RNT, but the official registry did not respond in time. We will check it at the next synchronization.',
    'prestador.rnt.resultado.NO_ENCONTRADO':
      'We saved your RNT, but we could not find it in the official registry. Check the number and change it if needed.',
    'prestador.rnt.resultado.INACTIVO':
      'We saved your RNT, but the official registry shows it as inactive. Your offers will not be shown until it is active again.',
    'prestador.rnt.error.numeroRequerido': 'Enter your RNT number.',
    'prestador.rnt.error.numeroInvalido': 'The RNT number has digits only (up to 10).',
    'prestador.rnt.error.tipoRequerido': 'Choose your provider type.',
    'prestador.rnt.error.empleadosRequerido': 'Enter how many employees you have.',
    'prestador.rnt.error.empleadosRango': 'Enter a whole number of employees, from 1 to 100000.',
    'prestador.rnt.error.modalidadesRequerida': 'Choose at least one modality.',
    'prestador.rnt.error.idiomasRequerido': 'Enter at least one language.',
    'prestador.rnt.error.idiomasInvalidos': 'Each language is a two-letter code, for example: es, en.',

    // Provider contact WhatsApp (P03.4)
    'prestador.whatsapp.intro':
      'This is your contact channel: this is where we notify you when a request is paid. It is never shown to the public or to tourists.',
    'prestador.whatsapp.cargando': 'Loading your WhatsApp…',
    'prestador.whatsapp.sinRnt.titulo': 'Register your RNT first',
    'prestador.whatsapp.sinRnt.texto':
      'Your contact WhatsApp is saved in your provider registration, which is created when you register your RNT.',
    'prestador.whatsapp.numero.titulo': 'Your number',
    'prestador.whatsapp.verificado':
      'Your WhatsApp is verified. We will notify you here when a request is paid.',
    'prestador.whatsapp.sinConfigurar':
      "You haven't set up your WhatsApp yet. Without it you won't get the notice of a paid request.",
    'prestador.whatsapp.guardar': 'Save number',
    'prestador.whatsapp.guardado': 'You saved your WhatsApp.',
    'prestador.whatsapp.guardadoSinVerificar':
      'You saved your WhatsApp. We sent you a code on WhatsApp to verify it.',
    'prestador.whatsapp.avisoVerificar':
      "Until you verify it you won't get the notice of a paid request. The code is valid for 30 minutes.",

    // Payout account (P03.5)
    'prestador.cobro.intro':
      'This is where you receive what you are owed for each payment. You register it once, not for every payment.',
    'prestador.cobro.comision.titulo': 'About the commission',
    'prestador.cobro.comision.texto':
      "The platform commission is deducted from what you receive; it is never added to the tourist's price.",
    'prestador.cobro.cargando': 'Loading your payout account…',
    'prestador.cobro.sinRnt.titulo': 'Register your RNT first',
    'prestador.cobro.sinRnt.texto':
      'Your payout account is saved in your provider registration, which is created when you register your RNT.',
    'prestador.cobro.vigente.titulo': 'Your current payout account',
    'prestador.cobro.vigente.tipo': 'Account type',
    'prestador.cobro.vigente.entidad': 'Bank',
    'prestador.cobro.vigente.numero': 'Number',
    'prestador.cobro.vigente.titular': 'Account holder',
    'prestador.cobro.vigente.desde': 'Current since',
    'prestador.cobro.terminaEn': 'Ends in {digitos}',
    'prestador.cobro.sinCuenta':
      "You haven't registered your payout account yet. Without it we can't transfer what you are owed.",
    'prestador.cobro.registrar.titulo': 'Register your payout account',
    'prestador.cobro.registrar.texto': 'Choose where you want to receive your payments.',
    'prestador.cobro.cambiar.titulo': 'Change the payout account',
    'prestador.cobro.cambiar.texto':
      'The previous account stays in the history. For security the full number is not shown again: type it again to save any change.',
    'prestador.cobro.tipo.AHORROS': 'Savings account',
    'prestador.cobro.tipo.CORRIENTE': 'Checking account',
    'prestador.cobro.tipo.NEQUI': 'Nequi',
    'prestador.cobro.tipo.DAVIPLATA': 'Daviplata',
    'prestador.cobro.form.tipo': 'Account type',
    'prestador.cobro.form.elegirTipo': 'Choose the type…',
    'prestador.cobro.form.entidad': 'Bank',
    'prestador.cobro.form.ayudaEntidad': 'Choose one from the list or type it.',
    'prestador.cobro.form.numeroCuenta': 'Account number',
    'prestador.cobro.form.celular': 'Mobile number',
    'prestador.cobro.form.ayudaNumeroCuenta': 'From 6 to 20 digits, no letters.',
    'prestador.cobro.form.ayudaCelular': 'Your 10-digit mobile number, starting with 3.',
    'prestador.cobro.form.ayudaPrivado': 'We store it encrypted and afterwards only show the last 4 digits.',
    'prestador.cobro.form.titular': 'Account holder',
    'prestador.cobro.form.ayudaTitular': 'The name as it appears on the account.',
    'prestador.cobro.form.guardar': 'Save account',
    'prestador.cobro.guardada': 'You saved your payout account.',
    'prestador.cobro.error.tipoRequerido': 'Choose the account type.',
    'prestador.cobro.error.entidadRequerida': 'Choose or type the bank.',
    'prestador.cobro.error.entidadLarga': 'The bank can have up to 100 characters.',
    'prestador.cobro.error.numeroRequerido': 'Enter the number.',
    'prestador.cobro.error.numeroBancoInvalido': 'The account number has 6 to 20 digits.',
    'prestador.cobro.error.numeroCelularInvalido': 'The mobile number has 10 digits and starts with 3.',
    'prestador.cobro.error.titularRequerido': "Enter the account holder's name.",
    'prestador.cobro.error.titularLargo': 'The name can have up to 100 characters.',

    // Provider public profile (P03.2)
    'prestador.perfil.intro':
      'This information appears on your public profile and helps tourists get to know you. It is published right away, without moderation.',
    'prestador.perfil.cargando': 'Loading your profile…',
    'prestador.perfil.datos.titulo': 'Profile details',
    'prestador.perfil.sinPerfil':
      "You don't have a public profile yet. Write the description and save it to publish it.",
    'prestador.perfil.opcional': '(optional)',
    'prestador.perfil.nombreComercial': 'Business name',
    'prestador.perfil.descripcion': 'Description',
    'prestador.perfil.ayudaDescripcion': 'Tell tourists in a few lines who you are and what you offer.',
    'prestador.perfil.horarios': 'Opening hours',
    'prestador.perfil.ayudaHorarios': 'For example: Monday to Saturday, 8:00 AM to 6:00 PM.',
    'prestador.perfil.sitioWeb': 'Website',
    'prestador.perfil.ayudaEnlace': 'An address that starts with http:// or https://.',
    'prestador.perfil.redes': 'Social media',
    'prestador.perfil.ayudaRedes': 'One address per line, starting with http:// or https://.',
    'prestador.perfil.guardar': 'Save profile',
    'prestador.perfil.creado': 'You created your public profile. Tourists can see it now.',
    'prestador.perfil.guardado': 'You saved your public profile.',
    'prestador.perfil.error.descripcionRequerida': 'Enter the description of your profile.',
    'prestador.perfil.error.sitioInvalido': 'Enter a web address that starts with http:// or https://.',
    'prestador.perfil.error.redesInvalidas':
      'Put each social media address on its own line, starting with http:// or https://.',
    'prestador.perfil.fotos.titulo': 'Profile photos',
    'prestador.perfil.fotos.primeroGuarda': 'Save the profile details first so you can add photos.',
    'prestador.perfil.fotos.leyenda': 'Profile photos',
    'prestador.perfil.fotos.ayuda':
      'You can have up to {maximo} photos in JPG, PNG or WebP format, up to 5 MB each. Each photo is uploaded and removed right away; there is no need to save.',
    'prestador.perfil.fotos.alt': 'Photo {indice} of {total} of the profile',
    'prestador.perfil.fotos.agregada': 'Photo added. The profile has {n} of {maximo} photos.',
    'prestador.perfil.fotos.quitada': 'Photo removed. The profile has {n} of {maximo} photos.',
    'prestador.perfil.fotos.quitadaSinFotos': 'Photo removed. The profile has no photos.',
    'prestador.perfil.fotos.motivoMaximo': 'a profile can have at most {maximo} photos',

    'prestador.ofertas.subtitulo':
      'Every new or edited offer goes through moderation before it appears in the catalog.',
    'prestador.ofertas.caption': 'Provider offers, {n} in total',
    'prestador.ofertas.colNombre': 'Offer name',
    'prestador.ofertas.colTipo': 'Type',
    'prestador.ofertas.colPrecio': 'Ref. price',
    'prestador.ofertas.colEstado': 'Status',
    'prestador.ofertas.colAcciones': 'Actions',
    'prestador.ofertas.motivo': 'Reason: {motivo}',
    'prestador.ofertas.sinMotivo': 'none given',
    'prestador.ofertas.sigueVisible': 'Your published version is still visible in the catalog.',
    'prestador.ofertas.verDetalle': 'View details',
    'prestador.ofertas.deLaOferta': 'of {nombre}',
    'prestador.ofertas.editar': 'Edit',
    'prestador.ofertas.retirar': 'Withdraw',
    'prestador.ofertas.vacioTitulo': "You don't have any offers yet",
    'prestador.ofertas.vacioAyuda': 'Publish your first one and we will send it to moderation.',
    'prestador.ofertas.cargando': 'Loading your offers…',
    'prestador.ofertas.sinNombre': 'Unnamed offer',
    'prestador.ofertas.retiroTitulo': 'Withdraw this offer?',
    'prestador.ofertas.retiroMensaje':
      '“{nombre}” will no longer appear in the catalog. Withdrawal is permanent: afterwards you will not be able to edit it or publish it again.',
    'prestador.ofertas.retiroAccion': 'Withdraw offer',
    'prestador.ofertas.retirada': 'We removed “{nombre}” from the catalog.',

    'prestador.detalle.noEncontrada': "We couldn't find this offer among yours",
    'prestador.detalle.noEncontradaAyuda':
      'It may not be yours, or it may no longer exist. Check your list of offers.',
    'prestador.detalle.verMis': 'View my offers',
    'prestador.detalle.versionPublicada': 'Published version',
    'prestador.detalle.vacioPublicada': "It doesn't have a published version yet.",
    'prestador.detalle.versionPendiente': 'Pending version',
    'prestador.detalle.vacioPendiente': 'It has no changes under review.',
    'prestador.detalle.numero': 'Version number',
    'prestador.detalle.moderacion': 'Moderation status',
    'prestador.detalle.enviada': 'Sent to moderation',
    'prestador.detalle.revisada': 'Reviewed',
    'prestador.detalle.aunNo': 'Not yet',
    'prestador.detalle.motivoRechazo': 'Reason for rejection',
    'prestador.detalle.nombre': 'Name',
    'prestador.detalle.precio': 'Reference price',
    'prestador.detalle.capacidad': 'Maximum capacity',
    'prestador.detalle.descripcion': 'Description',
    'prestador.detalle.fotos': 'Photos',
    'prestador.detalle.fotosN': { uno: '{n} photo', otros: '{n} photos' },
    'prestador.detalle.sinFotos': 'No photos',
    'prestador.detalle.corregir': 'Fix the rejected version',
    'prestador.detalle.verEnCatalogo': 'View in the catalog',

    'prestador.form.alGuardar': 'When you save, the offer goes to moderation.',
    'prestador.form.versionSigue':
      'The published version stays visible while the changes are reviewed.',
    'prestador.form.cargando': 'Loading the form…',
    'prestador.form.volver': 'Back to my offers',
    'prestador.form.cambiosEnRevision':
      'This offer already has changes under review (version {numero}). The form starts from that version; when you save, the new version replaces the pending one and the published one stays visible.',
    'prestador.form.primeraEnRevision':
      'The first version of this offer (version {numero}) is still in moderation. The form starts from it; when you save, the new version replaces it.',
    'prestador.form.rechazada':
      'Version {numero} of this offer was rejected. Reason: {motivo}. The form starts from that version: fix it and submit it again so it goes back to moderation.',
    'prestador.form.sinTipo':
      "You haven't registered your RNT and provider type yet, so you can't publish or edit offers. That screen will arrive in an upcoming phase.",
    'prestador.form.noPropia': "We couldn't find this offer among yours.",

    'prestador.form.generales': 'General information',
    'prestador.form.unicoTipo': '(the only type your provider type can publish)',
    'prestador.form.precio': 'Reference price (COP)',
    'prestador.form.ayudaPrecio': 'It is shown {unidad}. For information only.',
    'prestador.form.capacidad': 'Maximum capacity',
    'prestador.form.ayudaCapacidad': 'People per request.',
    'prestador.form.eligeZona': 'Choose an area',
    'prestador.form.horarios': 'Hours',
    'prestador.form.ayudaHorarios':
      'Days or operating hours, for example “Every day, 8:00 AM to 5:00 PM”.',
    'prestador.form.fotos': 'Photos',

    'prestador.form.actividad': 'Activity details',
    'prestador.form.ayudaGuia': 'As an independent guide you must complete the tour details.',
    'prestador.form.eligeTipo': 'Choose a type',
    'prestador.form.eligeNivel': 'Choose a level',
    'prestador.form.ayudaHoras': '24-hour times separated by commas: 07:30, 14:00.',
    'prestador.form.duracion': 'Duration in minutes',
    'prestador.form.modalidad': 'Tour format',
    'prestador.form.sinDefinir': 'Not set',
    'prestador.form.idiomas': 'Service languages',
    'prestador.form.ayudaIdiomas': 'Two-letter codes separated by commas: es, en.',
    'prestador.form.actividadSinNombre': 'Unnamed activity',

    'prestador.form.paquete': 'Package details',
    'prestador.form.ayudaActividades':
      "Choose {minimo} or more of your activities. The package's physical effort and accessibility come from them.",
    'prestador.form.sinAprobar': '(not approved yet)',
    'prestador.form.sinActividades':
      "You don't have any activities to include. First publish at least {minimo} activities.",
    'prestador.form.alojamiento': 'Accommodation details',
    'prestador.form.ayudaUnidad': 'For example: cabin, double room, hammock.',
    'prestador.form.opcional': '(optional)',
    'prestador.form.ayudaMapa':
      'Click on the map to place the pin where the offer is. To move it, click on another point.',
    'prestador.form.mapa': 'Map to choose the offer location',
    'prestador.form.latitud': 'Latitude',
    'prestador.form.longitud': 'Longitude',
    'prestador.form.sinUbicacion': 'No location',
    'prestador.form.quitarUbicacion': 'Remove location',
    'prestador.form.comoLlegar': 'How to get there',

    'prestador.form.revision':
      'An administrator reviews the offer before it appears in the catalog.',
    'prestador.form.enviando': 'Sending…',
    'prestador.form.enviar': 'Send to moderation',
    'prestador.form.enviada': 'We sent your offer to moderation',
    'prestador.form.enviadaConVersion': 'We sent your offer to moderation (version {numero})',
    'prestador.form.faltaFoto': 'Add at least one photo: the first one is the cover.',
    'prestador.form.esperaFotos':
      'Wait for the photos to finish uploading and submit the offer again.',
    'prestador.form.conDetalle': '{mensaje} Details: {detalle}.',
    'prestador.form.errorObligatorio': 'This field is required.',
    'prestador.form.errorMinimo': 'Must be {minimo} or more.',
    'prestador.form.errorEntero': 'Must be a whole number.',
    'prestador.form.errorHoras':
      'Write each time in the format 07:30 and separate them with commas.',
    'prestador.form.errorIdiomas':
      'Use lowercase two-letter codes, like es or en, separated by commas.',
    'prestador.form.errorActividades': 'Choose at least {minimo} activities.',
    'prestador.form.errorRevisa': 'Check this field.',

    'prestador.fotos.leyenda': 'Offer photos',
    'prestador.fotos.ayuda':
      'Add 1 to {maximo} photos in JPG, PNG or WebP format, up to 5 MB each. The first one is the cover: to change it, use “Move earlier” on another photo.',
    'prestador.fotos.agregar': 'Add photos',
    'prestador.fotos.contador': '{n} of {maximo}',
    'prestador.fotos.llegoAlMaximo': "You've reached the maximum of {maximo} photos.",
    'prestador.fotos.portada': 'Cover',
    'prestador.fotos.moverAntes': 'Move earlier',
    'prestador.fotos.moverDespues': 'Move later',
    'prestador.fotos.quitar': 'Remove',
    'prestador.fotos.laFoto': 'photo {n}',
    'prestador.fotos.subiendo': 'Uploading…',
    'prestador.fotos.reintentar': 'Retry',
    'prestador.fotos.laSubida': 'the upload of {nombre}',
    'prestador.fotos.descartar': 'Discard',
    'prestador.fotos.alt': 'Photo {indice} of {total} of the offer',
    'prestador.fotos.altPortada': 'Photo {indice} of {total} of the offer (cover)',
    'prestador.fotos.movida': 'Photo moved to position {posicion} of {total}.',
    'prestador.fotos.ahoraPortada': 'It is now the cover.',
    'prestador.fotos.quitadaSinFotos': 'Photo removed. The offer has no photos.',
    'prestador.fotos.quitada': 'Photo removed. The offer has {n} of {maximo} photos.',
    'prestador.fotos.agregada': 'Photo added. The offer has {n} of {maximo} photos.',
    'prestador.fotos.noSubido': 'Could not upload “{nombre}”: {motivo}.',
    'prestador.fotos.motivoFormato': 'only JPG, PNG or WebP photos are accepted',
    'prestador.fotos.motivoVacio': 'the file is empty',
    'prestador.fotos.motivoPesa': 'it is larger than 5 MB',
    'prestador.fotos.motivoMaximo': 'an offer can have at most {maximo} photos',
  },
);
