import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { CuentaInterna, PrestadorAdmin } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { idiomaInterceptor } from '../../../core/idioma/idioma.interceptor';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { sesionInterceptor } from '../../../core/sesion/sesion.interceptor';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { PrestadoresRnt } from './prestadores-rnt';

const PRESTADORES = '/api/nucleo/admin/prestadores';
const CUENTAS = '/api/nucleo/admin/cuentas-internas';
const SINCRONIZACION = '/api/nucleo/admin/rnt/sincronizacion';

function cuenta(id: string, nombre: string, extra: Partial<CuentaInterna> = {}): CuentaInterna {
  return {
    id,
    rol: 'OPERADOR_DISPONIBILIDAD',
    nombre,
    correo: `${id}@samariantour.co`,
    estado: 'ACTIVA',
    contrasenaEstablecida: true,
    registradaEn: '2026-01-01T00:00:00Z',
    ...extra,
  };
}

function prestador(id: string, razonSocial: string | undefined, extra: Partial<PrestadorAdmin> = {}): PrestadorAdmin {
  return {
    id,
    numeroRnt: '910005',
    razonSocial,
    tipo: 'AGENCIA_OPERADORA',
    estadoRnt: 'ACTIVO',
    registradoEn: '2026-10-30T14:00:00',
    ...extra,
  };
}

const CUENTAS_DEMO = [
  cuenta('op-beto', 'Beto Ruiz'),
  cuenta('op-ana', 'Ana Gómez'),
  cuenta('op-diana', 'Diana Soto', { estado: 'DESACTIVADA' }),
  cuenta('ges-1', 'Gestor Uno', { rol: 'GESTOR_DESTINO' }),
  cuenta('adm-1', 'Administradora', { rol: 'ADMINISTRADOR' }),
];

const SIN_OPERADOR = prestador('p1', 'TURISMO SIERRA NEVADA LTDA.');
const CON_OPERADOR = prestador('p2', 'Hotel Mar', { operadorId: 'op-ana', tipo: 'ALOJAMIENTO' });
const CON_DESACTIVADO = prestador('p3', undefined, { operadorId: 'op-diana', estadoRnt: 'INACTIVO', numeroRnt: '910009' });

