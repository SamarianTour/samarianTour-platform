import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  TitleStrategy,
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { erroresInterceptor } from './core/errores/errores.interceptor';
import { EstrategiaDeTitulo } from './core/idioma/estrategia-de-titulo';
import { idiomaInterceptor } from './core/idioma/idioma.interceptor';
import './core/idioma/locales';
import { SesionService } from './core/sesion/sesion.service';
import { sesionInterceptor } from './core/sesion/sesion.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    // El `title` de cada ruta es una clave de texto; esta estrategia lo traduce y lo mantiene al cambiar de idioma.
    { provide: TitleStrategy, useExisting: EstrategiaDeTitulo },
    // El idioma va primero, para que lo lleve también la petición que la sesión repite con un token renovado. Después, la
    // sesión: recibe los errores ya traducidos y puede repetir la petición con un token renovado.
    provideHttpClient(withInterceptors([idiomaInterceptor, sesionInterceptor, erroresInterceptor])),
    // Con una sesión guardada, se renueva antes de la primera petición si su token ya venció o está por vencer.
    provideAppInitializer(() => firstValueFrom(inject(SesionService).reanudar())),
    // Fijo: Angular no deja cambiarlo en marcha. Las fechas, los precios y los números siguen el idioma elegido con los
    // pipes propios `fecha`, `precio` y `numero`, no con los de Angular.
    { provide: LOCALE_ID, useValue: 'es-CO' },
  ],
};
