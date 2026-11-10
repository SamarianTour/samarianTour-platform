import { Routes } from '@angular/router';

/**
 * Las secciones del panel que llegan en los siguientes commits ocupan su lugar con esta, ya con su ruta y su título.
 */
const proximamente = () => import('../../shared/proximamente/proximamente').then((modulo) => modulo.Proximamente);

/** Rutas del prestador, todas dentro de su panel. Las protege la ruta `/prestador` (solo la sesión de un prestador). */
export const RUTAS_PRESTADOR: Routes = [
  {
    path: '',
    loadComponent: () => import('./panel/panel-prestador').then((modulo) => modulo.PanelPrestador),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'app.titulo.inicioPrestador',
        loadComponent: () => import('./inicio/inicio-prestador').then((modulo) => modulo.InicioPrestador),
      },
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
      {
        path: 'rnt',
        title: 'app.titulo.rntPrestador',
        loadComponent: () => import('./rnt/rnt-prestador').then((modulo) => modulo.RntPrestador),
      },
      {
        path: 'perfil',
        title: 'app.titulo.perfilPublico',
        data: { codigo: 'P03.2', nombre: 'app.titulo.perfilPublico' },
        loadComponent: proximamente,
      },
      {
        path: 'whatsapp',
        title: 'app.titulo.whatsappPrestador',
        data: { codigo: 'P03.4', nombre: 'app.titulo.whatsappPrestador' },
        loadComponent: proximamente,
      },
      {
        path: 'cuenta-cobro',
        title: 'app.titulo.cuentaCobro',
        data: { codigo: 'P03.5', nombre: 'app.titulo.cuentaCobro' },
        loadComponent: proximamente,
      },
    ],
  },
];
