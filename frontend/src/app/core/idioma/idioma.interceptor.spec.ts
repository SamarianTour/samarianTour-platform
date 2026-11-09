import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { sesionInterceptor } from '../sesion/sesion.interceptor';
import { SesionService } from '../sesion/sesion.service';
import { respuestaDeSesion } from '../sesion/testing';
import { establecerIdioma } from './idioma';
import { idiomaInterceptor } from './idioma.interceptor';

describe('idiomaInterceptor', () => {
  let cliente: HttpClient;
  let http: HttpTestingController;

  const idioma = (url: string) => http.expectOne(url).request.headers.get('Accept-Language');

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        // Como en app.config.ts: el idioma va antes que la sesión.
        provideHttpClient(withInterceptors([idiomaInterceptor, sesionInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('manda el idioma vigente en las peticiones a la API', () => {
    cliente.get('/api/catalogo/ofertas').subscribe();

    expect(idioma('/api/catalogo/ofertas')).toBe('es');
  });

  it('sigue el idioma cuando cambia, sin recargar', () => {
    establecerIdioma('en', { recordar: false });
    cliente.get('/api/catalogo/ofertas').subscribe();

    expect(idioma('/api/catalogo/ofertas')).toBe('en');
  });

  it.each(['/api/nucleo/cuentas/sesion', '/api/recomendacion/consultas', '/api/nucleo/cuenta'])(
    'también en %s',
    (url) => {
      establecerIdioma('en', { recordar: false });
      cliente.post(url, {}).subscribe();

      expect(idioma(url)).toBe('en');
    },
  );

  it('no agrega nada a lo que no es de la API', () => {
    cliente.get('/assets/datos.json').subscribe();

    expect(idioma('/assets/datos.json')).toBeNull();
  });

  it('respeta un Accept-Language que la petición ya traiga', () => {
    cliente.get('/api/catalogo/ofertas', { headers: { 'Accept-Language': 'es-CO' } }).subscribe();

    expect(idioma('/api/catalogo/ofertas')).toBe('es-CO');
  });

  it('lo lleva también la petición que la sesión repite con un token renovado', () => {
    TestBed.inject(SesionService).iniciarSesion('a@b.co', 'x', false).subscribe();
    http.expectOne('/api/nucleo/cuentas/sesion').flush(respuestaDeSesion({ numero: 1 }));
    establecerIdioma('en', { recordar: false });

    cliente.get('/api/catalogo/prestador/ofertas').subscribe({ error: () => undefined });
    http
      .expectOne('/api/catalogo/prestador/ofertas')
      .flush({ codigo: 'NO_AUTENTICADO' }, { status: 401, statusText: 'Unauthorized' });
    // La renovación es una petición de acceso, y también habla el idioma elegido.
    const renovacion = http.expectOne('/api/nucleo/cuentas/sesion/renovacion');
    expect(renovacion.request.headers.get('Accept-Language')).toBe('en');
    renovacion.flush(respuestaDeSesion({ numero: 2 }));

    const repetida = http.expectOne('/api/catalogo/prestador/ofertas');
    expect(repetida.request.headers.get('Accept-Language')).toBe('en');
    expect(repetida.request.headers.get('Authorization')).toContain('Bearer ');
    repetida.flush([]);
  });
});
