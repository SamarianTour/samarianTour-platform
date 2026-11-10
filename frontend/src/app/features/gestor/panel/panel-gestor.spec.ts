import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { RUTAS_GESTOR } from '../gestor.routes';
import { ENLACES_GESTOR, PanelGestor } from './panel-gestor';

describe('PanelGestor', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const enlaces = () => Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.menu a'));

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    const sesion = respuestaDeSesion({ rol: 'GESTOR_DESTINO' });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, nombre: 'Gestión del destino' } })),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'gestor', children: RUTAS_GESTOR }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.match(() => true).forEach((p) => p.flush([]));
  });

  it('muestra el nombre y el rol del gestor de la sesión', async () => {
    await harness.navigateByUrl('/gestor', PanelGestor);

    expect(raiz().querySelector('.identidad__nombre')?.textContent).toBe('Gestión del destino');
    expect(raiz().querySelector('.identidad .rotulo')?.textContent).toBe('Gestor del destino');
  });

  it('ofrece una sola sección: el panel de indicadores', async () => {
    await harness.navigateByUrl('/gestor', PanelGestor);

    expect(ENLACES_GESTOR).toHaveLength(1);
    expect(enlaces().map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Panel de indicadores', '/gestor'],
    ]);
  });

  it.each(['/gestor', '/gestor/embudo', '/gestor/distribucion'])(
    'el panel de indicadores queda marcado en %s',
    async (url) => {
      await harness.navigateByUrl(url, PanelGestor);

      expect(raiz().querySelector('.menu a[aria-current="page"]')?.getAttribute('href')).toBe('/gestor');
    },
  );

  it('el menú y el rol cambian de idioma', async () => {
    establecerIdioma('en', { recordar: false });
    await harness.navigateByUrl('/gestor', PanelGestor);

    expect(raiz().querySelector('.identidad .rotulo')?.textContent).toBe('Destination manager');
    expect(raiz().querySelector('nav .rotulo')?.textContent).toBe('Destination manager panel');
    expect(enlaces()[0].textContent?.trim()).toBe('Indicators dashboard');
  });
});
