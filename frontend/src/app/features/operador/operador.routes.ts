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
        data: { codigo: 'P02.4', nombre: 'app.titulo.cola' },
        loadComponent: proximamente,
      },
      {
        path: 'historial',
        title: 'app.titulo.historialOperador',
        data: { codigo: 'P02.4', nombre: 'app.titulo.historialOperador' },
        loadComponent: proximamente,
      },
      {
        path: 'solicitudes/:id',
        title: 'app.titulo.atenderSolicitud',
        data: { codigo: 'P02.5', nombre: 'app.titulo.atenderSolicitud' },
        loadComponent: proximamente,
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
