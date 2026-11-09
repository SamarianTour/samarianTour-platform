import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Rol } from '../../../core/api/modelos-nucleo';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { SinPermiso } from './sin-permiso';

describe('SinPermiso', () => {
  let fixture: ComponentFixture<SinPermiso>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;

  function crear(rol: Rol): void {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol }))));
    fixture = TestBed.createComponent(SinPermiso);
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('avisa que no tiene permiso y con qué rol entró', () => {
    crear('TURISTA');

    expect(raiz().querySelector('h1')?.textContent).toContain('No tienes permiso');
    expect(raiz().textContent).toContain('Entraste como Turista');
  });

  it.each([
    ['TURISTA', '/'],
    ['PRESTADOR', '/prestador/ofertas'],
    ['OPERADOR_DISPONIBILIDAD', '/operador'],
    ['GESTOR_DESTINO', '/gestor'],
    ['ADMINISTRADOR', '/admin'],
  ] as const)('ofrece volver al inicio de un %s', (rol, inicio) => {
    crear(rol);

    expect(raiz().querySelector('a.boton')?.getAttribute('href')).toBe(inicio);
  });

  it('"Entrar con otra cuenta" cierra la sesión y lleva a iniciar sesión', () => {
    crear('TURISTA');
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    raiz().querySelector<HTMLButtonElement>('button')!.click();
    http
      .expectOne('/api/nucleo/cuentas/sesion/cierre')
      .flush(null, { status: 204, statusText: 'No Content' });

    expect(TestBed.inject(SesionService).autenticada()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/iniciar-sesion']);
  });
});
