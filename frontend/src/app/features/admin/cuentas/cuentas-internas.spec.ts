import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { CuentaInterna } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { CuentasInternas } from './cuentas-internas';

const CUENTAS = '/api/nucleo/admin/cuentas-internas';

function cuenta(id: string, rol: CuentaInterna['rol'], nombre: string, extra: Partial<CuentaInterna> = {}): CuentaInterna {
  return {
    id,
    rol,
    nombre,
    correo: `${id}@samariantour.co`,
    estado: 'ACTIVA',
    contrasenaEstablecida: true,
    registradaEn: '2026-01-01T00:00:00',
    ...extra,
  };
}

/** El identificador de la cuenta de la sesión de pruebas (`respuestaDeSesion`). */
const MI_ID = '70000000-0000-0000-0000-000000000007';

const YO = cuenta(MI_ID, 'ADMINISTRADOR', 'Marta Ríos');
const OPERADOR = cuenta('op1', 'OPERADOR_DISPONIBILIDAD', 'Operador Uno');
const GESTOR = cuenta('ge1', 'GESTOR_DESTINO', 'Gestor Uno');
const DESACTIVADO = cuenta('op2', 'OPERADOR_DISPONIBILIDAD', 'Operador Dos', { estado: 'DESACTIVADA' });
const NUEVA = cuenta('nueva', 'OPERADOR_DISPONIBILIDAD', 'Valentina Ríos', {
  correo: 'valentina@samariantour.co',
  contrasenaEstablecida: false,
});
const TODAS = [YO, OPERADOR, GESTOR, DESACTIVADO];

