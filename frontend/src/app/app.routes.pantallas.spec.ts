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
import { PanelOperador } from './features/operador/panel/panel-operador';
import { PanelPrestador } from './features/prestador/panel/panel-prestador';
import { Catalogo } from './features/turista/catalogo/catalogo';
import { DetalleSolicitud } from './features/turista/solicitudes/detalle-solicitud/detalle-solicitud';
import { MisSolicitudes } from './features/turista/solicitudes/mis-solicitudes/mis-solicitudes';
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
      // P02.2: la lista y el detalle. El núcleo enlaza al detalle desde el regreso del pago y el aviso de WhatsApp.
      ['/turista/solicitudes', MisSolicitudes],
      ['/turista/solicitudes/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20', DetalleSolicitud],
      // P02.1: el botón "Solicitar disponibilidad" de la ficha.
      ['/turista/solicitar/b00000000000000000000001', FormularioSolicitud],
    ] as const)('%s crea su pantalla', async (url, pantalla) => {
      expect(await ir(url, pantalla)).toBeInstanceOf(pantalla);
    });

    // P04.1: el núcleo y la pasarela enlazan a esta dirección; con la solicitud confirmada, el detalle crea el bloque de pago.
    it('el detalle de una solicitud confirmada crea el bloque de pago y pide sus intentos', async () => {
      const id = '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20';
      await ir(`/turista/solicitudes/${id}`, DetalleSolicitud);

      http.expectOne('/api/catalogo/zonas').flush([]);
      http.expectOne(`/api/nucleo/turista/solicitudes/${id}`).flush({
        id,
        oferta: { id: 'b1', tipo: 'ACTIVIDAD', nombre: 'Caminata', zonaId: 'z1' },
        fechaInicio: '2026-10-28',
        personas: 2,
        estado: 'CONFIRMADA',
        condiciones: { precioBase: 300000, politicaCancelacion: 'x' },
        creadaEn: '2026-10-26T14:00:00Z',
        actualizadaEn: '2026-10-26T14:30:00Z',
        historial: [],
      });
      harness.detectChanges();
      http.expectOne(`/api/nucleo/turista/solicitudes/${id}/pagos`).flush([]);
      harness.detectChanges();

      expect(harness.routeNativeElement?.querySelector('app-pagar-solicitud')).not.toBeNull();
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
      ['GESTOR_DESTINO', '/gestor'],
      ['ADMINISTRADOR', '/admin'],
    ] as const)('un %s abre su pantalla "próximamente" en %s', async (rol, url) => {
      conSesion(rol);

      await ir(url, Proximamente);
    });

    // P02.4: la cola se crea dentro del panel y pide las solicitudes pendientes del operador.
    it('el operador abre la cola en /operador y esta pide sus solicitudes', async () => {
      conSesion('OPERADOR_DISPONIBILIDAD');

      await ir('/operador', PanelOperador);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-cola-operador')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/operador/solicitudes').length).toBeGreaterThan(0);
    });

    // Historial: la misma consulta de la cola con el parámetro estado.
    it('el operador abre el historial en /operador/historial y este pide el estado de la dirección', async () => {
      conSesion('OPERADOR_DISPONIBILIDAD');

      await ir('/operador/historial?estado=PAGADA', PanelOperador);

      expect(
        harness.routeNativeElement?.querySelector('.contenido > app-historial-operador'),
      ).not.toBeNull();
      const peticiones = http.match((p) => p.url === '/api/nucleo/operador/solicitudes');
      expect(peticiones.map((p) => p.request.params.get('estado'))).toEqual(['PAGADA']);
    });

    // P02.5: atender se crea con el id de la dirección y pide esa solicitud al núcleo.
    it('el operador abre atender en /operador/solicitudes/:id y este pide esa solicitud', async () => {
      conSesion('OPERADOR_DISPONIBILIDAD');

      await ir('/operador/solicitudes/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20', PanelOperador);

      expect(
        harness.routeNativeElement?.querySelector('.contenido > app-atender-solicitud'),
      ).not.toBeNull();
      expect(
        http.match(
          (p) => p.url === '/api/nucleo/operador/solicitudes/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20',
        ),
      ).toHaveLength(1);
    });

    // Avisos (RF-34): la lista se crea dentro del panel y pide los avisos del operador.
    it('el operador abre los avisos en /operador/avisos y estos piden sus avisos', async () => {
      conSesion('OPERADOR_DISPONIBILIDAD');

      await ir('/operador/avisos', PanelOperador);

      expect(
        harness.routeNativeElement?.querySelector('.contenido > app-avisos-operador'),
      ).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/operador/avisos')).toHaveLength(1);
    });
  });
});
