import { Routes } from '@angular/router';

/** Pantallas de acceso y de la cuenta: públicas o de cualquier rol; las protege cada una según corresponda. */
export const RUTAS_CUENTAS: Routes = [
  {
    path: 'iniciar-sesion',
    title: 'Iniciar sesión · Samarian Tour',
    loadComponent: () =>
      import('./iniciar-sesion/iniciar-sesion').then((modulo) => modulo.IniciarSesion),
  },
];