describe('PrestadoresRnt (P05.1)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const filas = () => Array.from(raiz().querySelectorAll<HTMLElement>('tbody tr'));
  const selector = (fila: number) => filas()[fila].querySelector<HTMLSelectElement>('select')!;
  const botonAsignar = (fila: number) => filas()[fila].querySelector<HTMLButtonElement>('.asignacion button')!;
  const botonSync = () => raiz().querySelector<HTMLButtonElement>('.sync__accion button')!;
  const listaDePrestadores = () => http.expectOne((p) => p.url === PRESTADORES);

  /** Navega a la dirección y devuelve las dos consultas de la pantalla. */
  async function abrir(url = '/admin'): Promise<{ prestadores: TestRequest; cuentas: TestRequest }> {
    await harness.navigateByUrl(url, PrestadoresRnt);
    harness.detectChanges();
    return { prestadores: listaDePrestadores(), cuentas: http.expectOne(CUENTAS) };
  }

  async function crear(
    prestadores: PrestadorAdmin[] = [SIN_OPERADOR, CON_OPERADOR, CON_DESACTIVADO],
    cuentas: CuentaInterna[] = CUENTAS_DEMO,
    url = '/admin',
  ): Promise<void> {
    const consultas = await abrir(url);
    consultas.prestadores.flush(prestadores);
    consultas.cuentas.flush(cuentas);
    harness.detectChanges();
  }

  /** Al terminar una sincronización la pantalla vuelve a leer la lista y las cuentas: se responden aquí. */
  function responderRecarga(prestadores: PrestadorAdmin[] = [SIN_OPERADOR], cuentas: CuentaInterna[] = CUENTAS_DEMO): void {
    listaDePrestadores().flush(prestadores);
    http.expectOne(CUENTAS).flush(cuentas);
    harness.detectChanges();
  }

  function elegir(fila: number, operadorId: string): void {
    const lista = selector(fila);
    lista.value = operadorId;
    lista.dispatchEvent(new Event('change'));
    harness.detectChanges();
  }

  beforeAll(() => {
    // jsdom no trae el comportamiento del <dialog>: se imita lo mínimo que usa el componente.
    HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    };
    HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
      this.removeAttribute('open');
      this.dispatchEvent(new Event('close'));
    };
  });

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'ADMINISTRADOR' }))),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'admin', component: PrestadoresRnt }]),
        // La cadena de interceptores de la aplicación (app.config.ts): ninguna le pone tiempo máximo a una petición.
        provideHttpClient(withInterceptors([idiomaInterceptor, sesionInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  describe('la lista', () => {
    it('pide todos los prestadores y las cuentas internas, de donde salen los nombres de los operadores', async () => {
      const { prestadores, cuentas } = await abrir();

      expect(prestadores.request.params.has('sinOperador')).toBe(false);
      expect(cuentas.request.method).toBe('GET');
      prestadores.flush([]);
      cuentas.flush([]);
    });

    it('muestra la razón social, el RNT, el tipo, el estado frente al RNT y cuándo se registró', async () => {
      await crear();

      expect(filas()).toHaveLength(3);
      expect(filas()[0].textContent).toContain('TURISMO SIERRA NEVADA LTDA.');
      expect(filas()[0].textContent).toContain('RNT n.º 910005');
      expect(filas()[0].textContent).toContain('Agencia operadora');
      expect(filas()[0].querySelector('.etiqueta')?.textContent?.trim()).toBe('Activo');
      expect(filas()[0].textContent).toContain('30 oct 2026');
      expect(filas()[1].textContent).toContain('Alojamiento');
      expect(filas()[2].querySelector('.etiqueta')?.textContent?.trim()).toBe('Inactivo');
    });

    it('un prestador sin razón social del RNT lo dice', async () => {
      await crear();

      expect(filas()[2].textContent).toContain('Sin razón social en el RNT');
    });

    it('dice cuántos prestadores hay, en singular y plural', async () => {
      await crear([SIN_OPERADOR]);
      expect(texto()).toContain('1 prestador');
      expect(texto()).not.toContain('1 prestadores');
    });

    it('con tres prestadores usa el plural', async () => {
      await crear();

      expect(texto()).toContain('3 prestadores');
    });

    it('sin prestadores lo dice', async () => {
      await crear([]);

      expect(raiz().querySelector('table')).toBeNull();
      expect(texto()).toContain('No hay prestadores');
    });

    it('si la consulta falla avisa del error', async () => {
      const { prestadores, cuentas } = await abrir();
      prestadores.error(new ProgressEvent('error'));
      cuentas.flush(CUENTAS_DEMO);
      harness.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    });
  });

  describe('el filtro "sin operador"', () => {
    it('pide solo los que faltan por asignar y marca el filtro', async () => {
      const { prestadores, cuentas } = await abrir('/admin?sinOperador=true');

      expect(prestadores.request.params.get('sinOperador')).toBe('true');
      prestadores.flush([SIN_OPERADOR]);
      cuentas.flush(CUENTAS_DEMO);
      harness.detectChanges();

      const filtros = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.filtros a'));
      expect(filtros.map((a) => [a.textContent?.trim(), a.getAttribute('aria-current')])).toEqual([
        ['Todos', null],
        ['Sin operador', 'page'],
      ]);
      expect(filas()).toHaveLength(1);
    });

    it('sin filtro marca "Todos" y el enlace de "Sin operador" lleva a ?sinOperador=true', async () => {
      await crear();

      const filtros = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.filtros a'));
      expect(filtros[0].getAttribute('aria-current')).toBe('page');
      expect(filtros[1].getAttribute('href')).toBe('/admin?sinOperador=true');
    });

    it('si todos tienen operador lo celebra en vez de mostrar una tabla vacía', async () => {
      await crear([], CUENTAS_DEMO, '/admin?sinOperador=true');

      expect(texto()).toContain('Todos tienen operador');
    });
  });

  describe('asignar un operador', () => {
    it('la lista muestra el NOMBRE de cada operador activo, nunca su identificador', async () => {
      await crear();

      const opciones = Array.from(selector(0).options).map((o) => o.textContent?.trim());
      expect(opciones).toEqual(['Elige un operador', 'Ana Gómez', 'Beto Ruiz']);
      expect(selector(0).textContent).not.toContain('op-');
      // El valor interno es el identificador, pero lo que se lee y se anuncia es el nombre.
      expect(Array.from(selector(0).options).map((o) => o.value)).toEqual(['', 'op-ana', 'op-beto']);
    });

    it('no ofrece gestores, administradores ni operadores desactivados', async () => {
      await crear();

      const texto = selector(0).textContent ?? '';
      expect(texto).not.toContain('Diana');
      expect(texto).not.toContain('Gestor');
      expect(texto).not.toContain('Administradora');
    });

    it('un prestador con operador lo muestra elegido, por su nombre', async () => {
      await crear();

      expect(selector(1).selectedOptions[0].textContent?.trim()).toBe('Ana Gómez');
      expect(filas()[1].textContent).not.toContain('op-ana');
    });

    it('si su operador está desactivado lo muestra con esa aclaración y sin poder elegirlo', async () => {
      await crear();

      const actual = selector(2).selectedOptions[0];
      expect(actual.textContent?.trim()).toBe('Diana Soto (desactivado)');
      expect(actual.disabled).toBe(true);
    });

    it('cada lista tiene una etiqueta para lectores de pantalla con el nombre del prestador', async () => {
      await crear();

      expect(selector(0).getAttribute('aria-label')).toBe('Operador de TURISMO SIERRA NEVADA LTDA.');
    });

    it('el botón Asignar espera a que se elija un operador distinto del actual', async () => {
      await crear();

      expect(botonAsignar(0).disabled).toBe(true);
      expect(botonAsignar(1).disabled).toBe(true);
      elegir(0, 'op-ana');
      expect(botonAsignar(0).disabled).toBe(false);
      elegir(1, 'op-beto');
      expect(botonAsignar(1).disabled).toBe(false);
      elegir(1, 'op-ana');
      expect(botonAsignar(1).disabled).toBe(true);
    });

    it('asigna con PUT y solo el identificador, actualiza la fila y avisa con los nombres', async () => {
      await crear();
      elegir(0, 'op-ana');

      botonAsignar(0).click();
      harness.detectChanges();
      expect(botonAsignar(0).textContent?.trim()).toBe('Asignando…');
      const peticion = http.expectOne(`${PRESTADORES}/p1/operador`);
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({ operadorId: 'op-ana' });
      peticion.flush({ ...SIN_OPERADOR, operadorId: 'op-ana' });
      harness.detectChanges();

      expect(selector(0).selectedOptions[0].textContent?.trim()).toBe('Ana Gómez');
      expect(botonAsignar(0).disabled).toBe(true);
      expect(mostrar).toHaveBeenCalledWith(
        'Ana Gómez atenderá las solicitudes nuevas de TURISMO SIERRA NEVADA LTDA..',
      );
    });

    it('con el filtro "sin operador", el prestador asignado sale de la lista', async () => {
      await crear([SIN_OPERADOR, prestador('p4', 'Otro')], CUENTAS_DEMO, '/admin?sinOperador=true');
      elegir(0, 'op-beto');

      botonAsignar(0).click();
      http.expectOne(`${PRESTADORES}/p1/operador`).flush({ ...SIN_OPERADOR, operadorId: 'op-beto' });
      harness.detectChanges();

      expect(filas()).toHaveLength(1);
      expect(filas()[0].textContent).toContain('Otro');
    });

    it('mientras se envía no deja asignar otro, y un doble clic envía una sola petición', async () => {
      await crear();
      elegir(0, 'op-ana');
      elegir(1, 'op-beto');

      botonAsignar(0).click();
      botonAsignar(0).click();
      harness.detectChanges();

      expect(http.match(`${PRESTADORES}/p1/operador`)).toHaveLength(1);
      expect(botonAsignar(1).disabled).toBe(true);
    });

    it('si el operador ya no es válido, lo explica en esa fila y vuelve a leer los datos', async () => {
      await crear();
      elegir(0, 'op-ana');

      botonAsignar(0).click();
      http
        .expectOne(`${PRESTADORES}/p1/operador`)
        .flush({ codigo: 'OPERADOR_NO_VALIDO' }, { status: 409, statusText: 'Conflict' });
      harness.detectChanges();

      expect(filas()[0].querySelector('.campo__error')?.textContent).toContain(
        'no es la de un operador de disponibilidad activo',
      );
      // Vuelve a leer todo, por si la cuenta se desactivó mientras tanto.
      const { prestadores, cuentas } = { prestadores: listaDePrestadores(), cuentas: http.expectOne(CUENTAS) };
      prestadores.flush([SIN_OPERADOR]);
      cuentas.flush([cuenta('op-beto', 'Beto Ruiz')]);
      harness.detectChanges();
      expect(filas()[0].querySelector('.campo__error')).not.toBeNull();
      expect(Array.from(selector(0).options).map((o) => o.textContent?.trim())).toEqual([
        'Elige un operador',
        'Beto Ruiz',
      ]);
    });

    it('el aviso del error de una fila desaparece al elegir otro operador', async () => {
      await crear();
      elegir(0, 'op-ana');
      botonAsignar(0).click();
      http
        .expectOne(`${PRESTADORES}/p1/operador`)
        .flush({ codigo: 'OPERADOR_NO_VALIDO' }, { status: 409, statusText: 'Conflict' });
      listaDePrestadores().flush([SIN_OPERADOR]);
      http.expectOne(CUENTAS).flush(CUENTAS_DEMO);
      harness.detectChanges();

      elegir(0, 'op-beto');

      expect(filas()[0].querySelector('.campo__error')).toBeNull();
    });

    it('un error cualquiera se avisa en la fila y deja reintentar', async () => {
      await crear();
      elegir(0, 'op-ana');

      botonAsignar(0).click();
      http.expectOne(`${PRESTADORES}/p1/operador`).error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(filas()[0].querySelector('.campo__error')).not.toBeNull();
      expect(botonAsignar(0).disabled).toBe(false);
    });

    it('aclara que la asignación rige para las solicitudes nuevas', async () => {
      await crear();

      expect(texto()).toContain('rige para las solicitudes que se envíen desde ahora');
    });

    it('sin operadores activos lo dice y manda a crear uno', async () => {
      await crear([SIN_OPERADOR], [cuenta('op-diana', 'Diana Soto', { estado: 'DESACTIVADA' })]);

      expect(texto()).toContain('No hay operadores activos para asignar');
      expect(selector(0).options).toHaveLength(1);
    });

    it('si no se pueden leer las cuentas avisa y no deja asignar, pero la lista se ve', async () => {
      const { prestadores, cuentas } = await abrir();
      prestadores.flush([SIN_OPERADOR]);
      cuentas.error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(filas()).toHaveLength(1);
      expect(texto()).toContain('No pudimos leer los operadores');
      expect(selector(0).disabled).toBe(true);
    });
  });

  describe('sincronizar con el RNT', () => {
    const RESULTADO = { revisados: 5, retirados: 1, restituidos: 0, sinRespuesta: 0, interrumpida: false };

    /** Abre el diálogo y lo confirma. */
    function confirmarSincronizacion(): void {
      botonSync().click();
      harness.detectChanges();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();
      harness.detectChanges();
    }

    it('antes de sincronizar pide confirmación y explica qué pasa con los prestadores retirados', async () => {
      await crear();

      botonSync().click();
      harness.detectChanges();

      const dialogo = raiz().querySelector('dialog')!;
      expect(dialogo.hasAttribute('open')).toBe(true);
      expect(dialogo.textContent).toContain('¿Sincronizar con el RNT ahora?');
      expect(dialogo.textContent).toContain('saldrán del catálogo');
      http.expectNone(SINCRONIZACION);
    });

    it('si se cancela el diálogo no sincroniza', async () => {
      await crear();
      botonSync().click();
      harness.detectChanges();

      raiz().querySelector<HTMLButtonElement>('dialog .boton--secundario')!.click();
      harness.detectChanges();

      http.expectNone(SINCRONIZACION);
      expect(botonSync().disabled).toBe(false);
    });

    it('al confirmar pide la sincronización y, mientras responde, el botón queda deshabilitado con "Sincronizando…"', async () => {
      await crear();

      confirmarSincronizacion();

      const peticion = http.expectOne(SINCRONIZACION);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toBeNull();
      expect(botonSync().disabled).toBe(true);
      expect(botonSync().textContent?.trim()).toBe('Sincronizando…');
      expect(raiz().querySelector('.sync')?.getAttribute('aria-busy')).toBe('true');
      expect(texto()).toContain('Esto puede tardar un poco');
      peticion.flush(RESULTADO);
      responderRecarga();
    });

    it('con la sincronización en curso no se puede volver a pedir', async () => {
      await crear();
      confirmarSincronizacion();

      botonSync().click();
      harness.detectChanges();

      expect(raiz().querySelector('dialog')?.hasAttribute('open')).toBe(false);
      const pendientes = http.match(SINCRONIZACION);
      expect(pendientes).toHaveLength(1);
      pendientes[0].flush(RESULTADO);
      responderRecarga();
    });

    it('al terminar muestra el resultado, avisa y vuelve a leer la lista', async () => {
      await crear();
      confirmarSincronizacion();

      http.expectOne(SINCRONIZACION).flush(RESULTADO);
      harness.detectChanges();

      expect(botonSync().disabled).toBe(false);
      expect(botonSync().textContent?.trim()).toBe('Sincronizar ahora');
      expect(texto()).toContain('Resultado de la sincronización');
      expect(texto()).toContain('Revisados5');
      expect(texto()).toContain('Retirados del catálogo1');
      expect(mostrar).toHaveBeenCalledWith(
        'Sincronización con el RNT terminada: 5 revisados, 1 retirados, 0 restituidos.',
      );
      // Los estados del RNT pudieron cambiar.
      listaDePrestadores().flush([SIN_OPERADOR]);
      http.expectOne(CUENTAS).flush(CUENTAS_DEMO);
    });

    it('sin retirados ni restituidos dice que no hubo cambios', async () => {
      await crear();
      confirmarSincronizacion();

      http.expectOne(SINCRONIZACION).flush({ ...RESULTADO, retirados: 0 });
      listaDePrestadores().flush([]);
      http.expectOne(CUENTAS).flush([]);
      harness.detectChanges();

      expect(texto()).toContain('No hubo cambios en el catálogo.');
    });

    it('si el RNT dejó de responder, lo explica (QAS-03)', async () => {
      await crear();
      confirmarSincronizacion();

      http.expectOne(SINCRONIZACION).flush({ ...RESULTADO, interrumpida: true, sinRespuesta: 3 });
      listaDePrestadores().flush([]);
      http.expectOne(CUENTAS).flush([]);
      harness.detectChanges();

      expect(texto()).toContain('El RNT dejó de responder y la sincronización se detuvo');
      expect(texto()).toContain('Cada prestador conserva su último estado');
    });

    it('si algunos no respondieron, avisa que conservaron su último estado', async () => {
      await crear();
      confirmarSincronizacion();

      http.expectOne(SINCRONIZACION).flush({ ...RESULTADO, sinRespuesta: 2 });
      listaDePrestadores().flush([]);
      http.expectOne(CUENTAS).flush([]);
      harness.detectChanges();

      expect(texto()).toContain('Algunos prestadores conservaron su último estado');
    });

    it('si falla, lo avisa y vuelve a habilitar el botón', async () => {
      await crear();
      confirmarSincronizacion();

      http.expectOne(SINCRONIZACION).error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(raiz().querySelector('.sync .alerta--error')).not.toBeNull();
      expect(botonSync().disabled).toBe(false);
    });

    it('NINGÚN tiempo máximo del frontend corta la petición: sigue esperando después de media hora', async () => {
      await crear();
      confirmarSincronizacion();
      const peticion = http.expectOne(SINCRONIZACION);

      // Los temporizadores falsos se instalan después de crear el servicio de sesión, para no disparar su renovación real.
      vi.useFakeTimers();
      vi.advanceTimersByTime(30 * 60_000);
      harness.detectChanges();

      expect(peticion.cancelled).toBe(false);
      expect(botonSync().disabled).toBe(true);
      expect(botonSync().textContent?.trim()).toBe('Sincronizando…');
      expect(raiz().querySelector('.sync .alerta--error')).toBeNull();

      peticion.flush(RESULTADO);
      harness.detectChanges();
      expect(texto()).toContain('Resultado de la sincronización');
      listaDePrestadores().flush([]);
      http.expectOne(CUENTAS).flush([]);
    });

    it('la petición no se cancela al salir de la pantalla: el resultado llega y se avisa igual', async () => {
      await crear();
      confirmarSincronizacion();
      const peticion = http.expectOne(SINCRONIZACION);

      harness.fixture.destroy();

      expect(peticion.cancelled).toBe(false);
      peticion.flush(RESULTADO);
      expect(mostrar).toHaveBeenCalledWith(
        'Sincronización con el RNT terminada: 5 revisados, 1 retirados, 0 restituidos.',
      );
    });
  });

  describe('en inglés', () => {
    it('todo cambia al idioma: estados, columnas, nombres y botones', async () => {
      establecerIdioma('en', { recordar: false });
      await crear();

      expect(texto()).toContain('Providers and RNT');
      expect(texto()).toContain('Synchronize with the RNT');
      expect(texto()).toContain('3 providers');
      expect(filas()[0].querySelector('.etiqueta')?.textContent?.trim()).toBe('Active');
      expect(filas()[2].querySelector('.etiqueta')?.textContent?.trim()).toBe('Inactive');
      expect(Array.from(selector(0).options).map((o) => o.textContent?.trim())).toEqual([
        'Choose an operator',
        'Ana Gómez',
        'Beto Ruiz',
      ]);
      expect(selector(2).selectedOptions[0].textContent?.trim()).toBe('Diana Soto (deactivated)');
      expect(botonAsignar(0).textContent?.trim()).toBe('Assign');
      expect(botonSync().textContent?.trim()).toBe('Synchronize now');
    });
  });
});
