import { Routes } from '@angular/router';

/** Rutas del turista: el catálogo, la ficha de una oferta y sus opiniones propias. */
export const RUTAS_TURISTA: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Catálogo · Samarian Tour',
    loadComponent: () => import('./catalogo/catalogo').then((modulo) => modulo.Catalogo),
  },
  {
    path: 'mis-opiniones',
    title: 'Mis opiniones · Samarian Tour',
    loadComponent: () => import('./mis-opiniones/mis-opiniones').then((modulo) => modulo.MisOpiniones),
  },
  {
    path: 'ofertas/:id',
    title: 'Oferta · Samarian Tour',
    loadComponent: () => import('./ficha/ficha').then((modulo) => modulo.Ficha),
  },
];
