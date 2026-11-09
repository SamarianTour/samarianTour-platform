import { Routes } from '@angular/router';
import { rolGuard } from './core/sesion/guards';
import { RUTAS_CUENTAS } from './features/cuentas/cuentas.routes';

/** Las secciones que llegan en ramas posteriores ocupan su lugar con esta pantalla, ya protegida por rol. */
const proximamente = () =>
  import('./shared/proximamente/proximamente').then((modulo) => modulo.Proximamente);

/**
 * El `title` de cada ruta es una clave de texto (`app.titulo.*`), no el texto: `EstrategiaDeTitulo` lo traduce y lo
 * mantiene al cambiar de idioma. Lo mismo vale para el `nombre` y la `descripcion` de las pantallas "próximamente".
 *
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
        // P02.1: el botón "Solicitar disponibilidad" de la ficha lleva aquí. Sin sesión, el guard lleva a iniciar sesión y vuelve.
        path: 'solicitar/:ofertaId',
        title: 'app.titulo.solicitar',
        loadComponent: () =>
          import('./features/turista/solicitudes/formulario-solicitud/formulario-solicitud').then(
            (modulo) => modulo.FormularioSolicitud,
          ),
      },
      {
        path: '**',
        title: 'app.titulo.misSolicitudes',
        data: {
          codigo: 'P02.2',
          nombre: 'app.proximamente.turista.nombre',
          descripcion: 'app.proximamente.turista.descripcion',
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
        title: 'app.titulo.cola',
        data: {
          codigo: 'P02.4',
          nombre: 'app.proximamente.operador.nombre',
          descripcion: 'app.proximamente.operador.descripcion',
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
        title: 'app.titulo.indicadores',
        data: {
          codigo: 'P09.1',
          nombre: 'app.proximamente.gestor.nombre',
          descripcion: 'app.proximamente.gestor.descripcion',
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
        title: 'app.titulo.administracion',
        data: {
          codigo: 'P05.1',
          nombre: 'app.proximamente.admin.nombre',
          descripcion: 'app.proximamente.admin.descripcion',
        },
        loadComponent: proximamente,
      },
    ],
  },
  // Las pantallas de cuentas van sueltas en la raíz y NO dentro de un grupo `path: ''`. Un grupo de ruta vacía que no
  // tiene un hijo para la dirección vacía "coincide" igual con "/" y la deja sin pantalla: el contenido queda en blanco,
  // sin errores y sin peticiones, y las rutas que vienen después ni se intentan. Solo la definición de cada ruta se
  // importa aquí; cada pantalla sigue cargándose por demanda.
  ...RUTAS_CUENTAS,
  {
    path: '',
    loadChildren: () =>
      import('./features/turista/turista.routes').then((modulo) => modulo.RUTAS_TURISTA),
  },
  { path: '**', redirectTo: '' },
];
