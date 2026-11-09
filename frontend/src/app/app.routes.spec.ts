import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { routes } from './app.routes';

describe('rutas de la aplicación', () => {
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    router = TestBed.inject(Router);
  });

  it.each(['/iniciar-sesion', '/ofertas/abc', '/mis-opiniones', '/prestador/ofertas'])(
    '%s existe y no cae en el comodín',
    async (url) => {
      await router.navigateByUrl(url);

      expect(router.url).toBe(url);
    },
  );

  it('una dirección desconocida vuelve al catálogo', async () => {
    await router.navigateByUrl('/no-existe');

    expect(router.url).toBe('/');
  });
});
