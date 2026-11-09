import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { Rol } from './core/api/modelos-nucleo';
import { CLAVE_SESION, aSesionGuardada } from './core/sesion/almacen-sesion';
import { respuestaDeSesion } from './core/sesion/testing';
import { IniciarSesion } from './features/cuentas/iniciar-sesion/iniciar-sesion';
import { MisDatos } from './features/cuentas/mis-datos/mis-datos';
import { RecuperarContrasena } from './features/cuentas/recuperar-contrasena/recuperar-contrasena';
import { Registro } from './features/cuentas/registro/registro';
import { RestablecerContrasena } from './features/cuentas/restablecer-contrasena/restablecer-contrasena';
import { SinPermiso } from './features/cuentas/sin-permiso/sin-permiso';
import { PanelPrestador } from './features/prestador/panel/panel-prestador';
import { Catalogo } from './features/turista/catalogo/catalogo';
import { FormularioSolicitud } from './features/turista/solicitudes/formulario-solicitud/formulario-solicitud';
import { Ficha } from './features/turista/ficha/ficha';
import { MisOpiniones } from './features/turista/mis-opiniones/mis-opiniones';
import { Proximamente } from './shared/proximamente/proximamente';

/**
 * Qué pantalla se CREA de verdad en cada ruta. `app.routes.spec.ts` solo comprueba a qué dirección termina la
 * navegación; eso no basta: una ruta de ruta vacía puede "coincidir" con la dirección sin crear ninguna pantalla y la
 * página queda en blanco, sin errores y sin peticiones.
 */
describe('pantallas que crea cada ruta', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  function conSesion(rol: Rol): void {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol }))));
  }

  /** Navega a la dirección y exige que la pantalla creada sea la indicada; si es otra, o ninguna, falla. */
  function ir(url: string, pantalla: Type<unknown>): Promise<unknown> {
    return harness.navigateByUrl(url, pantalla);
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      // Como en la aplicación (app.config.ts): los datos de la ruta llegan a las pantallas como inputs, por ejemplo el `id`.
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  // Las pantallas piden datos al crearse: se cierran esas peticiones para que no queden abiertas.
  afterEach(() =>
    http.match(() => true).forEach((peticion) => peticion.error(new ProgressEvent('error'))),
  );

  describe('sin sesión', () => {
    it('"/" crea el catálogo y este pide las ofertas', async () => {
      await ir('/', Catalogo);

      expect(http.match((p) => p.url === '/api/catalogo/ofertas').length).toBeGreaterThan(0);
    });

    it.each([
      ['/ofertas/abc', Ficha],
      ['/iniciar-sesion', IniciarSesion],
      ['/registro', Registro],
      ['/registro?tipo=prestador', Registro],
      ['/recuperar-contrasena', RecuperarContrasena],
      // El correo de recuperación del núcleo enlaza exactamente aquí.
      ['/restablecer-contrasena?token=abc123', RestablecerContrasena],
    ] as const)('%s crea su pantalla', async (url, pantalla) => {
      expect(await ir(url, pantalla)).toBeInstanceOf(pantalla);
    });
  });

  describe('con la sesión de un turista', () => {
    beforeEach(() => conSesion('TURISTA'));

    it('"/" crea el catálogo y este pide las ofertas', async () => {
      await ir('/', Catalogo);

      expect(http.match((p) => p.url === '/api/catalogo/ofertas').length).toBeGreaterThan(0);
    });

    it.each([
      ['/ofertas/abc', Ficha],
      ['/mis-opiniones', MisOpiniones],
      ['/mis-datos', MisDatos],
      ['/turista/solicitudes/abc', Proximamente],
      // P02.1: el botón "Solicitar disponibilidad" de la ficha.
      ['/turista/solicitar/b00000000000000000000001', FormularioSolicitud],
    ] as const)('%s crea su pantalla', async (url, pantalla) => {
      expect(await ir(url, pantalla)).toBeInstanceOf(pantalla);
    });

    it('la sección de otro rol crea "no tienes permiso"', async () => {
      await ir('/prestador/ofertas', SinPermiso);
    });
  });

  describe('con la sesión de los demás roles', () => {
    it.each(['PRESTADOR', 'OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO', 'ADMINISTRADOR'] as const)(
      '"/" crea el catálogo también para un %s',
      async (rol) => {
        conSesion(rol);

        await ir('/', Catalogo);
      },
    );

    it('el prestador abre su panel', async () => {
      conSesion('PRESTADOR');

      await ir('/prestador/ofertas', PanelPrestador);
    });

    it.each([
      ['OPERADOR_DISPONIBILIDAD', '/operador'],
      ['GESTOR_DESTINO', '/gestor'],
      ['ADMINISTRADOR', '/admin'],
    ] as const)('un %s abre su pantalla "próximamente" en %s', async (rol, url) => {
      conSesion(rol);

      await ir(url, Proximamente);
    });
  });
});
