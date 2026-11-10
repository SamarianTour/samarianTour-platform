import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { RUTAS_OPERADOR } from '../operador.routes';
import { PanelOperador } from './panel-operador';

describe('PanelOperador', () => {
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const enlaces = () => Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.menu a'));

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    const sesion = respuestaDeSesion({ rol: 'OPERADOR_DISPONIBILIDAD' });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(
        aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, nombre: 'Operador Sierra' } }),
      ),
    );
    TestBed.configureTestingModule({
      providers: [
        // Con `withComponentInputBinding`, como en la aplicación: atender recibe el `id` de la dirección.
        provideRouter([{ path: 'operador', children: RUTAS_OPERADOR }], withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    harness = await RouterTestingHarness.create();
  });

  it('muestra el nombre y el rol del operador de la sesión', async () => {
    await harness.navigateByUrl('/operador', PanelOperador);

    expect(raiz().querySelector('.identidad__nombre')?.textContent).toBe('Operador Sierra');
    expect(raiz().querySelector('.identidad__avatar')?.textContent).toBe('O');
    expect(raiz().querySelector('.identidad .rotulo')?.textContent).toBe(
      'Operador de disponibilidad',
    );
  });

  it('ofrece Cola, Historial y Avisos', async () => {
    await harness.navigateByUrl('/operador', PanelOperador);

    expect(enlaces().map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Cola', '/operador'],
      ['Historial', '/operador/historial'],
      ['Avisos', '/operador/avisos'],
    ]);
  });

  it.each([
    ['/operador', 'Cola'],
    ['/operador/solicitudes/6f1c2a9e', 'Cola'],
    ['/operador/historial', 'Historial'],
    ['/operador/avisos', 'Avisos'],
  ] as const)('en %s marca solo "%s"', async (url, marcada) => {
    await harness.navigateByUrl(url, PanelOperador);
    harness.detectChanges();

    const activos = enlaces().filter((a) => a.classList.contains('activo'));
    expect(activos.map((a) => a.textContent?.trim())).toEqual([marcada]);
    expect(activos[0].getAttribute('aria-current')).toBe('page');
  });
});
