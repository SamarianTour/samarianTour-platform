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
        data: { codigo: 'P05.1', nombre: 'app.titulo.prestadoresRnt' },
        loadComponent: proximamente,
      },
      {
        path: 'moderacion',
        title: 'app.titulo.moderacion',
        data: { codigo: 'P05.2', nombre: 'app.titulo.moderacion' },
        loadComponent: proximamente,
      },
      {
        path: 'cuentas',
        title: 'app.titulo.cuentasInternas',
        data: { codigo: 'P05.3', nombre: 'app.titulo.cuentasInternas' },
        loadComponent: proximamente,
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
