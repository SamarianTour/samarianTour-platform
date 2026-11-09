import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ErrorApi } from '../errores/error-api';
import { erroresInterceptor } from '../errores/errores.interceptor';
import { esRutaDeAcceso, esRutaProtegida, sesionInterceptor } from './sesion.interceptor';
import { SesionService } from './sesion.service';
import { respuestaDeSesion } from './testing';

const SESION = '/api/nucleo/cuentas/sesion';
const RENOVACION = `${SESION}/renovacion`;
const PROTEGIDA = '/api/catalogo/prestador/ofertas';
const PUBLICA = '/api/catalogo/ofertas';

const NO_AUTENTICADO = { status: 401, codigo: 'NO_AUTENTICADO' };
const RECHAZO = { status: 401, statusText: 'Unauthorized' };

describe('sesionInterceptor', () => {
  let cliente: HttpClient;
  let http: HttpTestingController;
  let sesion: SesionService;
  let router: Router;
  let navegar: ReturnType<typeof vi.spyOn>;

  /** Abre una sesión con tokens numerados, para distinguir el original del renovado. */
  function iniciar(numero = 1): void {
    sesion.iniciarSesion('a@b.co', 'x', false).subscribe();
    http.expectOne(SESION).flush(respuestaDeSesion({ numero }));
  }

  const token = (numero: number) => respuestaDeSesion({ numero }).tokenAcceso;
  const encabezado = (peticion: { request: { headers: { get(n: string): string | null } } }) =>
    peticion.request.headers.get('Authorization');

  beforeEach(() => {
    vi.useFakeTimers({ now: new Date('2026-11-08T12:00:00Z') });
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([sesionInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(SesionService);
    router = TestBed.inject(Router);
    navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  describe('qué peticiones llevan el token', () => {
    it('sin sesión no manda ninguna autorización', () => {
      cliente.get(PROTEGIDA).subscribe();

      expect(encabezado(http.expectOne(PROTEGIDA))).toBeNull();
    });

    it.each([PROTEGIDA, '/api/nucleo/cuenta', '/api/recomendacion/consultas', PUBLICA])(
      'con sesión manda el token a %s',
      (url) => {
        iniciar(1);

        cliente.get(url).subscribe();

        expect(encabezado(http.expectOne(url))).toBe(`Bearer ${token(1)}`);
      },
    );

    it.each([
      '/api/nucleo/cuentas/sesion',
      '/api/nucleo/cuentas/registro',
      '/api/nucleo/cuentas/contrasena',
      '/api/nucleo/politica-datos/vigente',
    ])('no manda el token a la ruta de acceso %s', (url) => {
      iniciar(1);

      cliente.post(url, {}).subscribe();

      expect(encabezado(http.expectOne(url))).toBeNull();
    });

    it('no manda el token a lo que no es la API', () => {
      iniciar(1);

      cliente.get('/assets/logo.svg').subscribe();
      cliente.get('https://otro.sitio.co/api/datos').subscribe();

      expect(encabezado(http.expectOne('/assets/logo.svg'))).toBeNull();
      expect(encabezado(http.expectOne('https://otro.sitio.co/api/datos'))).toBeNull();
    });
  });

  describe('token vencido', () => {
    it('renueva la sesión y repite la petición una vez, con el token nuevo', () => {
      iniciar(1);
      let respuesta: unknown;

      cliente.get(PROTEGIDA).subscribe((datos) => (respuesta = datos));

      http.expectOne(PROTEGIDA).flush(NO_AUTENTICADO, RECHAZO);
      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));
      const repetida = http.expectOne(PROTEGIDA);
      expect(encabezado(repetida)).toBe(`Bearer ${token(2)}`);
      repetida.flush({ contenido: [] });
      expect(respuesta).toEqual({ contenido: [] });
    });

    it('si la petición repetida vuelve a recibir 401, no renueva otra vez y entrega el error', () => {
      iniciar(1);
      let error: unknown;

      cliente.get(PROTEGIDA).subscribe({ error: (e: unknown) => (error = e) });
      http.expectOne(PROTEGIDA).flush(NO_AUTENTICADO, RECHAZO);
      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));
      http.expectOne(PROTEGIDA).flush(NO_AUTENTICADO, RECHAZO);

      expect(error).toBeInstanceOf(ErrorApi);
      expect((error as ErrorApi).codigo).toBe('NO_AUTENTICADO');
      http.expectNone(RENOVACION);
      expect(sesion.autenticada()).toBe(true);
      expect(navegar).not.toHaveBeenCalled();
    });

    it('con varias peticiones vencidas a la vez hace una sola renovación', () => {
      iniciar(1);
      const respuestas: string[] = [];

      cliente.get('/api/catalogo/prestador/uno').subscribe(() => respuestas.push('uno'));
      cliente.get('/api/catalogo/prestador/dos').subscribe(() => respuestas.push('dos'));
      http.expectOne('/api/catalogo/prestador/uno').flush(NO_AUTENTICADO, RECHAZO);
      http.expectOne('/api/catalogo/prestador/dos').flush(NO_AUTENTICADO, RECHAZO);

      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));
      http.expectOne('/api/catalogo/prestador/uno').flush({});
      http.expectOne('/api/catalogo/prestador/dos').flush({});
      expect(respuestas.sort()).toEqual(['dos', 'uno']);
    });

    it('un 401 que no es de token vencido no renueva (contraseña incorrecta al eliminar la cuenta)', () => {
      iniciar(1);
      let error: unknown;

      cliente
        .post('/api/nucleo/turista/supresion', {})
        .subscribe({ error: (e: unknown) => (error = e) });
      http
        .expectOne('/api/nucleo/turista/supresion')
        .flush({ status: 401, codigo: 'CONTRASENA_INCORRECTA' }, RECHAZO);

      http.expectNone(RENOVACION);
      expect((error as ErrorApi).codigo).toBe('CONTRASENA_INCORRECTA');
      expect(sesion.autenticada()).toBe(true);
    });

    it('otros errores, como un 403, no renuevan', () => {
      iniciar(1);
      let error: unknown;

      cliente.get('/api/nucleo/admin/comision').subscribe({ error: (e: unknown) => (error = e) });
      http
        .expectOne('/api/nucleo/admin/comision')
        .flush(
          { status: 403, codigo: 'ACCESO_DENEGADO' },
          { status: 403, statusText: 'Forbidden' },
        );

      http.expectNone(RENOVACION);
      expect((error as ErrorApi).codigo).toBe('ACCESO_DENEGADO');
    });
  });

  describe('la renovación es rechazada', () => {
    it('en una ruta protegida cierra la sesión local y lleva a iniciar sesión', () => {
      iniciar(1);
      vi.spyOn(router, 'url', 'get').mockReturnValue('/prestador/ofertas?page=2');
      let error: unknown;

      cliente.get(PROTEGIDA).subscribe({ error: (e: unknown) => (error = e) });
      http.expectOne(PROTEGIDA).flush(NO_AUTENTICADO, RECHAZO);
      http.expectOne(RENOVACION).flush({ status: 401, codigo: 'SESION_INVALIDA' }, RECHAZO);

      expect(sesion.autenticada()).toBe(false);
      expect(navegar).toHaveBeenCalledWith(['/iniciar-sesion'], {
        queryParams: { volver: '/prestador/ofertas?page=2' },
      });
      expect((error as ErrorApi).codigo).toBe('NO_AUTENTICADO');
    });

    it('no vuelve a navegar si ya está en la pantalla de iniciar sesión', () => {
      iniciar(1);
      vi.spyOn(router, 'url', 'get').mockReturnValue('/iniciar-sesion?volver=%2F');

      cliente.get(PROTEGIDA).subscribe({ error: () => undefined });
      http.expectOne(PROTEGIDA).flush(NO_AUTENTICADO, RECHAZO);
      http.expectOne(RENOVACION).flush({ status: 401, codigo: 'SESION_INVALIDA' }, RECHAZO);

      expect(navegar).not.toHaveBeenCalled();
    });

    it('en una ruta pública repite la petición sin token y no saca al usuario de la página', () => {
      iniciar(1);
      let respuesta: unknown;

      cliente.get(PUBLICA).subscribe((datos) => (respuesta = datos));
      http.expectOne(PUBLICA).flush(NO_AUTENTICADO, RECHAZO);
      http.expectOne(RENOVACION).flush({ status: 401, codigo: 'SESION_INVALIDA' }, RECHAZO);
      const sinToken = http.expectOne(PUBLICA);

      expect(encabezado(sinToken)).toBeNull();
      sinToken.flush({ contenido: ['oferta'] });
      expect(respuesta).toEqual({ contenido: ['oferta'] });
      expect(sesion.autenticada()).toBe(false);
      expect(navegar).not.toHaveBeenCalled();
    });

    it('si no hay conexión para renovar, conserva la sesión y no navega', () => {
      iniciar(1);
      let error: unknown;

      cliente.get(PROTEGIDA).subscribe({ error: (e: unknown) => (error = e) });
      http.expectOne(PROTEGIDA).flush(NO_AUTENTICADO, RECHAZO);
      http.expectOne(RENOVACION).error(new ProgressEvent('error'));

      expect(error).toBeDefined();
      expect(sesion.autenticada()).toBe(true);
      expect(navegar).not.toHaveBeenCalled();
    });
  });
});

