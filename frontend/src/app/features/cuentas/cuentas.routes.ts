import { Routes } from '@angular/router';
import { autenticadoGuard } from '../../core/sesion/guards';

/** Pantallas de acceso y de la cuenta: públicas o de cualquier rol; las protege cada una según corresponda. */
export const RUTAS_CUENTAS: Routes = [
  {
    path: 'iniciar-sesion',
    title: 'Iniciar sesión · Samarian Tour',
    loadComponent: () =>
      import('./iniciar-sesion/iniciar-sesion').then((modulo) => modulo.IniciarSesion),
  },
  {
    path: 'registro',
    title: 'Crear cuenta · Samarian Tour',
    loadComponent: () => import('./registro/registro').then((modulo) => modulo.Registro),
  },
  {
    path: 'mis-datos',
    title: 'Mis datos · Samarian Tour',
    canActivate: [autenticadoGuard],
    loadComponent: () => import('./mis-datos/mis-datos').then((modulo) => modulo.MisDatos),
  },
  {
    path: 'recuperar-contrasena',
    title: 'Recuperar el acceso · Samarian Tour',
    loadComponent: () =>
      import('./recuperar-contrasena/recuperar-contrasena').then(
        (modulo) => modulo.RecuperarContrasena,
      ),
  },
  {
    // El correo de recuperación del núcleo enlaza exactamente a este nombre, con ?token=…
    path: 'restablecer-contrasena',
    title: 'Contraseña nueva · Samarian Tour',
    loadComponent: () =>
      import('./restablecer-contrasena/restablecer-contrasena').then(
        (modulo) => modulo.RestablecerContrasena,
      ),
  },
  {
    path: 'sin-permiso',
    title: 'Sin permiso · Samarian Tour',
    canActivate: [autenticadoGuard],
    loadComponent: () => import('./sin-permiso/sin-permiso').then((modulo) => modulo.SinPermiso),
  },
];
