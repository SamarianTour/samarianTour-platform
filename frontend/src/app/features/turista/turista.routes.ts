import { Routes } from '@angular/router';

/** Rutas del turista: el catálogo y la ficha de una oferta. */
export const RUTAS_TURISTA: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Catálogo · Samarian Tour',
    loadComponent: () => import('./catalogo/catalogo').then((modulo) => modulo.Catalogo),
  },
  {
    path: 'ofertas/:id',
    title: 'Oferta · Samarian Tour',
    loadComponent: () => import('./ficha/ficha').then((modulo) => modulo.Ficha),
  },
];
