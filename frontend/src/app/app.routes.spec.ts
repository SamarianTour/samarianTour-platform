import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { Rol } from './core/api/modelos-nucleo';
import { CLAVE_SESION, aSesionGuardada } from './core/sesion/almacen-sesion';
import { respuestaDeSesion } from './core/sesion/testing';

describe('rutas de la aplicación', () => {
  let router: Router;

  function conSesion(rol: Rol): void {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol }))));
  }

  /** Navega y devuelve dónde terminó, con redirecciones y guards ya aplicados. */
  async function ir(url: string): Promise<string> {
    await router.navigateByUrl(url);
    return router.url;
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    router = TestBed.inject(Router);
  });

  describe('sin sesión', () => {
    it.each([
      '/',
      '/ofertas/abc',
      '/iniciar-sesion',
      '/registro',
      '/registro?tipo=prestador',
      '/recuperar-contrasena',
      // El correo de recuperación del núcleo enlaza exactamente aquí.
      '/restablecer-contrasena?token=abc123',
      // P08.1 y P08.2: el turista no necesita sesión para pedir recomendaciones ni ver las alertas.
      '/recomendaciones',
      '/alertas',
    ])('%s es pública', async (url) => {
      expect(await ir(url)).toBe(url);
    });

    it.each([
      '/prestador/ofertas',
      '/mis-opiniones',
      '/turista/solicitudes/abc',
      '/turista/solicitar/abc',
      '/operador',
      '/operador/historial',
      '/operador/avisos',
      '/gestor',
      '/admin',
      '/admin/zonas',
      '/admin/alertas-sesgo',
      '/sin-permiso',
      '/mis-datos',
    ])('%s lleva a iniciar sesión y recuerda la página', async (url) => {
      expect(await ir(url)).toBe(`/iniciar-sesion?volver=${encodeURIComponent(url)}`);
    });

    it('una dirección desconocida vuelve al catálogo', async () => {
      expect(await ir('/no-existe')).toBe('/');
    });
  });

  describe('con la sesión de cada rol', () => {
    it.each([
      ['PRESTADOR', '/prestador/ofertas'],
      ['PRESTADOR', '/prestador/ofertas/nueva'],
      ['TURISTA', '/mis-opiniones'],
      ['TURISTA', '/turista/solicitudes/abc'],
      ['TURISTA', '/turista/solicitar/abc'],
      ['OPERADOR_DISPONIBILIDAD', '/operador'],
      ['OPERADOR_DISPONIBILIDAD', '/operador/solicitudes/abc'],
      ['OPERADOR_DISPONIBILIDAD', '/operador/historial'],
      ['OPERADOR_DISPONIBILIDAD', '/operador/avisos'],
      ['GESTOR_DESTINO', '/gestor'],
      ['ADMINISTRADOR', '/admin'],
      ['ADMINISTRADOR', '/admin/zonas'],
      ['ADMINISTRADOR', '/admin/moderacion'],
      ['ADMINISTRADOR', '/admin/cuentas'],
      ['ADMINISTRADOR', '/admin/comision'],
      ['ADMINISTRADOR', '/admin/alertas-sesgo'],
      ['ADMINISTRADOR', '/admin/pendientes'],
    ] as const)('un %s entra a %s', async (rol, url) => {
      conSesion(rol);

      expect(await ir(url)).toBe(url);
    });

    it.each([
      ['TURISTA', '/prestador/ofertas'],
      ['TURISTA', '/admin'],
      ['TURISTA', '/admin/alertas-sesgo'],
      ['OPERADOR_DISPONIBILIDAD', '/admin/cuentas'],
      ['PRESTADOR', '/mis-opiniones'],
      ['PRESTADOR', '/turista/solicitudes/abc'],
      ['PRESTADOR', '/turista/solicitar/abc'],
      ['PRESTADOR', '/operador'],
      ['TURISTA', '/operador/historial'],
      ['OPERADOR_DISPONIBILIDAD', '/gestor'],
      ['GESTOR_DESTINO', '/admin'],
      ['ADMINISTRADOR', '/prestador/ofertas'],
    ] as const)('un %s que abre %s ve "no tienes permiso"', async (rol, url) => {
      conSesion(rol);

      expect(await ir(url)).toBe('/sin-permiso');
    });

    it('/turista lleva a la lista de solicitudes', async () => {
      conSesion('TURISTA');

      expect(await ir('/turista')).toBe('/turista/solicitudes');
    });

    it('el catálogo y la ficha siguen abiertos a todos los roles', async () => {
      conSesion('ADMINISTRADOR');

      expect(await ir('/')).toBe('/');
      expect(await ir('/ofertas/abc')).toBe('/ofertas/abc');
    });

    it.each([
      'TURISTA',
      'PRESTADOR',
      'OPERADOR_DISPONIBILIDAD',
      'GESTOR_DESTINO',
      'ADMINISTRADOR',
    ] as const)('un %s puede ver sus datos en /mis-datos', async (rol) => {
      conSesion(rol);

      expect(await ir('/mis-datos')).toBe('/mis-datos');
    });

    it('/sin-permiso se ve con cualquier sesión', async () => {
      conSesion('TURISTA');

      expect(await ir('/sin-permiso')).toBe('/sin-permiso');
    });
  });
});