describe('clasificación de rutas', () => {
  it.each([
    '/api/catalogo/prestador/ofertas',
    '/api/catalogo/prestador/ofertas?page=1',
    '/api/catalogo/turista/opiniones',
    '/api/catalogo/admin/zonas',
    '/api/nucleo/cuenta',
    '/api/nucleo/turista/solicitudes/abc',
    '/api/nucleo/operador/solicitudes',
    '/api/nucleo/gestor/indicadores/tablero',
    '/api/recomendacion/admin/algo',
  ])('%s exige el token', (url) => {
    expect(esRutaProtegida(url)).toBe(true);
  });

  it.each([
    '/api/catalogo/ofertas',
    '/api/catalogo/zonas',
    '/api/nucleo/cuentas/sesion',
    '/api/nucleo/politica-datos/vigente',
    '/api/nucleo/cuentas-otra-cosa',
    '/api/recomendacion/consultas',
    '/otra/api/catalogo/prestador/ofertas',
  ])('%s es pública', (url) => {
    expect(esRutaProtegida(url)).toBe(false);
  });

  it.each([
    '/api/nucleo/cuentas/sesion',
    '/api/nucleo/cuentas/sesion/renovacion',
    '/api/nucleo/cuentas/registro',
    '/api/nucleo/cuentas/contrasena/restablecimiento',
    '/api/nucleo/politica-datos/v1',
  ])('%s es una ruta de acceso', (url) => {
    expect(esRutaDeAcceso(url)).toBe(true);
  });

  it('/cuenta (los datos personales) no es una ruta de acceso', () => {
    expect(esRutaDeAcceso('/api/nucleo/cuenta')).toBe(false);
  });
});
