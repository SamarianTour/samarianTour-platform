import { Routes } from '@angular/router';

/** Las pantallas del panel que llegan en los siguientes commits ocupan su lugar con esta, ya con su ruta y su título. */
const proximamente = () =>
  import('../../shared/proximamente/proximamente').then((modulo) => modulo.Proximamente);

/**
 * Rutas del operador de disponibilidad, todas dentro de su panel. Las protege la ruta `/operador` (solo la sesión de un
 * operador). `solicitudes/:id` es P02.5, la pantalla a la que lleva cada fila de la cola.
 */
export const RUTAS_OPERADOR: Routes = [
  {
    path: '',
    loadComponent: () => import('./panel/panel-operador').then((modulo) => modulo.PanelOperador),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'app.titulo.cola',
        loadComponent: () =>
          import('./cola/cola-operador').then((modulo) => modulo.ColaOperador),
      },
      {
        path: 'historial',
        title: 'app.titulo.historialOperador',
        loadComponent: () =>
          import('./historial/historial-operador').then((modulo) => modulo.HistorialOperador),
      },
      {
        path: 'solicitudes/:id',
        title: 'app.titulo.atenderSolicitud',
        loadComponent: () =>
          import('./atender/atender-solicitud').then((modulo) => modulo.AtenderSolicitud),
      },
      {
        path: 'avisos',
        title: 'app.titulo.avisosOperador',
        data: { nombre: 'app.titulo.avisosOperador' },
        loadComponent: proximamente,
      },
    ],
  },
];
