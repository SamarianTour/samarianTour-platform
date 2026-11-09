import { Routes } from '@angular/router';
import { rolGuard } from './core/sesion/guards';

/** Las secciones que llegan en ramas posteriores ocupan su lugar con esta pantalla, ya protegida por rol. */
const proximamente = () =>
  import('./shared/proximamente/proximamente').then((modulo) => modulo.Proximamente);

/**
 * Cada funcionalidad se carga solo cuando se entra a ella. Las secciones de un rol solo las abre ese rol: sin sesión
 * llevan a iniciar sesión y con otro rol, a "no tienes permiso".
 */
export const routes: Routes = [
  {
    path: 'prestador',
    canActivate: [rolGuard('PRESTADOR')],
    loadChildren: () =>
      import('./features/prestador/prestador.routes').then((modulo) => modulo.RUTAS_PRESTADOR),
  },
  {
    // El núcleo enlaza aquí: el regreso de la pasarela de pago y el aviso de WhatsApp (/turista/solicitudes/:id).
    path: 'turista',
    canActivate: [rolGuard('TURISTA')],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'solicitudes' },
      {
        path: '**',
        title: 'Mis solicitudes · Samarian Tour',
        data: {
          codigo: 'P02.2',
          nombre: 'Mis solicitudes',
          descripcion:
            'Aquí verás el estado de tus solicitudes de disponibilidad, y podrás confirmar o declinar las condiciones y pagar.',
        },
        loadComponent: proximamente,
      },
    ],
  },
  {
    path: 'operador',
    canActivate: [rolGuard('OPERADOR_DISPONIBILIDAD')],
    children: [
      {
        path: '**',
        title: 'Cola de solicitudes · Samarian Tour',
        data: {
          codigo: 'P02.4',
          nombre: 'Cola de solicitudes',
          descripcion:
            'Aquí atenderás la cola de solicitudes de disponibilidad de los prestadores que tienes asignados.',
        },
        loadComponent: proximamente,
      },
    ],
  },
  {
    path: 'gestor',
    canActivate: [rolGuard('GESTOR_DESTINO')],
    children: [
      {
        path: '**',
        title: 'Indicadores · Samarian Tour',
        data: {
          codigo: 'P09.1',
          nombre: 'Panel de indicadores',
          descripcion:
            'Aquí verás la demanda por zona, el embudo de solicitudes y la distribución por categorías del destino.',
        },
        loadComponent: proximamente,
      },
    ],
  },
  {
    path: 'admin',
    canActivate: [rolGuard('ADMINISTRADOR')],
    children: [
      {
        path: '**',
        title: 'Administración · Samarian Tour',
        data: {
          codigo: 'P05.1',
          nombre: 'Administración',
          descripcion:
            'Aquí revisarás los prestadores y su RNT, la moderación de ofertas y opiniones, las cuentas internas y las zonas.',
        },
        loadComponent: proximamente,
      },
    ],
  },
  {
    path: '',
    loadChildren: () =>
      import('./features/cuentas/cuentas.routes').then((modulo) => modulo.RUTAS_CUENTAS),
  },
  {
    path: '',
    loadChildren: () =>
      import('./features/turista/turista.routes').then((modulo) => modulo.RUTAS_TURISTA),
  },
  { path: '**', redirectTo: '' },
];
