import { Routes } from '@angular/router';

/** Páginas legales del pie: públicas, para cualquier persona con o sin sesión. */
export const RUTAS_LEGAL: Routes = [
  {
    path: 'terminos',
    title: 'app.titulo.terminos',
    loadComponent: () => import('./terminos/terminos').then((modulo) => modulo.Terminos),
  },
  {
    path: 'proteccion-consumidor',
    title: 'app.titulo.proteccionConsumidor',
    loadComponent: () =>
      import('./proteccion-consumidor/proteccion-consumidor').then((modulo) => modulo.ProteccionConsumidor),
  },
  {
    path: 'pqrsf',
    title: 'app.titulo.pqrsf',
    loadComponent: () => import('./pqrsf/pqrsf').then((modulo) => modulo.Pqrsf),
  },
  {
    path: 'politica-datos',
    title: 'app.titulo.politicaDatos',
    loadComponent: () => import('./politica-datos/politica-datos').then((modulo) => modulo.PoliticaDatosPagina),
  },
];
