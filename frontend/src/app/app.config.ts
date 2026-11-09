import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localeEsCo from '@angular/common/locales/es-CO';
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
import { SesionService } from './core/sesion/sesion.service';
import { sesionInterceptor } from './core/sesion/sesion.interceptor';

// Fechas y precios con el formato de Colombia.
registerLocaleData(localeEsCo);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    // El `title` de cada ruta es una clave de texto; esta estrategia lo traduce y lo mantiene al cambiar de idioma.
    { provide: TitleStrategy, useExisting: EstrategiaDeTitulo },
    // La sesión va primero: recibe los errores ya traducidos y puede repetir la petición con un token renovado.
    provideHttpClient(withInterceptors([sesionInterceptor, erroresInterceptor])),
    // Con una sesión guardada, se renueva antes de la primera petición si su token ya venció o está por vencer.
    provideAppInitializer(() => firstValueFrom(inject(SesionService).reanudar())),
    { provide: LOCALE_ID, useValue: 'es-CO' },
  ],
};
