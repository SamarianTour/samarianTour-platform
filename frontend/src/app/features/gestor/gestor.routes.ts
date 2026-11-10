import { Routes } from '@angular/router';

/** Las vistas que llegan en los siguientes commits ocupan su lugar con esta, ya con su ruta y su título. */
const proximamente = () => import('../../shared/proximamente/proximamente').then((modulo) => modulo.Proximamente);

/**
 * Rutas del gestor del destino, todas dentro de su panel. Las protege la ruta `/gestor` (solo la sesión de un gestor). El panel de
 * indicadores (P09.1) tiene tres vistas, cada una con su dirección: Demanda (`/gestor`), Embudo de conversión (`/gestor/embudo`) y
 * Distribución por tipo de prestador (`/gestor/distribucion`). El periodo viaja en la dirección: `?desde=AAAA-MM-DD&hasta=AAAA-MM-DD`.
 */
export const RUTAS_GESTOR: Routes = [
  {
    path: '',
    loadComponent: () => import('./panel/panel-gestor').then((modulo) => modulo.PanelGestor),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./indicadores/panel-indicadores').then((modulo) => modulo.PanelIndicadores),
        children: [
          {
            path: '',
            pathMatch: 'full',
            title: 'app.titulo.demanda',
            data: { codigo: 'P09.1', nombre: 'app.titulo.demanda' },
            loadComponent: proximamente,
          },
          {
            path: 'embudo',
            title: 'app.titulo.embudo',
            data: { codigo: 'P09.1', nombre: 'app.titulo.embudo' },
            loadComponent: proximamente,
          },
          {
            path: 'distribucion',
            title: 'app.titulo.distribucionPrestador',
            data: { codigo: 'P09.1', nombre: 'app.titulo.distribucionPrestador' },
            loadComponent: proximamente,
          },
        ],
      },
    ],
  },
];
