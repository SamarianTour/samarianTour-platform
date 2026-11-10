import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { RUTAS_ADMIN } from '../admin.routes';
import { ENLACES_ADMIN, PanelAdmin } from './panel-admin';

describe('PanelAdmin', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const enlaces = () => Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.menu a'));

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    const sesion = respuestaDeSesion({ rol: 'ADMINISTRADOR' });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, nombre: 'Marta Ríos' } })),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'admin', children: RUTAS_ADMIN }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.match(() => true).forEach((p) => p.flush([])));

  it('muestra el nombre y el rol del administrador de la sesión', async () => {
    await harness.navigateByUrl('/admin', PanelAdmin);

    expect(raiz().querySelector('.identidad__nombre')?.textContent).toBe('Marta Ríos');
    expect(raiz().querySelector('.identidad .rotulo')?.textContent).toBe('Administrador');
  });

  it('ofrece las siete secciones del administrador, en orden', async () => {
    await harness.navigateByUrl('/admin', PanelAdmin);

    expect(enlaces().map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Prestadores y RNT', '/admin'],
      ['Moderación', '/admin/moderacion'],
      ['Cuentas internas', '/admin/cuentas'],
      ['Comisión y pagos', '/admin/comision'],
      ['Zonas', '/admin/zonas'],
      ['Alertas de sesgo', '/admin/alertas-sesgo'],
      ['Pendientes técnicos', '/admin/pendientes'],
    ]);
  });

  it.each([
    ['/admin', 'Prestadores y RNT'],
    ['/admin/moderacion', 'Moderación'],
    ['/admin/cuentas', 'Cuentas internas'],
    ['/admin/comision', 'Comisión y pagos'],
    ['/admin/zonas', 'Zonas'],
    ['/admin/alertas-sesgo', 'Alertas de sesgo'],
    ['/admin/pendientes', 'Pendientes técnicos'],
  ] as const)('en %s marca solo "%s"', async (url, marcada) => {
    await harness.navigateByUrl(url, PanelAdmin);
    harness.detectChanges();

    const activos = enlaces().filter((a) => a.classList.contains('activo'));
    expect(activos.map((a) => a.textContent?.trim())).toEqual([marcada]);
    expect(activos[0].getAttribute('aria-current')).toBe('page');
  });

  it('las direcciones del menú son las rutas del panel, ni una más ni una menos', () => {
    const rutas = RUTAS_ADMIN[0].children!.map((ruta) => (ruta.path ? `/admin/${ruta.path}` : '/admin'));

    expect([...rutas].sort()).toEqual(ENLACES_ADMIN.map((e) => e.ruta).sort());
  });
});
