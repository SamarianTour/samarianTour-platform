import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RespuestaSesion } from '../api/modelos-nucleo';
import { CLAVE_SESION, SesionGuardada, aSesionGuardada, guardarSesion } from './almacen-sesion';
import { MARGEN_RENOVACION_MS, SesionService } from './sesion.service';
import { respuestaDeSesion } from './testing';

const SESION = '/api/nucleo/cuentas/sesion';
const RENOVACION = `${SESION}/renovacion`;
const CIERRE = `${SESION}/cierre`;
const MINUTO = 60_000;

describe('SesionService', () => {
  let http: HttpTestingController;

  const servicio = () => TestBed.inject(SesionService);

  /** Deja una sesión guardada antes de que el servicio exista, como si la página se hubiera recargado. */
  function guardarAntes(
    respuesta: RespuestaSesion,
    persistencia: 'sesion' | 'local' = 'sesion',
  ): void {
    guardarSesion(aSesionGuardada(respuesta), persistencia);
  }

  beforeEach(() => {
    vi.useFakeTimers({ now: new Date('2026-11-08T12:00:00Z') });
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  describe('iniciar sesión', () => {
    it('envía el correo y la contraseña y deja la sesión abierta con su rol', () => {
      const respuesta = respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador: 'ALOJAMIENTO' });
      let cuenta: unknown;

      servicio()
        .iniciarSesion('laura@demo.samariantour.co', 'Demo1234', false)
        .subscribe((c) => (cuenta = c));

      const peticion = http.expectOne(SESION);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({
        correo: 'laura@demo.samariantour.co',
        contrasena: 'Demo1234',
      });
      peticion.flush(respuesta);
      expect(cuenta).toEqual(respuesta.cuenta);
      expect(servicio().autenticada()).toBe(true);
      expect(servicio().rol()).toBe('PRESTADOR');
      expect(servicio().cuenta()?.nombre).toBe('Laura');
      expect(servicio().tokenAcceso()).toBe(respuesta.tokenAcceso);
    });

    it('toma el tipo de prestador del token y no de la cuenta', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http
        .expectOne(SESION)
        .flush(respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador: 'ALOJAMIENTO' }));

      expect(servicio().tipoPrestador()).toBe('ALOJAMIENTO');
    });

    it('un turista no tiene tipo de prestador', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion());

      expect(servicio().tipoPrestador()).toBeNull();
    });

    it('sin "Recordarme" guarda la sesión solo en sessionStorage', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion());

      expect(sessionStorage.getItem(CLAVE_SESION)).not.toBeNull();
      expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('con "Recordarme" guarda la sesión solo en localStorage', () => {
      servicio().iniciarSesion('a@b.co', 'x', true).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion());

      expect(localStorage.getItem(CLAVE_SESION)).not.toBeNull();
      expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('si el servidor lo rechaza, no queda sesión ni se guarda nada', () => {
      let fallo: unknown;

      servicio()
        .iniciarSesion('a@b.co', 'mala', true)
        .subscribe({ error: (e: unknown) => (fallo = e) });
      http
        .expectOne(SESION)
        .flush(
          { status: 401, codigo: 'CREDENCIALES_INVALIDAS' },
          { status: 401, statusText: 'Unauthorized' },
        );

      expect(fallo).toBeDefined();
      expect(servicio().autenticada()).toBe(false);
      expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
      expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    });
  });

  describe('recargar la página', () => {
    it.each(['sesion', 'local'] as const)('recupera la sesión guardada en %s', (persistencia) => {
      guardarAntes(respuestaDeSesion({ rol: 'ADMINISTRADOR' }), persistencia);

      expect(servicio().autenticada()).toBe(true);
      expect(servicio().rol()).toBe('ADMINISTRADOR');
    });

    it('descarta una sesión cuya renovación ya venció', () => {
      const vencida: SesionGuardada = {
        ...aSesionGuardada(respuestaDeSesion()),
        renovacionExpiraEn: new Date(Date.now() - MINUTO).toISOString(),
      };
      guardarSesion(vencida, 'local');

      expect(servicio().autenticada()).toBe(false);
      expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('ignora un valor guardado que está dañado', () => {
      sessionStorage.setItem(CLAVE_SESION, '{"tokenAcceso": 5');

      expect(servicio().autenticada()).toBe(false);
    });

    it('ignora un valor guardado al que le faltan datos', () => {
      localStorage.setItem(CLAVE_SESION, JSON.stringify({ tokenAcceso: 'x' }));

      expect(servicio().autenticada()).toBe(false);
    });
  });

  describe('renovar', () => {
    function iniciar(numero = 1): void {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion({ numero }));
    }

    it('envía el token de renovación y reemplaza los dos tokens', () => {
      iniciar(1);
      const nueva = respuestaDeSesion({ numero: 2 });
      let token: string | undefined;

      servicio()
        .renovar()
        .subscribe((t) => (token = t));

      const peticion = http.expectOne(RENOVACION);
      expect(peticion.request.body).toEqual({ tokenRenovacion: 'renovacion-1' });
      peticion.flush(nueva);
      expect(token).toBe(nueva.tokenAcceso);
      expect(servicio().tokenAcceso()).toBe(nueva.tokenAcceso);
      expect(JSON.parse(sessionStorage.getItem(CLAVE_SESION)!).tokenRenovacion).toBe(
        'renovacion-2',
      );
    });

    it('guarda la renovación en el mismo lugar donde estaba la sesión', () => {
      servicio().iniciarSesion('a@b.co', 'x', true).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion({ numero: 1 }));

      servicio().renovar().subscribe();
      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));

      expect(JSON.parse(localStorage.getItem(CLAVE_SESION)!).tokenRenovacion).toBe('renovacion-2');
      expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('con varias peticiones a la vez hace una sola renovación y todas reciben el mismo token', () => {
      iniciar(1);
      const recibidos: string[] = [];

      servicio()
        .renovar()
        .subscribe((t) => recibidos.push(t));
      servicio()
        .renovar()
        .subscribe((t) => recibidos.push(t));
      servicio()
        .renovar()
        .subscribe((t) => recibidos.push(t));

      const nueva = respuestaDeSesion({ numero: 2 });
      http.expectOne(RENOVACION).flush(nueva);
      expect(recibidos).toEqual([nueva.tokenAcceso, nueva.tokenAcceso, nueva.tokenAcceso]);
    });

    it('permite renovar otra vez cuando la anterior terminó', () => {
      iniciar(1);
      servicio().renovar().subscribe();
      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));

      servicio().renovar().subscribe();

      const peticion = http.expectOne(RENOVACION);
      expect(peticion.request.body).toEqual({ tokenRenovacion: 'renovacion-2' });
      peticion.flush(respuestaDeSesion({ numero: 3 }));
    });

    it('si el servidor responde 401, la sesión ya no sirve y se descarta', () => {
      iniciar(1);
      let fallo: unknown;

      servicio()
        .renovar()
        .subscribe({ error: (e: unknown) => (fallo = e) });
      http
        .expectOne(RENOVACION)
        .flush(
          { status: 401, codigo: 'SESION_INVALIDA' },
          { status: 401, statusText: 'Unauthorized' },
        );

      expect(fallo).toBeDefined();
      expect(servicio().autenticada()).toBe(false);
      expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('si no hay conexión, conserva la sesión para intentarlo después', () => {
      iniciar(1);

      servicio()
        .renovar()
        .subscribe({ error: () => undefined });
      http.expectOne(RENOVACION).error(new ProgressEvent('error'));

      expect(servicio().autenticada()).toBe(true);
    });

    it('sin sesión falla sin llamar al servidor', () => {
      let fallo: unknown;

      servicio()
        .renovar()
        .subscribe({ error: (e: unknown) => (fallo = e) });

      expect(fallo).toBeDefined();
      http.expectNone(RENOVACION);
    });

    it('si otra pestaña ya renovó, usa sus tokens en lugar de gastar el viejo', () => {
      iniciar(1);
      const deOtraPestana = respuestaDeSesion({ numero: 2 });
      sessionStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(deOtraPestana)));
      let token: string | undefined;

      servicio()
        .renovar()
        .subscribe((t) => (token = t));

      http.expectNone(RENOVACION);
      expect(token).toBe(deOtraPestana.tokenAcceso);
      expect(servicio().tokenAcceso()).toBe(deOtraPestana.tokenAcceso);
    });
  });

  describe('renovación automática', () => {
    it('renueva un minuto antes de que venza el token de acceso', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion({ numero: 1 }));

      vi.advanceTimersByTime(14 * MINUTO - 1);
      http.expectNone(RENOVACION);
      vi.advanceTimersByTime(1);

      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));
    });

    it('después de renovar deja programada la siguiente', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion({ numero: 1 }));
      vi.advanceTimersByTime(14 * MINUTO);
      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));

      vi.advanceTimersByTime(14 * MINUTO);

      const peticion = http.expectOne(RENOVACION);
      expect(peticion.request.body).toEqual({ tokenRenovacion: 'renovacion-2' });
      peticion.flush(respuestaDeSesion({ numero: 3 }));
    });

    it('al reanudar con un token de acceso vigente solo programa la renovación', () => {
      guardarAntes(respuestaDeSesion({ numero: 1 }));
      let terminó = false;

      servicio()
        .reanudar()
        .subscribe(() => (terminó = true));

      expect(terminó).toBe(true);
      http.expectNone(RENOVACION);
      vi.advanceTimersByTime(14 * MINUTO);
      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));
    });

    it('al reanudar con un token de acceso por vencer lo renueva de inmediato', () => {
      const casiVencido: SesionGuardada = {
        ...aSesionGuardada(respuestaDeSesion({ numero: 1 })),
        accesoExpiraEn: new Date(Date.now() + MARGEN_RENOVACION_MS / 2).toISOString(),
      };
      guardarSesion(casiVencido, 'sesion');
      let terminó = false;

      servicio()
        .reanudar()
        .subscribe(() => (terminó = true));

      http.expectOne(RENOVACION).flush(respuestaDeSesion({ numero: 2 }));
      expect(terminó).toBe(true);
      expect(servicio().tokenAcceso()).toBe(respuestaDeSesion({ numero: 2 }).tokenAcceso);
    });

    it('al reanudar no falla aunque la renovación sea rechazada', () => {
      const vencido: SesionGuardada = {
        ...aSesionGuardada(respuestaDeSesion({ numero: 1 })),
        accesoExpiraEn: new Date(Date.now() - MINUTO).toISOString(),
      };
      guardarSesion(vencido, 'sesion');
      let fallo: unknown;
      let terminó = false;

      servicio()
        .reanudar()
        .subscribe({ next: () => (terminó = true), error: (e: unknown) => (fallo = e) });
      http
        .expectOne(RENOVACION)
        .flush({ status: 401, codigo: 'SESION_INVALIDA' }, { status: 401, statusText: 'x' });

      expect(fallo).toBeUndefined();
      expect(terminó).toBe(true);
      expect(servicio().autenticada()).toBe(false);
    });

    it('sin sesión, reanudar no hace nada', () => {
      servicio().reanudar().subscribe();

      http.expectNone(RENOVACION);
    });
  });

  describe('cerrar sesión', () => {
    it('avisa al servidor con el token de renovación y borra la sesión', () => {
      servicio().iniciarSesion('a@b.co', 'x', true).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion({ numero: 1 }));
      let terminó = false;

      servicio()
        .cerrarSesion()
        .subscribe(() => (terminó = true));

      const peticion = http.expectOne(CIERRE);
      expect(peticion.request.body).toEqual({ tokenRenovacion: 'renovacion-1' });
      peticion.flush(null, { status: 204, statusText: 'No Content' });
      expect(terminó).toBe(true);
      expect(servicio().autenticada()).toBe(false);
      expect(servicio().cuenta()).toBeNull();
      expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('la sesión local queda cerrada aunque el servidor no responda', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion());
      let terminó = false;

      servicio()
        .cerrarSesion()
        .subscribe(() => (terminó = true));
      http.expectOne(CIERRE).error(new ProgressEvent('error'));

      expect(terminó).toBe(true);
      expect(servicio().autenticada()).toBe(false);
      expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('cancela la renovación programada', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion());
      servicio().cerrarSesion().subscribe();
      http.expectOne(CIERRE).flush(null, { status: 204, statusText: 'No Content' });

      vi.advanceTimersByTime(30 * MINUTO);

      http.expectNone(RENOVACION);
    });

    it('sin sesión no llama al servidor', () => {
      servicio().cerrarSesion().subscribe();

      http.expectNone(CIERRE);
    });
  });

  describe('cuenta', () => {
    it('actualizarCuenta cambia la cuenta y lo guarda sin tocar los tokens', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion({ numero: 1 }));

      servicio().actualizarCuenta({ ...servicio().cuenta()!, nombre: 'Laura Gómez Ruiz' });

      expect(servicio().cuenta()?.nombre).toBe('Laura Gómez Ruiz');
      const guardada = JSON.parse(sessionStorage.getItem(CLAVE_SESION)!);
      expect(guardada.cuenta.nombre).toBe('Laura Gómez Ruiz');
      expect(guardada.tokenRenovacion).toBe('renovacion-1');
    });
  });

  describe('otras pestañas', () => {
    function avisarCambio(valor: string | null): void {
      window.dispatchEvent(
        new StorageEvent('storage', {
          key: CLAVE_SESION,
          newValue: valor,
          storageArea: localStorage,
        }),
      );
    }

    it('al cerrar la sesión en otra pestaña, esta también se cierra', () => {
      servicio().iniciarSesion('a@b.co', 'x', true).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion());

      avisarCambio(null);

      expect(servicio().autenticada()).toBe(false);
    });

    it('al renovar otra pestaña, esta adopta los tokens nuevos', () => {
      servicio().iniciarSesion('a@b.co', 'x', true).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion({ numero: 1 }));
      const nueva = respuestaDeSesion({ numero: 2 });

      avisarCambio(JSON.stringify(aSesionGuardada(nueva)));

      expect(servicio().tokenAcceso()).toBe(nueva.tokenAcceso);
    });

    it('una pestaña sin sesión toma la que se abrió en otra', () => {
      const abierta = respuestaDeSesion({ rol: 'GESTOR_DESTINO' });
      servicio();

      avisarCambio(JSON.stringify(aSesionGuardada(abierta)));

      expect(servicio().rol()).toBe('GESTOR_DESTINO');
    });

    it('una sesión que no se recuerda (sessionStorage) no se ve afectada por otras pestañas', () => {
      servicio().iniciarSesion('a@b.co', 'x', false).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion({ numero: 1 }));

      avisarCambio(null);
      avisarCambio(JSON.stringify(aSesionGuardada(respuestaDeSesion({ numero: 9 }))));

      expect(servicio().autenticada()).toBe(true);
      expect(servicio().tokenAcceso()).toBe(respuestaDeSesion({ numero: 1 }).tokenAcceso);
    });

    function iniciarRecordando(): SesionService {
      const sesion = servicio();
      sesion.iniciarSesion('a@b.co', 'x', true).subscribe();
      http.expectOne(SESION).flush(respuestaDeSesion());
      return sesion;
    }

    it('ignora los cambios de otras claves del almacenamiento', () => {
      const sesion = iniciarRecordando();

      window.dispatchEvent(
        new StorageEvent('storage', { key: 'otra', newValue: null, storageArea: localStorage }),
      );

      expect(sesion.autenticada()).toBe(true);
    });

    it('un valor dañado en el almacenamiento no cierra la sesión', () => {
      const sesion = iniciarRecordando();

      avisarCambio('{dañado');

      expect(sesion.autenticada()).toBe(true);
    });
  });
});
