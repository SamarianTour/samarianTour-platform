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
import { PanelAdmin } from './features/admin/panel/panel-admin';
import { PanelGestor } from './features/gestor/panel/panel-gestor';
import { AlertasDestino } from './features/recomendacion/alertas/alertas-destino';
import { ChatRecomendaciones } from './features/recomendacion/chat/chat-recomendaciones';
import { Proximamente } from './shared/proximamente/proximamente';
import { PanelOperador } from './features/operador/panel/panel-operador';
import { PanelPrestador } from './features/prestador/panel/panel-prestador';
import { Catalogo } from './features/turista/catalogo/catalogo';
import { DetalleSolicitud } from './features/turista/solicitudes/detalle-solicitud/detalle-solicitud';
import { MisSolicitudes } from './features/turista/solicitudes/mis-solicitudes/mis-solicitudes';
import { FormularioSolicitud } from './features/turista/solicitudes/formulario-solicitud/formulario-solicitud';
import { Ficha } from './features/turista/ficha/ficha';
import { MisOpiniones } from './features/turista/mis-opiniones/mis-opiniones';

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
      ['/recomendaciones', ChatRecomendaciones],
      // P08.2: pública; al crearse pide todas las alertas.
      ['/alertas', AlertasDestino],
      ['/registro', Registro],
      ['/registro?tipo=prestador', Registro],
      ['/recuperar-contrasena', RecuperarContrasena],
      // El correo de recuperación del núcleo enlaza exactamente aquí.
      ['/restablecer-contrasena?token=abc123', RestablecerContrasena],
    ] as const)('%s crea su pantalla', async (url, pantalla) => {
      expect(await ir(url, pantalla)).toBeInstanceOf(pantalla);
    });
  });

  // P08.1: pública; al crearse lee las zonas del catálogo para el filtro, y no pide recomendaciones hasta que el turista pregunta.
  it('"/recomendaciones" crea el asistente, pide las zonas una vez y no pide recomendaciones', async () => {
    expect(await ir('/recomendaciones', ChatRecomendaciones)).toBeInstanceOf(ChatRecomendaciones);

    expect(http.match((p) => p.url === '/api/catalogo/zonas')).toHaveLength(1);
    expect(http.match((p) => p.url === '/api/recomendacion/recomendaciones')).toHaveLength(0);
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

    // P03.0: el inicio del panel es el dashboard; pide su registro (núcleo) y sus ofertas (catálogo).
    it('el prestador abre su inicio en /prestador y este pide su registro y sus ofertas', async () => {
      conSesion('PRESTADOR');

      await ir('/prestador', PanelPrestador);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-inicio-prestador')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/prestador/registro')).toHaveLength(1);
      expect(http.match((p) => p.url === '/api/catalogo/prestador/ofertas')).toHaveLength(1);
    });

    // Las secciones que llegan en los siguientes commits ya tienen su ruta dentro del panel.
    // P03.1: el RNT se lee en el núcleo.
    it('el prestador abre su RNT y este pide su registro', async () => {
      conSesion('PRESTADOR');

      await ir('/prestador/rnt', PanelPrestador);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-rnt-prestador')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/prestador/registro')).toHaveLength(1);
    });

    // P03.4: el WhatsApp de contacto se lee en el núcleo, con el registro.
    it('el prestador abre su WhatsApp y este pide su registro', async () => {
      conSesion('PRESTADOR');

      await ir('/prestador/whatsapp', PanelPrestador);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-whatsapp-prestador')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/prestador/registro')).toHaveLength(1);
    });

    // P03.5: la cuenta de cobro se lee en el núcleo.
    it('el prestador abre su cuenta de cobro y esta pide la cuenta vigente', async () => {
      conSesion('PRESTADOR');

      await ir('/prestador/cuenta-cobro', PanelPrestador);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-cuenta-cobro-prestador')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/prestador/cuenta-cobro')).toHaveLength(1);
    });

    // P03.2: el perfil público se lee en el catálogo.
    it('el prestador abre su perfil público y este pide el perfil al catálogo', async () => {
      conSesion('PRESTADOR');

      await ir('/prestador/perfil', PanelPrestador);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-perfil-prestador')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/catalogo/prestador/perfil')).toHaveLength(1);
    });

    it.each(['/prestador', '/prestador/rnt', '/prestador/perfil', '/prestador/whatsapp', '/prestador/cuenta-cobro'])(
      'un turista que abre %s ve "no tienes permiso"',
      async (url) => {
        conSesion('TURISTA');

        await ir(url, SinPermiso);
      },
    );

    // P09.1: el gestor entra a su panel; el panel de indicadores y la vista de cada dirección se crean dentro.
    it('el gestor abre su panel en /gestor y dentro se crean el panel de indicadores y la demanda, que pide el tablero', async () => {
      conSesion('GESTOR_DESTINO');

      await ir('/gestor', PanelGestor);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-panel-indicadores')).not.toBeNull();
      expect(harness.routeNativeElement?.querySelector('app-panel-indicadores app-demanda-gestor')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/gestor/indicadores/tablero')).toHaveLength(1);
      expect(http.match((p) => p.url === '/api/catalogo/zonas')).toHaveLength(1);
    });

    it('el gestor abre el embudo en /gestor/embudo y este pide el embudo del periodo', async () => {
      conSesion('GESTOR_DESTINO');

      await ir('/gestor/embudo', PanelGestor);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-panel-indicadores')).not.toBeNull();
      expect(harness.routeNativeElement?.querySelector('app-panel-indicadores app-embudo-gestor')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/gestor/indicadores/embudo')).toHaveLength(1);
    });

    it('el gestor abre la distribución en /gestor/distribucion y esta pide la distribución por categoría', async () => {
      conSesion('GESTOR_DESTINO');

      await ir('/gestor/distribucion', PanelGestor);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-panel-indicadores')).not.toBeNull();
      expect(harness.routeNativeElement?.querySelector('app-panel-indicadores app-distribucion-gestor')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/gestor/indicadores/categorias')).toHaveLength(1);
    });

    it.each(['/gestor', '/gestor/embudo', '/gestor/distribucion'])(
      'un turista que abre %s ve "no tienes permiso"',
      async (url) => {
        conSesion('TURISTA');

        await ir(url, SinPermiso);
      },
    );

    // P05.5: las zonas se leen y se escriben directo en el catálogo.
    it('el administrador abre las zonas y estas piden la lista del catálogo', async () => {
      conSesion('ADMINISTRADOR');

      await ir('/admin/zonas', PanelAdmin);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-zonas-admin')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/catalogo/admin/zonas')).toHaveLength(1);
    });

    // P05.1: el inicio del panel es la lista de prestadores; pide los prestadores y las cuentas para ponerle nombre al operador.
    it('el administrador abre los prestadores en /admin y estos piden sus datos', async () => {
      conSesion('ADMINISTRADOR');

      await ir('/admin', PanelAdmin);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-prestadores-rnt')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/admin/prestadores')).toHaveLength(1);
      expect(http.match((p) => p.url === '/api/nucleo/admin/cuentas-internas')).toHaveLength(1);
    });

    // P05.2: la cola de ofertas se lee en el catálogo (con las zonas, para ponerles nombre) y se decide en el núcleo.
    it('el administrador abre la moderación y esta pide la cola de ofertas del catálogo', async () => {
      conSesion('ADMINISTRADOR');

      await ir('/admin/moderacion', PanelAdmin);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-moderacion-admin')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/catalogo/admin/moderacion/ofertas')).toHaveLength(1);
      expect(http.match((p) => p.url === '/api/catalogo/admin/zonas')).toHaveLength(1);
    });

    // P05.3: las cuentas internas se leen del núcleo.
    it('el administrador abre las cuentas internas y estas piden la lista', async () => {
      conSesion('ADMINISTRADOR');

      await ir('/admin/cuentas', PanelAdmin);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-cuentas-internas')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/admin/cuentas-internas')).toHaveLength(1);
    });

    // P05.4: la comisión y los pagos tardíos se leen del núcleo; las cuentas ponen nombre a quien cambió el porcentaje.
    it('el administrador abre comisión y pagos y estos piden sus datos', async () => {
      conSesion('ADMINISTRADOR');

      await ir('/admin/comision', PanelAdmin);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-comision-pagos')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/admin/comision')).toHaveLength(1);
      expect(http.match((p) => p.url === '/api/nucleo/admin/cuentas-internas')).toHaveLength(1);
      expect(http.match((p) => p.url === '/api/nucleo/admin/pagos-tardios')).toHaveLength(1);
    });

    // Pendientes técnicos: avisos al catálogo rechazados y eventos de pago fallidos, cada uno con su lista.
    it('el administrador abre los pendientes técnicos y estos piden sus dos listas', async () => {
      conSesion('ADMINISTRADOR');

      await ir('/admin/pendientes', PanelAdmin);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-pendientes-tecnicos')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/admin/bandeja/rechazados')).toHaveLength(1);
      expect(http.match((p) => p.url === '/api/nucleo/admin/pagos/eventos-fallidos')).toHaveLength(1);
    });

    // Alertas de sesgo (RET-02): se crea dentro del panel y pide las alertas y los nombres de los prestadores.
    it('el administrador abre las alertas de sesgo y estas piden sus datos', async () => {
      conSesion('ADMINISTRADOR');

      await ir('/admin/alertas-sesgo', PanelAdmin);

      expect(harness.routeNativeElement?.querySelector('.contenido > app-alertas-sesgo')).not.toBeNull();
      expect(http.match((p) => p.url === '/api/nucleo/admin/alertas-sesgo')).toHaveLength(1);
      expect(http.match((p) => p.url === '/api/nucleo/admin/prestadores')).toHaveLength(1);
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
