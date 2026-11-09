import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Rol } from '../api/modelos-nucleo';
import { CLAVE_SESION, aSesionGuardada } from './almacen-sesion';
import { autenticadoGuard, rolGuard } from './guards';
import { respuestaDeSesion } from './testing';

describe('guards de sesión', () => {
  let router: Router;

  /** Abre una sesión guardada, como si la página se hubiera recargado con ella. */
  function conSesion(rol: Rol): void {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol }))));
  }

  /** Ejecuta el guard como lo haría el router al ir a `url` y devuelve a dónde manda: true o una dirección. */
  function ir(guard: CanActivateFn, url: string): true | string {
    const resultado = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );
    return resultado instanceof UrlTree ? router.serializeUrl(resultado) : (resultado as true);
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    router = TestBed.inject(Router);
  });

  describe('autenticadoGuard', () => {
    it('deja pasar a quien tiene sesión, sea cual sea su rol', () => {
      conSesion('OPERADOR_DISPONIBILIDAD');

      expect(ir(autenticadoGuard, '/mis-datos')).toBe(true);
    });

    it('sin sesión lleva a iniciar sesión y recuerda la página', () => {
      expect(ir(autenticadoGuard, '/mis-datos')).toBe('/iniciar-sesion?volver=%2Fmis-datos');
    });

    it('recuerda también el resto de la dirección', () => {
      expect(ir(autenticadoGuard, '/mis-datos?ver=politica')).toBe(
        '/iniciar-sesion?volver=%2Fmis-datos%3Fver%3Dpolitica',
      );
    });
  });

  describe('rolGuard', () => {
    it('deja pasar al rol que la sección exige', () => {
      conSesion('PRESTADOR');

      expect(ir(rolGuard('PRESTADOR'), '/prestador/ofertas')).toBe(true);
    });

    it('deja pasar a cualquiera de varios roles', () => {
      conSesion('ADMINISTRADOR');

      expect(ir(rolGuard('GESTOR_DESTINO', 'ADMINISTRADOR'), '/compartida')).toBe(true);
    });

    it('con otro rol lleva a "no tienes permiso"', () => {
      conSesion('TURISTA');

      expect(ir(rolGuard('PRESTADOR'), '/prestador/ofertas')).toBe('/sin-permiso');
    });

    it('sin sesión lleva a iniciar sesión, no a "no tienes permiso"', () => {
      expect(ir(rolGuard('PRESTADOR'), '/prestador/ofertas/nueva')).toBe(
        '/iniciar-sesion?volver=%2Fprestador%2Fofertas%2Fnueva',
      );
    });

    it.each([
      ['TURISTA', 'OPERADOR_DISPONIBILIDAD'],
      ['OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO'],
      ['GESTOR_DESTINO', 'ADMINISTRADOR'],
      ['ADMINISTRADOR', 'TURISTA'],
      ['PRESTADOR', 'ADMINISTRADOR'],
    ] as const)('un %s no entra a una sección de %s', (tiene, exige) => {
      conSesion(tiene);

      expect(ir(rolGuard(exige), '/seccion')).toBe('/sin-permiso');
    });
  });
});
