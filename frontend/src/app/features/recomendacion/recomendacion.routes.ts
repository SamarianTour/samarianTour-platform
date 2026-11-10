import { Routes } from '@angular/router';

/** Las pantallas que llegan en los siguientes commits ocupan su lugar con esta, ya con su ruta y su título. */
const proximamente = () => import('../../shared/proximamente/proximamente').then((modulo) => modulo.Proximamente);

/**
 * P08.1 Recomendaciones (`/recomendaciones`). Es pública: el turista no necesita sesión para pedir recomendaciones; la sesión solo se
 * exige al enviar una solicitud, en el núcleo.
 */
export const RUTAS_RECOMENDACIONES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'app.titulo.recomendaciones',
    loadComponent: () => import('./chat/chat-recomendaciones').then((modulo) => modulo.ChatRecomendaciones),
  },
];

/**
 * P08.2 Alertas del destino (`/alertas`). También es pública. No está en el menú: se llega a ella desde Recomendaciones y desde la
 * sección de alertas de la ficha de una oferta.
 */
export const RUTAS_ALERTAS: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'app.titulo.alertas',
    data: { codigo: 'P08.2', nombre: 'app.titulo.alertas' },
    loadComponent: proximamente,
  },
];
