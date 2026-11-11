import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Title } from '@angular/platform-browser';
import { TestBed } from '@angular/core/testing';
import { Router, TitleStrategy, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../../app.routes';
import { EstrategiaDeTitulo } from '../../../core/idioma/estrategia-de-titulo';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { PaginaNoEncontrada } from './pagina-no-encontrada';

describe('Página no encontrada', () => {
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement!;

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: TitleStrategy, useClass: EstrategiaDeTitulo },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => establecerIdioma('es', { recordar: false }));

  it.each(['/no-existe', '/ofertas', '/admin/algo/que/no/existe', '/zonas/a/b/c'])(
    '%s crea la pantalla y no redirige en silencio',
    async (url) => {
      const componente = await harness.navigateByUrl(url, PaginaNoEncontrada);

      expect(componente).toBeInstanceOf(PaginaNoEncontrada);
      expect(TestBed.inject(Router).url).toBe(url);
    },
  );

  it('explica el problema y ofrece volver al Inicio', async () => {
    await harness.navigateByUrl('/no-existe', PaginaNoEncontrada);

    expect(raiz().querySelector('h1')?.textContent).toContain('Página no encontrada');
    expect(raiz().querySelector('a.boton')?.getAttribute('href')).toBe('/');
    expect(raiz().textContent).toContain('Ir al inicio');
  });

  it('tiene título en la pestaña y cambia a inglés', async () => {
    await harness.navigateByUrl('/no-existe', PaginaNoEncontrada);
    expect(TestBed.inject(Title).getTitle()).toBe('Página no encontrada · Samarian Tour');

    establecerIdioma('en', { recordar: false });
    harness.detectChanges();

    expect(TestBed.inject(Title).getTitle()).toBe('Page not found · Samarian Tour');
    expect(raiz().querySelector('h1')?.textContent).toContain('Page not found');
    expect(raiz().textContent).toContain('Go to the home page');
  });
});