describe('CuentasInternas (P05.3)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const filas = () => Array.from(raiz().querySelectorAll<HTMLElement>('tbody tr'));
  const botonDeFila = (fila: number) => filas()[fila].querySelector<HTMLButtonElement>('.accion button');
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#cuenta-${id}`)!;

  async function crear(cuentas: CuentaInterna[] = TODAS, url = '/admin/cuentas'): Promise<void> {
    await harness.navigateByUrl(url, CuentasInternas);
    harness.detectChanges();
    http.expectOne(CUENTAS).flush(cuentas);
    harness.detectChanges();
  }

  function escribir(id: string, valor: string): void {
    const entrada = campo(id);
    entrada.value = valor;
    entrada.dispatchEvent(new Event(entrada.tagName === 'SELECT' ? 'change' : 'input'));
    harness.detectChanges();
  }

  function llenarFormulario(): void {
    escribir('rol', 'OPERADOR_DISPONIBILIDAD');
    escribir('nombre', '  Valentina Ríos ');
    escribir('correo', ' valentina@samariantour.co ');
  }

  function enviarFormulario(): void {
    raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
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
        provideRouter([{ path: 'admin/cuentas', component: CuentasInternas }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  describe('la lista', () => {
    it('muestra todas las cuentas, activas y desactivadas, con su rol, su estado y su correo', async () => {
      await crear();

      expect(filas()).toHaveLength(4);
      expect(filas()[1].textContent).toContain('Operador Uno');
      expect(filas()[1].textContent).toContain('op1@samariantour.co');
      expect(filas()[1].textContent).toContain('Operador de disponibilidad');
      expect(filas()[1].querySelector('.etiqueta')?.textContent?.trim()).toBe('Activa');
      expect(filas()[2].textContent).toContain('Gestor del destino');
      expect(filas()[3].querySelector('.etiqueta')?.textContent?.trim()).toBe('Desactivada');
      expect(texto()).toContain('4 cuentas');
    });

    it('marca la propia cuenta con "Tú"', async () => {
      await crear();

      expect(filas()[0].textContent).toContain('Tú');
      expect(filas()[1].textContent).not.toContain('Tú');
    });

    it('las cuentas de administrador solo se consultan: no tienen botones', async () => {
      await crear();

      expect(filas()[0].querySelector('button')).toBeNull();
      expect(filas()[0].textContent).toContain('Solo consulta');
    });

    it('una cuenta que todavía no puso su contraseña lo dice', async () => {
      await crear([YO, NUEVA]);

      expect(filas()[1].textContent).toContain('Aún no puso su contraseña');
      expect(filas()[1].textContent).toContain('Recuperar el acceso');
      expect(filas()[0].textContent).not.toContain('Aún no puso');
    });

    it('si la consulta falla avisa del error', async () => {
      await harness.navigateByUrl('/admin/cuentas', CuentasInternas);
      harness.detectChanges();
      http.expectOne(CUENTAS).error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    });
  });

  describe('el filtro por rol', () => {
    it('?rol= muestra solo las cuentas de ese rol y marca el filtro', async () => {
      await crear(TODAS, '/admin/cuentas?rol=OPERADOR_DISPONIBILIDAD');

      expect(filas().map((f) => f.querySelector('.nombre')?.textContent?.trim())).toEqual(['Operador Uno', 'Operador Dos']);
      const enlaces = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.filtros a'));
      expect(enlaces.map((a) => [a.textContent?.trim(), a.getAttribute('aria-current')])).toEqual([
        ['Todas', null],
        ['Operadores', 'page'],
        ['Gestores', null],
        ['Administradores', null],
      ]);
      expect(texto()).toContain('2 cuentas');
    });

    it('sin filtro marca "Todas" y los enlaces llevan a su rol', async () => {
      await crear();

      const enlaces = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.filtros a'));
      expect(enlaces[0].getAttribute('aria-current')).toBe('page');
      expect(enlaces[0].getAttribute('href')).toBe('/admin/cuentas');
      expect(enlaces[2].getAttribute('href')).toBe('/admin/cuentas?rol=GESTOR_DESTINO');
    });

    it('un rol que no es interno se ignora', async () => {
      await crear(TODAS, '/admin/cuentas?rol=TURISTA');

      expect(filas()).toHaveLength(4);
    });

    it('un filtro sin cuentas lo dice', async () => {
      await crear([YO], '/admin/cuentas?rol=GESTOR_DESTINO');

      expect(texto()).toContain('No hay cuentas con este filtro.');
    });
  });

  describe('crear una cuenta', () => {
    it('solo ofrece los roles de operador de disponibilidad y gestor del destino', async () => {
      await crear();

      const opciones = Array.from(campo('rol').querySelectorAll('option')).map((o) => [o.value, o.textContent?.trim()]);
      expect(opciones).toEqual([
        ['', 'Elige un rol'],
        ['OPERADOR_DISPONIBILIDAD', 'Operador de disponibilidad'],
        ['GESTOR_DESTINO', 'Gestor del destino'],
      ]);
    });

    it('con el formulario vacío no envía, señala lo que falta y lleva el foco al primero', async () => {
      await crear();

      enviarFormulario();

      http.expectNone(CUENTAS);
      expect(texto()).toContain('Elige el rol de la cuenta.');
      expect(texto()).toContain('Escribe el nombre.');
      expect(texto()).toContain('Escribe el correo.');
      expect(document.activeElement).toBe(campo('rol'));
      expect(campo('nombre').getAttribute('aria-invalid')).toBe('true');
    });

    it('un correo sin forma de correo no se envía', async () => {
      await crear();
      llenarFormulario();
      escribir('correo', 'sin-arroba');

      enviarFormulario();

      http.expectNone(CUENTAS);
      expect(texto()).toContain('Escribe un correo válido');
    });

    it('envía el rol, el nombre y el correo sin espacios, agrega la cuenta a la lista, limpia el formulario y avisa', async () => {
      await crear();
      llenarFormulario();

      enviarFormulario();

      const peticion = http.expectOne((p) => p.url === CUENTAS && p.method === 'POST');
      expect(peticion.request.body).toEqual({
        rol: 'OPERADOR_DISPONIBILIDAD',
        nombre: 'Valentina Ríos',
        correo: 'valentina@samariantour.co',
      });
      peticion.flush(NUEVA);
      harness.detectChanges();

      expect(filas()).toHaveLength(5);
      expect(filas()[4].textContent).toContain('Valentina Ríos');
      expect(filas()[4].textContent).toContain('Aún no puso su contraseña');
      expect(campo('nombre').value).toBe('');
      expect(campo('correo').value).toBe('');
      expect(campo('rol').value).toBe('');
      expect(mostrar).toHaveBeenCalledWith(
        'Creaste la cuenta de Valentina Ríos. Le enviamos un correo para que ponga su contraseña.',
      );
    });

    it('explica que la cuenta nace sin contraseña y que el enlace vence a las 72 horas', async () => {
      await crear();

      expect(texto()).toContain('sin contraseña');
      expect(texto()).toContain('vence a las 72 horas');
    });

    it('un doble clic envía una sola petición', async () => {
      await crear();
      llenarFormulario();

      enviarFormulario();
      enviarFormulario();

      const peticiones = http.match((p) => p.url === CUENTAS && p.method === 'POST');
      expect(peticiones).toHaveLength(1);
      expect(raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!.disabled).toBe(true);
      peticiones[0].flush(NUEVA);
    });

    it('si el correo ya está registrado lo dice en ese campo y conserva lo escrito', async () => {
      await crear();
      llenarFormulario();

      enviarFormulario();
      http
        .expectOne((p) => p.method === 'POST')
        .flush({ codigo: 'CORREO_YA_REGISTRADO' }, { status: 409, statusText: 'Conflict' });
      harness.detectChanges();

      expect(raiz().querySelector('#cuenta-correo-error')?.textContent).toContain('Ya hay una cuenta con ese correo. Usa otro.');
      expect(campo('nombre').value).toBe('  Valentina Ríos ');
      expect(document.activeElement).toBe(campo('correo'));
      expect(filas()).toHaveLength(4);
    });

    it('si el correo del enlace no se pudo enviar, no se crea nada y se puede reintentar', async () => {
      await crear();
      llenarFormulario();

      enviarFormulario();
      http
        .expectOne((p) => p.method === 'POST')
        .flush({ codigo: 'CORREO_NO_DISPONIBLE' }, { status: 503, statusText: 'Service Unavailable' });
      harness.detectChanges();

      expect(raiz().querySelector('form .alerta--error')?.textContent).toContain('No pudimos enviar el correo');
      expect(filas()).toHaveLength(4);
      expect(raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!.disabled).toBe(false);
    });
  });

  describe('desactivar', () => {
    it('antes de desactivar pide confirmación y explica qué pasa con sus sesiones', async () => {
      await crear();

      botonDeFila(1)!.click();
      harness.detectChanges();

      const dialogo = raiz().querySelector('dialog')!;
      expect(dialogo.hasAttribute('open')).toBe(true);
      expect(dialogo.textContent).toContain('¿Desactivar la cuenta de Operador Uno?');
      expect(dialogo.textContent).toContain('a lo sumo 15 minutos');
      expect(dialogo.textContent).toContain('se puede reactivar');
      http.expectNone((p) => p.method === 'POST');
    });

    it('si se cancela no desactiva nada', async () => {
      await crear();
      botonDeFila(1)!.click();
      harness.detectChanges();

      raiz().querySelector<HTMLButtonElement>('dialog .boton--secundario')!.click();
      harness.detectChanges();

      http.expectNone((p) => p.method === 'POST');
    });

    it('al confirmar desactiva con POST, actualiza la fila y avisa', async () => {
      await crear();
      botonDeFila(1)!.click();
      harness.detectChanges();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();
      harness.detectChanges();

      const peticion = http.expectOne(`${CUENTAS}/op1/desactivacion`);
      expect(peticion.request.method).toBe('POST');
      peticion.flush({ ...OPERADOR, estado: 'DESACTIVADA' });
      harness.detectChanges();

      expect(filas()[1].querySelector('.etiqueta')?.textContent?.trim()).toBe('Desactivada');
      expect(botonDeFila(1)!.textContent?.trim()).toBe('Reactivar');
      expect(mostrar).toHaveBeenCalledWith('Desactivaste la cuenta de Operador Uno.');
    });

    it('un operador con prestadores asignados no se desactiva: lo explica y manda a reasignarlos', async () => {
      await crear();
      botonDeFila(1)!.click();
      harness.detectChanges();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();

      http
        .expectOne(`${CUENTAS}/op1/desactivacion`)
        .flush({ codigo: 'OPERADOR_CON_PRESTADORES' }, { status: 409, statusText: 'Conflict' });
      harness.detectChanges();

      expect(filas()[1].querySelector('.campo__error')?.textContent).toContain('todavía tiene prestadores asignados');
      expect(filas()[1].querySelector('a')?.getAttribute('href')).toBe('/admin');
      expect(filas()[1].querySelector('.etiqueta')?.textContent?.trim()).toBe('Activa');
    });

    it('mientras se aplica una acción no deja iniciar otra', async () => {
      await crear();
      botonDeFila(1)!.click();
      harness.detectChanges();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();
      harness.detectChanges();

      expect(botonDeFila(2)!.disabled).toBe(true);
      expect(botonDeFila(3)!.disabled).toBe(true);
      http.expectOne(`${CUENTAS}/op1/desactivacion`).flush({ ...OPERADOR, estado: 'DESACTIVADA' });
    });
  });

  describe('reactivar', () => {
    it('reactiva con POST, actualiza la fila y avisa que un operador no recupera sus prestadores', async () => {
      await crear();

      botonDeFila(3)!.click();
      const peticion = http.expectOne(`${CUENTAS}/op2/reactivacion`);
      expect(peticion.request.method).toBe('POST');
      peticion.flush({ ...DESACTIVADO, estado: 'ACTIVA' });
      harness.detectChanges();

      expect(filas()[3].querySelector('.etiqueta')?.textContent?.trim()).toBe('Activa');
      expect(botonDeFila(3)!.textContent?.trim()).toBe('Desactivar');
      expect(mostrar).toHaveBeenCalledWith(
        'Reactivaste la cuenta de Operador Dos. Un operador reactivado no recupera sus prestadores: vuelve a asignárselos.',
      );
    });

    it('reactivar a un gestor no menciona prestadores', async () => {
      await crear([YO, cuenta('ge2', 'GESTOR_DESTINO', 'Gestor Dos', { estado: 'DESACTIVADA' })]);

      botonDeFila(1)!.click();
      http.expectOne(`${CUENTAS}/ge2/reactivacion`).flush(cuenta('ge2', 'GESTOR_DESTINO', 'Gestor Dos'));

      expect(mostrar).toHaveBeenCalledWith('Reactivaste la cuenta de Gestor Dos.');
    });

    it('si la cuenta ya no existe, lo explica y vuelve a leer la lista', async () => {
      await crear();

      botonDeFila(3)!.click();
      http
        .expectOne(`${CUENTAS}/op2/reactivacion`)
        .flush({ codigo: 'CUENTA_NO_ENCONTRADA' }, { status: 404, statusText: 'Not Found' });
      harness.detectChanges();

      expect(filas()[3].querySelector('.campo__error')).not.toBeNull();
      http.expectOne(CUENTAS).flush([YO, OPERADOR, GESTOR]);
    });
  });

  describe('en inglés', () => {
    it('todo cambia al idioma: filtros, columnas, estados, botones y formulario', async () => {
      establecerIdioma('en', { recordar: false });
      await crear();

      expect(texto()).toContain('Internal accounts');
      expect(texto()).toContain('Create an account');
      expect(texto()).toContain('4 accounts');
      expect(filas()[0].textContent).toContain('You');
      expect(filas()[0].textContent).toContain('View only');
      expect(filas()[1].textContent).toContain('Availability operator');
      expect(filas()[3].querySelector('.etiqueta')?.textContent?.trim()).toBe('Deactivated');
      expect(botonDeFila(1)!.textContent?.trim()).toBe('Deactivate');
      expect(Array.from(raiz().querySelectorAll('.filtros a')).map((a) => a.textContent?.trim())).toEqual([
        'All',
        'Operators',
        'Managers',
        'Administrators',
      ]);
    });
  });
});
