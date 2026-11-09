import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { CLAVE_SESION } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { IniciarSesion } from './iniciar-sesion';

const SESION = '/api/nucleo/cuentas/sesion';
const RECHAZO = { status: 401, statusText: 'Unauthorized' };

describe('IniciarSesion', () => {
  let fixture: ComponentFixture<IniciarSesion>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#${id}`)!;
  const texto = () => raiz().textContent ?? '';
  const boton = () => raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!;

  function escribir(id: string, valor: string): void {
    const entrada = campo(id);
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
  }

  function enviar(correo = 'laura@demo.samariantour.co', contrasena = 'Demo1234'): void {
    escribir('correo', correo);
    escribir('contrasena', contrasena);
    boton().click();
    fixture.detectChanges();
  }

  function crear(): void {
    fixture = TestBed.createComponent(IniciarSesion);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.useFakeTimers({ now: new Date('2026-11-08T12:00:00Z') });
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

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('muestra el título, los campos y el enlace para recuperar la contraseña', () => {
    crear();

    expect(raiz().querySelector('h1')?.textContent).toContain('Iniciar sesión');
    expect(campo('correo').type).toBe('email');
    expect(campo('contrasena').type).toBe('password');
    expect(texto()).toContain('Recordarme');
    expect(raiz().querySelector('a[href="/recuperar-contrasena"]')).not.toBeNull();
    expect(raiz().querySelector('a[href="/registro"]')?.textContent).toContain('Crea una');
  });

  it('no envía nada si faltan datos y marca los campos con su motivo', () => {
    crear();

    boton().click();
    fixture.detectChanges();

    http.expectNone(SESION);
    expect(texto()).toContain('Escribe tu correo.');
    expect(texto()).toContain('Escribe tu contraseña.');
    expect(campo('correo').getAttribute('aria-invalid')).toBe('true');
  });

  it('pide un correo con forma de correo', () => {
    crear();

    enviar('laura', 'Demo1234');

    http.expectNone(SESION);
    expect(texto()).toContain('Escribe un correo válido');
  });

  it('un correo de solo espacios cuenta como vacío', () => {
    crear();

    enviar('   ', 'Demo1234');

    http.expectNone(SESION);
    expect(texto()).toContain('Escribe tu correo.');
  });

  it('envía el correo sin espacios sobrantes y la contraseña tal cual', () => {
    crear();

    enviar('  laura@demo.samariantour.co ', ' Demo1234');

    const peticion = http.expectOne(SESION);
    expect(peticion.request.body).toEqual({
      correo: 'laura@demo.samariantour.co',
      contrasena: ' Demo1234',
    });
    peticion.flush(respuestaDeSesion());
  });

  it('mientras espera la respuesta deshabilita el botón y no envía dos veces', () => {
    crear();

    enviar();
    boton().click();
    fixture.detectChanges();

    expect(boton().disabled).toBe(true);
    expect(boton().textContent).toContain('Entrando…');
    http.expectOne(SESION).flush(respuestaDeSesion());
  });

  describe('al entrar', () => {
    it.each([
      ['TURISTA', '/'],
      ['PRESTADOR', '/prestador/ofertas'],
      ['OPERADOR_DISPONIBILIDAD', '/operador'],
      ['GESTOR_DESTINO', '/gestor'],
      ['ADMINISTRADOR', '/admin'],
    ] as const)('lleva al %s a su inicio', (rol, inicio) => {
      crear();

      enviar();
      http.expectOne(SESION).flush(respuestaDeSesion({ rol }));

      expect(navegar).toHaveBeenCalledWith(inicio, { replaceUrl: true });
    });

    it('vuelve a la página que el usuario quería ver', () => {
      crear();
      fixture.componentRef.setInput('volver', '/prestador/ofertas/nueva');

      enviar();
      http.expectOne(SESION).flush(respuestaDeSesion({ rol: 'PRESTADOR' }));

      expect(navegar).toHaveBeenCalledWith('/prestador/ofertas/nueva', { replaceUrl: true });
    });

    it('no vuelve a una dirección de otro sitio', () => {
      crear();
      fixture.componentRef.setInput('volver', 'https://malo.com/robar');

      enviar();
      http.expectOne(SESION).flush(respuestaDeSesion({ rol: 'TURISTA' }));

      expect(navegar).toHaveBeenCalledWith('/', { replaceUrl: true });
    });

    it('no vuelve a una sección que es de otro rol', () => {
      crear();
      fixture.componentRef.setInput('volver', '/prestador/ofertas');

      enviar();
      http.expectOne(SESION).flush(respuestaDeSesion({ rol: 'TURISTA' }));

      expect(navegar).toHaveBeenCalledWith('/', { replaceUrl: true });
    });

    it('sin "Recordarme" la sesión queda solo en sessionStorage', () => {
      crear();

      enviar();
      http.expectOne(SESION).flush(respuestaDeSesion());

      expect(sessionStorage.getItem(CLAVE_SESION)).not.toBeNull();
      expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('con "Recordarme" la sesión queda en localStorage', () => {
      crear();
      const recordar = raiz().querySelector<HTMLInputElement>('input[type="checkbox"]')!;
      recordar.click();

      enviar();
      http.expectOne(SESION).flush(respuestaDeSesion());

      expect(localStorage.getItem(CLAVE_SESION)).not.toBeNull();
      expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('"Recordarme" empieza desmarcado', () => {
      crear();

      expect(raiz().querySelector<HTMLInputElement>('input[type="checkbox"]')!.checked).toBe(false);
    });
  });

  describe('cuando el núcleo lo rechaza', () => {
    const rechazar = (estado: number, cuerpo: object) =>
      http.expectOne(SESION).flush(cuerpo, { status: estado, statusText: 'Error' });

    it('con credenciales inválidas avisa sin decir cuál dato falló, conserva el correo y vacía la contraseña', () => {
      crear();

      enviar('laura@demo.samariantour.co', 'mala');
      rechazar(401, { status: 401, codigo: 'CREDENCIALES_INVALIDAS' });
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'El correo o la contraseña no son correctos.',
      );
      expect(campo('correo').value).toBe('laura@demo.samariantour.co');
      expect(campo('contrasena').value).toBe('');
      expect(boton().disabled).toBe(false);
      expect(navegar).not.toHaveBeenCalled();
    });

    it('con la cuenta bloqueada dice cuánto falta para volver a intentar (RF-05)', () => {
      crear();

      enviar();
      rechazar(423, {
        status: 423,
        codigo: 'CUENTA_BLOQUEADA',
        bloqueadaHasta: '2026-11-08T12:15:00Z',
      });
      fixture.detectChanges();

      expect(texto()).toContain('Tu cuenta está bloqueada por intentos fallidos');
      expect(texto()).toContain('en 15 minutos');
    });

    it('con la cuenta bloqueada pero sin hora, da el aviso general', () => {
      crear();

      enviar();
      rechazar(423, { status: 423, codigo: 'CUENTA_BLOQUEADA' });
      fixture.detectChanges();

      expect(texto()).toContain('Tu cuenta está bloqueada por intentos fallidos.');
    });

    it('con la cuenta desactivada lo dice', () => {
      crear();

      enviar();
      rechazar(403, { status: 403, codigo: 'CUENTA_DESACTIVADA' });
      fixture.detectChanges();

      expect(texto()).toContain('Un administrador desactivó tu cuenta');
    });

    it('sin conexión avisa y deja intentar de nuevo', () => {
      crear();

      enviar();
      http.expectOne(SESION).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(texto()).toContain('No pudimos conectar con el servidor');
      expect(boton().disabled).toBe(false);
    });

    it('no abre la sesión', () => {
      crear();

      enviar();
      rechazar(401, { status: 401, codigo: 'CREDENCIALES_INVALIDAS' });

      expect(TestBed.inject(SesionService).autenticada()).toBe(false);
    });
  });

  describe('quien ya tiene sesión', () => {
    it('no ve el formulario: va a su inicio', () => {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(sesionGuardada('PRESTADOR')));

      crear();

      expect(navegar).toHaveBeenCalledWith('/prestador/ofertas', { replaceUrl: true });
    });

    it('si venía de otra página, vuelve a ella', () => {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(sesionGuardada('TURISTA')));
      fixture = TestBed.createComponent(IniciarSesion);
      fixture.componentRef.setInput('volver', '/mis-datos');

      fixture.detectChanges();

      expect(navegar).toHaveBeenCalledWith('/mis-datos', { replaceUrl: true });
    });
  });
});

function sesionGuardada(rol: 'TURISTA' | 'PRESTADOR') {
  const { tokenAcceso, accesoExpiraEn, tokenRenovacion, renovacionExpiraEn, cuenta } =
    respuestaDeSesion({ rol });
  return { tokenAcceso, accesoExpiraEn, tokenRenovacion, renovacionExpiraEn, cuenta };
}
