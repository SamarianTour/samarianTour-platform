import { Routes } from '@angular/router';

/** Las pantallas del panel que llegan en los siguientes commits ocupan su lugar con esta, ya con su ruta y su título. */
const proximamente = () =>
  import('../../shared/proximamente/proximamente').then((modulo) => modulo.Proximamente);

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
        data: { codigo: 'P05.4', nombre: 'app.titulo.comisionPagos' },
        loadComponent: proximamente,
      },
      {
        path: 'zonas',
        title: 'app.titulo.zonasAdmin',
        data: { codigo: 'P05.5', nombre: 'app.titulo.zonasAdmin' },
        loadComponent: proximamente,
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
        data: { nombre: 'app.titulo.pendientesTecnicos' },
        loadComponent: proximamente,
      },
    ],
  },
];
