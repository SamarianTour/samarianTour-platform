import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { CLAVE_SESION, aSesionGuardada } from './core/sesion/almacen-sesion';
import { respuestaDeSesion } from './core/sesion/testing';
import { RestablecerContrasena } from './features/cuentas/restablecer-contrasena/restablecer-contrasena';
import { DetalleSolicitud } from './features/turista/solicitudes/detalle-solicitud/detalle-solicitud';

/**
 * Las dos direcciones del frontend a las que enlaza el backend, y que por eso no pueden cambiar de nombre ni quedar sin
 * pantalla:
 *  - `/turista/solicitudes/:id`: el regreso de la pasarela de pago y el enlace del aviso de WhatsApp.
 *  - `/restablecer-contrasena?token=...`: el correo de recuperación.
 */
describe('rutas a las que enlaza el backend', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const ID = '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20';

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.match(() => true).forEach((p) => p.error(new ProgressEvent('error'))));

  describe('/turista/solicitudes/:id', () => {
    it('con la sesión de un turista crea el detalle y pide esa solicitud', async () => {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
      );

      const componente = await harness.navigateByUrl(`/turista/solicitudes/${ID}`, DetalleSolicitud);

      expect(componente).toBeInstanceOf(DetalleSolicitud);
      expect(http.match((p) => p.url === `/api/nucleo/turista/solicitudes/${ID}`)).toHaveLength(1);
    });

    it('sin sesión lleva a iniciar sesión y recuerda esa dirección para volver después', async () => {
      await harness.navigateByUrl(`/turista/solicitudes/${ID}`);

      expect(TestBed.inject(Router).url).toBe(
        `/iniciar-sesion?volver=${encodeURIComponent(`/turista/solicitudes/${ID}`)}`,
      );
    });
  });

  describe('/restablecer-contrasena?token=...', () => {
    it('crea la pantalla sin necesitar sesión y recibe el token de la dirección', async () => {
      const componente = await harness.navigateByUrl(
        '/restablecer-contrasena?token=abc',
        RestablecerContrasena,
      );

      expect(componente).toBeInstanceOf(RestablecerContrasena);
      expect(componente.token()).toBe('abc');
      expect(TestBed.inject(Router).url).toBe('/restablecer-contrasena?token=abc');
    });
  });
});
