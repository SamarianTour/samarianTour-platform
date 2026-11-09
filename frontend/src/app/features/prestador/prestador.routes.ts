import { Routes } from '@angular/router';

/** Rutas del prestador, todas dentro de su panel. Las protege la ruta `/prestador` (solo la sesión de un prestador). */
export const RUTAS_PRESTADOR: Routes = [
  {
    path: '',
    loadComponent: () => import('./panel/panel-prestador').then((modulo) => modulo.PanelPrestador),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'ofertas' },
      {
        path: 'ofertas',
        title: 'app.titulo.misOfertas',
        loadComponent: () => import('./mis-ofertas/mis-ofertas').then((modulo) => modulo.MisOfertas),
      },
      {
        path: 'ofertas/nueva',
        title: 'app.titulo.publicarOferta',
        loadComponent: () =>
          import('./formulario-oferta/formulario-oferta').then((modulo) => modulo.FormularioOferta),
      },
      {
        path: 'ofertas/:id',
        title: 'app.titulo.detalleOferta',
        loadComponent: () => import('./detalle-oferta/detalle-oferta').then((modulo) => modulo.DetalleOferta),
      },
      {
        path: 'ofertas/:id/editar',
        title: 'app.titulo.editarOferta',
        loadComponent: () =>
          import('./formulario-oferta/formulario-oferta').then((modulo) => modulo.FormularioOferta),
      },
    ],
  },
];
