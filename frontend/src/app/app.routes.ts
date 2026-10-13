import { Routes } from '@angular/router';

/** Cada funcionalidad se carga solo cuando se entra a ella. */
export const routes: Routes = [
  {
    path: 'prestador',
    loadChildren: () => import('./features/prestador/prestador.routes').then((modulo) => modulo.RUTAS_PRESTADOR),
  },
  {
    path: '',
    loadChildren: () => import('./features/turista/turista.routes').then((modulo) => modulo.RUTAS_TURISTA),
  },
  { path: '**', redirectTo: '' },
];
