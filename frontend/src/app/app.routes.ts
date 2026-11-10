import { Routes } from '@angular/router';
import { rolGuard } from './core/sesion/guards';
import { RUTAS_CUENTAS } from './features/cuentas/cuentas.routes';

/**
 * El `title` de cada ruta es una clave de texto (`app.titulo.*`), no el texto: `EstrategiaDeTitulo` lo traduce y lo
 * mantiene al cambiar de idioma. Lo mismo vale para el `nombre` y la `descripcion` de las pantallas "próximamente".
 *
 * Cada funcionalidad se carga solo cuando se entra a ella. Las secciones de un rol solo las abre ese rol: sin sesión
 * llevan a iniciar sesión y con otro rol, a "no tienes permiso".
 */
export const routes: Routes = [
  {
    path: 'prestador',
    canActivate: [rolGuard('PRESTADOR')],
    loadChildren: () =>
      import('./features/prestador/prestador.routes').then((modulo) => modulo.RUTAS_PRESTADOR),
  },
  {
    // El núcleo enlaza aquí: el regreso de la pasarela de pago y el aviso de WhatsApp (/turista/solicitudes/:id).
    path: 'turista',
    canActivate: [rolGuard('TURISTA')],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'solicitudes' },
      {
        // P02.2: la lista.
        path: 'solicitudes',
        pathMatch: 'full',
        title: 'app.titulo.misSolicitudes',
        loadComponent: () =>
          import('./features/turista/solicitudes/mis-solicitudes/mis-solicitudes').then(
            (modulo) => modulo.MisSolicitudes,
          ),
      },
      {
        // P02.1: el botón "Solicitar disponibilidad" de la ficha lleva aquí. Sin sesión, el guard lleva a iniciar sesión y vuelve.
        path: 'solicitar/:ofertaId',
        title: 'app.titulo.solicitar',
        loadComponent: () =>
          import('./features/turista/solicitudes/formulario-solicitud/formulario-solicitud').then(
            (modulo) => modulo.FormularioSolicitud,
          ),
      },
      {
        // P02.2: el detalle. El núcleo enlaza exactamente aquí: el regreso de la pasarela de pago y el aviso de WhatsApp.
        path: 'solicitudes/:id',
        title: 'app.titulo.detalleSolicitud',
        loadComponent: () =>
          import('./features/turista/solicitudes/detalle-solicitud/detalle-solicitud').then(
            (modulo) => modulo.DetalleSolicitud,
          ),
      },
    ],
  },
  {
    path: 'operador',
    canActivate: [rolGuard('OPERADOR_DISPONIBILIDAD')],
    loadChildren: () =>
      import('./features/operador/operador.routes').then((modulo) => modulo.RUTAS_OPERADOR),
  },
  {
    path: 'gestor',
    canActivate: [rolGuard('GESTOR_DESTINO')],
    loadChildren: () => import('./features/gestor/gestor.routes').then((modulo) => modulo.RUTAS_GESTOR),
  },
  {
    path: 'admin',
    canActivate: [rolGuard('ADMINISTRADOR')],
    loadChildren: () => import('./features/admin/admin.routes').then((modulo) => modulo.RUTAS_ADMIN),
  },
  // Las pantallas de cuentas van sueltas en la raíz y NO dentro de un grupo `path: ''`. Un grupo de ruta vacía que no
  // tiene un hijo para la dirección vacía "coincide" igual con "/" y la deja sin pantalla: el contenido queda en blanco,
  // sin errores y sin peticiones, y las rutas que vienen después ni se intentan. Solo la definición de cada ruta se
  // importa aquí; cada pantalla sigue cargándose por demanda.
  ...RUTAS_CUENTAS,
  {
    path: '',
    loadChildren: () =>
      import('./features/turista/turista.routes').then((modulo) => modulo.RUTAS_TURISTA),
  },
  { path: '**', redirectTo: '' },
];
