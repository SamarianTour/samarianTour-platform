import { Routes } from '@angular/router';

/**
 * Rutas del operador de disponibilidad, todas dentro de su panel. Las protege la ruta `/operador` (solo la sesión de un
 * operador): la cola (P02.4), su historial, atender una solicitud (P02.5, a la que lleva cada fila de la cola y cada aviso)
 * y los avisos.
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
        loadComponent: () =>
          import('./avisos/avisos-operador').then((modulo) => modulo.AvisosOperador),
      },
    ],
  },
];
