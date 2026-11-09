import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Rol } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { EliminarCuenta } from './eliminar-cuenta';

const BASE = '/api/nucleo';

describe('EliminarCuenta', () => {
  let fixture: ComponentFixture<EliminarCuenta>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';
  const dialogo = () => raiz().querySelector<HTMLDialogElement>('dialog')!;
  const contrasena = () => raiz().querySelector<HTMLInputElement>('#contrasena-eliminar')!;
  const confirmar = () => dialogo().querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const abrirDialogo = () => {
    raiz().querySelector<HTMLButtonElement>('section > div > button')!.click();
    fixture.detectChanges();
  };

  function crear(rol: Rol): void {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol }))));
    fixture = TestBed.createComponent(EliminarCuenta);
    fixture.detectChanges();
  }

  function escribirYConfirmar(clave: string): void {
    contrasena().value = clave;
    contrasena().dispatchEvent(new Event('input'));
    confirmar().click();
    fixture.detectChanges();
  }

  const rechazar = (url: string, estado: number, cuerpo: object) =>
    http.expectOne(url).flush(cuerpo, { status: estado, statusText: 'Error' });

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

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  describe('quién puede eliminar su cuenta', () => {
    it.each(['TURISTA', 'PRESTADOR'] as const)('un %s ve el botón', (rol) => {
      crear(rol);

      expect(raiz().querySelector('.boton--peligro')?.textContent).toContain('Eliminar mi cuenta');
    });

    it.each(['OPERADOR_DISPONIBILIDAD', 'GESTOR_DESTINO', 'ADMINISTRADOR'] as const)(
      'un %s no: su cuenta interna la gestiona un administrador',
      (rol) => {
        crear(rol);

        expect(raiz().querySelector('section > div > button')).toBeNull();
        expect(texto()).toContain('la gestiona un administrador');
      },
    );

    it('explica al turista qué se borra y qué se conserva', () => {
      crear('TURISTA');

      expect(texto()).toContain('quitaremos tus opiniones del catálogo');
      expect(texto()).toContain('Con el mismo correo podrás crear una cuenta nueva');
      expect(texto()).not.toContain('Tus ofertas se retiran');
    });

    it('explica al prestador qué se borra y qué se conserva', () => {
      crear('PRESTADOR');

      expect(texto()).toContain('Tus ofertas se retiran para siempre');
      expect(texto()).toContain('El RNT, la razón social');
      expect(texto()).not.toContain('quitaremos tus opiniones');
    });

    it('a una cuenta interna no se le dibuja el diálogo de eliminar', () => {
      crear('ADMINISTRADOR');

      expect(raiz().querySelector('dialog')).toBeNull();
    });

    it('avisa que no se puede deshacer', () => {
      crear('TURISTA');

      expect(texto()).toContain('inmediata y no se puede deshacer');
    });
  });

  describe('confirmar con la contraseña', () => {
    it('abre un diálogo que pide la contraseña, sin eliminar nada todavía', () => {
      crear('TURISTA');

      abrirDialogo();

      expect(dialogo().hasAttribute('open')).toBe(true);
      expect(dialogo().textContent).toContain('¿Eliminar tu cuenta?');
      expect(contrasena().type).toBe('password');
    });

    it('cancelar lo cierra sin enviar nada', () => {
      crear('TURISTA');
      abrirDialogo();

      dialogo().querySelector<HTMLButtonElement>('button[type="button"]')!.click();

      expect(dialogo().hasAttribute('open')).toBe(false);
      http.expectNone(`${BASE}/turista/supresion`);
    });

    it('sin contraseña no envía nada y lo explica', () => {
      crear('TURISTA');
      abrirDialogo();

      confirmar().click();
      fixture.detectChanges();

      http.expectNone(`${BASE}/turista/supresion`);
      expect(dialogo().textContent).toContain('Escribe tu contraseña para confirmar.');
      expect(document.activeElement).toBe(contrasena());
    });

    it('al reabrirlo no queda la contraseña ni el error anterior', () => {
      crear('TURISTA');
      abrirDialogo();
      escribirYConfirmar('mala');
      rechazar(`${BASE}/turista/supresion`, 401, { status: 401, codigo: 'CONTRASENA_INCORRECTA' });
      fixture.detectChanges();
      dialogo().querySelector<HTMLButtonElement>('button[type="button"]')!.click();

      abrirDialogo();

      expect(contrasena().value).toBe('');
      expect(dialogo().querySelector('[role="alert"]')).toBeNull();
    });
  });

  describe('al eliminar', () => {
    it('un turista usa su ruta, cierra la sesión local, avisa y vuelve al catálogo', () => {
      crear('TURISTA');
      abrirDialogo();

      escribirYConfirmar('Demo1234');

      const peticion = http.expectOne(`${BASE}/turista/supresion`);
      expect(peticion.request.body).toEqual({ contrasena: 'Demo1234' });
      peticion.flush(null, { status: 204, statusText: 'No Content' });

      expect(TestBed.inject(SesionService).autenticada()).toBe(false);
      expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
      expect(TestBed.inject(AvisosService).avisos()[0].texto).toContain(
        'Eliminamos tus datos personales',
      );
      expect(navegar).toHaveBeenCalledWith('/', { replaceUrl: true });
      expect(dialogo().hasAttribute('open')).toBe(false);
    });

    it('un prestador usa la ruta de supresión del prestador', () => {
      crear('PRESTADOR');
      abrirDialogo();

      escribirYConfirmar('Demo1234');

      const peticion = http.expectOne(`${BASE}/prestador/supresion`);
      expect(peticion.request.body).toEqual({ contrasena: 'Demo1234' });
      peticion.flush(null, { status: 204, statusText: 'No Content' });
      expect(navegar).toHaveBeenCalledWith('/', { replaceUrl: true });
    });

    it('la contraseña se envía tal cual, con sus espacios', () => {
      crear('TURISTA');
      abrirDialogo();

      escribirYConfirmar(' Demo 1234 ');

      expect(http.expectOne(`${BASE}/turista/supresion`).request.body).toEqual({
        contrasena: ' Demo 1234 ',
      });
    });

    it('mientras espera deshabilita el botón y no envía dos veces', () => {
      crear('TURISTA');
      abrirDialogo();
      escribirYConfirmar('Demo1234');

      confirmar().click();
      fixture.detectChanges();

      expect(confirmar().disabled).toBe(true);
      expect(confirmar().textContent).toContain('Eliminando…');
      http
        .expectOne(`${BASE}/turista/supresion`)
        .flush(null, { status: 204, statusText: 'No Content' });
    });
  });

  describe('cuando el núcleo lo rechaza', () => {
    it('con una contraseña equivocada lo dice, vacía el campo y no cierra la sesión', () => {
      crear('TURISTA');
      abrirDialogo();
      escribirYConfirmar('mala');

      rechazar(`${BASE}/turista/supresion`, 401, { status: 401, codigo: 'CONTRASENA_INCORRECTA' });
      fixture.detectChanges();

      expect(dialogo().querySelector('[role="alert"]')?.textContent).toContain(
        'La contraseña no es correcta.',
      );
      expect(contrasena().value).toBe('');
      expect(document.activeElement).toBe(contrasena());
      expect(dialogo().hasAttribute('open')).toBe(true);
      expect(TestBed.inject(SesionService).autenticada()).toBe(true);
      expect(navegar).not.toHaveBeenCalled();
    });

    it('con la cuenta bloqueada por intentos dice cuánto falta (RF-05)', () => {
      crear('TURISTA');
      abrirDialogo();
      escribirYConfirmar('mala');

      rechazar(`${BASE}/turista/supresion`, 423, {
        status: 423,
        codigo: 'CUENTA_BLOQUEADA',
        bloqueadaHasta: new Date(Date.now() + 15 * 60_000).toISOString(),
      });
      fixture.detectChanges();

      expect(dialogo().querySelector('[role="alert"]')?.textContent).toContain(
        'Tu cuenta está bloqueada',
      );
      expect(dialogo().querySelector('[role="alert"]')?.textContent).toContain('minutos');
    });

    it('un prestador con compromisos pendientes ve la lista de lo que debe cumplir', () => {
      crear('PRESTADOR');
      abrirDialogo();
      escribirYConfirmar('Demo1234');

      rechazar(`${BASE}/prestador/supresion`, 409, {
        status: 409,
        codigo: 'PRESTADOR_CON_COMPROMISOS',
        compromisos: ['2 dispersiones por recibir', '1 servicio pagado sin terminar'],
      });
      fixture.detectChanges();

      const alerta = dialogo().querySelector('[role="alert"]')!;
      expect(alerta.textContent).toContain('debes cumplir los compromisos que tienes pendientes');
      expect(Array.from(alerta.querySelectorAll('li')).map((li) => li.textContent)).toEqual([
        '2 dispersiones por recibir',
        '1 servicio pagado sin terminar',
      ]);
      expect(TestBed.inject(SesionService).autenticada()).toBe(true);
      expect(navegar).not.toHaveBeenCalled();
    });

    it('la lista de compromisos desaparece al intentarlo de nuevo', () => {
      crear('PRESTADOR');
      abrirDialogo();
      escribirYConfirmar('Demo1234');
      rechazar(`${BASE}/prestador/supresion`, 409, {
        status: 409,
        codigo: 'PRESTADOR_CON_COMPROMISOS',
        compromisos: ['2 dispersiones por recibir'],
      });
      fixture.detectChanges();

      escribirYConfirmar('Demo1234');

      expect(dialogo().querySelectorAll('li')).toHaveLength(0);
      http
        .expectOne(`${BASE}/prestador/supresion`)
        .flush(null, { status: 204, statusText: 'No Content' });
    });

    it('sin conexión avisa y deja intentar de nuevo', () => {
      crear('TURISTA');
      abrirDialogo();
      escribirYConfirmar('Demo1234');

      http.expectOne(`${BASE}/turista/supresion`).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(dialogo().querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos conectar con el servidor',
      );
      expect(confirmar().disabled).toBe(false);
      expect(TestBed.inject(SesionService).autenticada()).toBe(true);
    });
  });
});
