import { diccionario } from './diccionario';

/**
 * Textos del armazón de la aplicación: cabecera, menú de cada rol, títulos de las pestañas del navegador y las
 * pantallas "próximamente". Los títulos son el nombre de la pantalla; el sufijo "· Samarian Tour" lo agrega
 * `EstrategiaDeTitulo`.
 */
export const APP = diccionario(
  {
    'app.saltar': 'Saltar al contenido',
    'app.nav.principal': 'Principal',
    'app.cuenta.iniciarSesion': 'Iniciar sesión',
    'app.cuenta.registrarse': 'Registrarse',
    'app.pie.lema': 'Turismo sostenible en Santa Marta',

    // Enlaces del menú de cada rol.
    'app.nav.catalogo': 'Catálogo',
    'app.nav.soyPrestador': 'Soy prestador',
    'app.nav.misSolicitudes': 'Mis solicitudes',
    'app.nav.misOpiniones': 'Mis opiniones',
    'app.nav.misOfertas': 'Mis ofertas',
    'app.nav.cola': 'Cola de solicitudes',
    'app.nav.indicadores': 'Indicadores',
    'app.nav.administracion': 'Administración',

    // Menú de la cuenta.
    'app.menu.cuenta': 'menú de la cuenta',
    'app.menu.misDatos': 'Mis datos',
    'app.menu.cerrarSesion': 'Cerrar sesión',
    'app.menu.sesionCerrada': 'Cerraste sesión',

    // Títulos de las rutas (la pestaña del navegador).
    'app.titulo.catalogo': 'Catálogo',
    'app.titulo.misOpiniones': 'Mis opiniones',
    'app.titulo.oferta': 'Oferta',
    'app.titulo.misSolicitudes': 'Mis solicitudes',
    'app.titulo.cola': 'Cola de solicitudes',
    'app.titulo.historialOperador': 'Historial de solicitudes',
    'app.titulo.avisosOperador': 'Avisos',
    'app.titulo.atenderSolicitud': 'Atender solicitud',
    'app.titulo.indicadores': 'Indicadores',
    'app.titulo.administracion': 'Administración',
    'app.titulo.misOfertas': 'Mis ofertas',
    'app.titulo.publicarOferta': 'Publicar oferta',
    'app.titulo.detalleOferta': 'Detalle de la oferta',
    'app.titulo.editarOferta': 'Editar oferta',
    'app.titulo.iniciarSesion': 'Iniciar sesión',
    'app.titulo.crearCuenta': 'Crear cuenta',
    'app.titulo.misDatos': 'Mis datos',
    'app.titulo.recuperarAcceso': 'Recuperar el acceso',
    'app.titulo.contrasenaNueva': 'Contraseña nueva',
    'app.titulo.sinPermiso': 'Sin permiso',
    'app.titulo.solicitar': 'Solicitar disponibilidad',
    'app.titulo.detalleSolicitud': 'Detalle de la solicitud',

    // Pantallas que llegan en ramas posteriores (se ven con la ruta ya protegida por rol).
    'app.proximamente.operador.nombre': 'Cola de solicitudes',
    'app.proximamente.operador.descripcion':
      'Aquí atenderás la cola de solicitudes de disponibilidad de los prestadores que tienes asignados.',
    'app.proximamente.gestor.nombre': 'Panel de indicadores',
    'app.proximamente.gestor.descripcion':
      'Aquí verás la demanda por zona, el embudo de solicitudes y la distribución por categorías del destino.',
    'app.proximamente.admin.nombre': 'Administración',
    'app.proximamente.admin.descripcion':
      'Aquí revisarás los prestadores y su RNT, la moderación de ofertas y opiniones, las cuentas internas y las zonas.',
  },
  {
    'app.saltar': 'Skip to content',
    'app.nav.principal': 'Main',
    'app.cuenta.iniciarSesion': 'Log in',
    'app.cuenta.registrarse': 'Sign up',
    'app.pie.lema': 'Sustainable tourism in Santa Marta',

    'app.nav.catalogo': 'Catalog',
    'app.nav.soyPrestador': "I'm a provider",
    'app.nav.misSolicitudes': 'My requests',
    'app.nav.misOpiniones': 'My reviews',
    'app.nav.misOfertas': 'My offers',
    'app.nav.cola': 'Request queue',
    'app.nav.indicadores': 'Indicators',
    'app.nav.administracion': 'Administration',

    'app.menu.cuenta': 'account menu',
    'app.menu.misDatos': 'My data',
    'app.menu.cerrarSesion': 'Log out',
    'app.menu.sesionCerrada': 'You logged out',

    'app.titulo.catalogo': 'Catalog',
    'app.titulo.misOpiniones': 'My reviews',
    'app.titulo.oferta': 'Offer',
    'app.titulo.misSolicitudes': 'My requests',
    'app.titulo.cola': 'Request queue',
    'app.titulo.historialOperador': 'Request history',
    'app.titulo.avisosOperador': 'Notices',
    'app.titulo.atenderSolicitud': 'Handle request',
    'app.titulo.indicadores': 'Indicators',
    'app.titulo.administracion': 'Administration',
    'app.titulo.misOfertas': 'My offers',
    'app.titulo.publicarOferta': 'Publish offer',
    'app.titulo.detalleOferta': 'Offer details',
    'app.titulo.editarOferta': 'Edit offer',
    'app.titulo.iniciarSesion': 'Log in',
    'app.titulo.crearCuenta': 'Create account',
    'app.titulo.misDatos': 'My data',
    'app.titulo.recuperarAcceso': 'Recover access',
    'app.titulo.contrasenaNueva': 'New password',
    'app.titulo.sinPermiso': 'No permission',
    'app.titulo.solicitar': 'Request availability',
    'app.titulo.detalleSolicitud': 'Request details',

    'app.proximamente.operador.nombre': 'Request queue',
    'app.proximamente.operador.descripcion':
      'Here you will handle the queue of availability requests from the providers assigned to you.',
    'app.proximamente.gestor.nombre': 'Indicators dashboard',
    'app.proximamente.gestor.descripcion':
      "Here you'll see demand by area, the request funnel and the breakdown by category for the destination.",
    'app.proximamente.admin.nombre': 'Administration',
    'app.proximamente.admin.descripcion':
      'Here you will review providers and their RNT, moderate offers and reviews, and manage internal accounts and areas.',
  },
);
