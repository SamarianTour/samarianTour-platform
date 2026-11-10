import { Routes } from '@angular/router';

/**
 * Rutas del administrador, todas dentro de su panel. Las protege la ruta `/admin` (solo la sesión de un administrador). El
 * inicio es la lista de prestadores (P05.1).
 */
export const RUTAS_ADMIN: Routes = [
  {
    path: '',
    loadComponent: () => import('./panel/panel-admin').then((modulo) => modulo.PanelAdmin),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'app.titulo.prestadoresRnt',
        loadComponent: () =>
          import('./prestadores/prestadores-rnt').then((modulo) => modulo.PrestadoresRnt),
      },
      {
        path: 'moderacion',
        title: 'app.titulo.moderacion',
        loadComponent: () =>
          import('./moderacion/moderacion-admin').then((modulo) => modulo.ModeracionAdmin),
      },
      {
        path: 'cuentas',
        title: 'app.titulo.cuentasInternas',
        loadComponent: () =>
          import('./cuentas/cuentas-internas').then((modulo) => modulo.CuentasInternas),
      },
      {
        path: 'comision',
        title: 'app.titulo.comisionPagos',
        loadComponent: () =>
          import('./comision/comision-pagos').then((modulo) => modulo.ComisionPagos),
      },
      {
        path: 'zonas',
        title: 'app.titulo.zonasAdmin',
        loadComponent: () => import('./zonas/zonas-admin').then((modulo) => modulo.ZonasAdmin),
      },
      {
        path: 'alertas-sesgo',
        title: 'app.titulo.alertasSesgo',
        loadComponent: () =>
          import('./alertas-sesgo/alertas-sesgo').then((modulo) => modulo.AlertasSesgo),
      },
      {
        path: 'pendientes',
        title: 'app.titulo.pendientesTecnicos',
        loadComponent: () =>
          import('./pendientes/pendientes-tecnicos').then((modulo) => modulo.PendientesTecnicos),
      },
    ],
  },
];
