import { Routes } from '@angular/router';
import { rolGuard } from '../../core/sesion/guards';

/** Rutas del turista: el catálogo (`/catalogo`), la ficha de una oferta y sus opiniones propias. */
export const RUTAS_TURISTA: Routes = [
  {
    path: 'catalogo',
    pathMatch: 'full',
    title: 'app.titulo.catalogo',
    loadComponent: () => import('./catalogo/catalogo').then((modulo) => modulo.Catalogo),
  },
  {
    path: 'mis-opiniones',
    title: 'app.titulo.misOpiniones',
    canActivate: [rolGuard('TURISTA')],
    loadComponent: () => import('./mis-opiniones/mis-opiniones').then((modulo) => modulo.MisOpiniones),
  },
  {
    path: 'ofertas/:id',
    title: 'app.titulo.oferta',
    loadComponent: () => import('./ficha/ficha').then((modulo) => modulo.Ficha),
  },
];
