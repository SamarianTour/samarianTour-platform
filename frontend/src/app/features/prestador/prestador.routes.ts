import { Routes } from '@angular/router';

/** Rutas del prestador, todas dentro del panel que lleva el selector de prestador demo. */
export const RUTAS_PRESTADOR: Routes = [
  {
    path: '',
    loadComponent: () => import('./panel/panel-prestador').then((modulo) => modulo.PanelPrestador),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'ofertas' },
      {
        path: 'ofertas',
        title: 'Mis ofertas · Samarian Tour',
        loadComponent: () => import('./mis-ofertas/mis-ofertas').then((modulo) => modulo.MisOfertas),
      },
      {
        path: 'ofertas/nueva',
        title: 'Publicar oferta · Samarian Tour',
        loadComponent: () =>
          import('./formulario-oferta/formulario-oferta').then((modulo) => modulo.FormularioOferta),
      },
      {
        path: 'ofertas/:id',
        title: 'Detalle de la oferta · Samarian Tour',
        loadComponent: () => import('./detalle-oferta/detalle-oferta').then((modulo) => modulo.DetalleOferta),
      },
      {
        path: 'ofertas/:id/editar',
        title: 'Editar oferta · Samarian Tour',
        loadComponent: () =>
          import('./formulario-oferta/formulario-oferta').then((modulo) => modulo.FormularioOferta),
      },
    ],
  },
];
