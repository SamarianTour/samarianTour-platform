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
    // P01.3: el mismo catálogo con el tipo fijo en PAQUETE y su propio título.
    path: 'paquetes',
    pathMatch: 'full',
    title: 'app.titulo.paquetes',
    data: { tipoFijo: 'PAQUETE' },
    loadComponent: () => import('./catalogo/catalogo').then((modulo) => modulo.Catalogo),
  },
  {
    path: 'mis-opiniones',
    title: 'app.titulo.misOpiniones',
    canActivate: [rolGuard('TURISTA')],
    loadComponent: () => import('./mis-opiniones/mis-opiniones').then((modulo) => modulo.MisOpiniones),
  },
  {
    // Ficha de la zona (RF-25): la enlazan la ficha de una oferta y, más adelante, el Inicio.
    path: 'zonas/:id',
    title: 'app.titulo.zona',
    loadComponent: () => import('./zona/ficha-zona').then((modulo) => modulo.FichaZona),
  },
  {
    path: 'ofertas/:id',
    title: 'app.titulo.oferta',
    loadComponent: () => import('./ficha/ficha').then((modulo) => modulo.Ficha),
  },
];
