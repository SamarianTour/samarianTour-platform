import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { RestablecerContrasena } from './restablecer-contrasena';

const URL = '/api/nucleo/cuentas/contrasena';

describe('RestablecerContrasena', () => {
  let fixture: ComponentFixture<RestablecerContrasena>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#${id}`)!;
  const boton = () => raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!;

  function crear(token?: string): void {
    fixture = TestBed.createComponent(RestablecerContrasena);
    if (token) {
      fixture.componentRef.setInput('token', token);
    }
    fixture.detectChanges();
  }

  function escribir(id: string, valor: string): void {
    campo(id).value = valor;
    campo(id).dispatchEvent(new Event('input'));
  }

  function enviar(contrasena = 'Playa2027', repetir = contrasena): void {
    escribir('contrasena', contrasena);
    escribir('repetir', repetir);
    boton().click();
    fixture.detectChanges();
  }

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
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  describe('sin token en la dirección', () => {
    it('avisa que el enlace no es válido y ofrece pedir otro', () => {
      crear();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'Este enlace no es válido',
      );
      expect(raiz().querySelector('a[href="/recuperar-contrasena"]')).not.toBeNull();
      expect(raiz().querySelector('form')).toBeNull();
    });
  });

  describe('con el token del correo', () => {
    it('pide la contraseña nueva dos veces', () => {
      crear('token-del-correo');

      expect(raiz().querySelector('h1')?.textContent).toContain('Elige una contraseña nueva');
      expect(campo('contrasena').type).toBe('password');
      expect(campo('repetir').type).toBe('password');
    });

    it('no envía nada si faltan datos', () => {
      crear('token-del-correo');

      boton().click();
      fixture.detectChanges();

      http.expectNone(URL);
      expect(texto()).toContain('Escribe una contraseña.');
      expect(texto()).toContain('Repite la contraseña.');
      expect(document.activeElement).toBe(campo('contrasena'));
    });

    it.each([
      ['corta', 'Playa1', 'entre 8 y 72 caracteres'],
      ['sin número', 'SoloLetrasAqui', 'al menos una letra y un número'],
      ['sin letras', '123456789', 'al menos una letra y un número'],
    ])('rechaza una contraseña %s', (_motivo, clave, mensaje) => {
      crear('token-del-correo');

      enviar(clave);

      http.expectNone(URL);
      expect(texto()).toContain(mensaje);
    });

    it('rechaza una repetición distinta', () => {
      crear('token-del-correo');

      enviar('Playa2027', 'Playa2028');

      http.expectNone(URL);
      expect(texto()).toContain('Las contraseñas no coinciden.');
      expect(document.activeElement).toBe(campo('repetir'));
    });

    it('envía el token del enlace y la contraseña nueva', () => {
      crear('token-del-correo');

      enviar('Playa2027');

      const peticion = http.expectOne(URL);
      expect(peticion.request.body).toEqual({ token: 'token-del-correo', contrasena: 'Playa2027' });
      peticion.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('al terminar avisa y lleva a iniciar sesión', () => {
      crear('token-del-correo');

      enviar();
      http.expectOne(URL).flush(null, { status: 204, statusText: 'No Content' });

      expect(TestBed.inject(AvisosService).avisos()[0].texto).toContain('Cambiaste tu contraseña');
      expect(navegar).toHaveBeenCalledWith(['/iniciar-sesion'], { replaceUrl: true });
    });

    it('cierra la sesión de este navegador, porque el núcleo cerró todas las de la cuenta', () => {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion())));
      crear('token-del-correo');
      const sesion = TestBed.inject(SesionService);
      expect(sesion.autenticada()).toBe(true);

      enviar();
      http.expectOne(URL).flush(null, { status: 204, statusText: 'No Content' });

      expect(sesion.autenticada()).toBe(false);
      expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
    });

    it('mientras espera deshabilita el botón y no envía dos veces', () => {
      crear('token-del-correo');

      enviar();
      boton().click();
      fixture.detectChanges();

      expect(boton().disabled).toBe(true);
      expect(boton().textContent).toContain('Guardando…');
      http.expectOne(URL).flush(null, { status: 204, statusText: 'No Content' });
    });
  });

  describe('cuando el núcleo lo rechaza', () => {
    it('con un enlace vencido o ya usado lo explica y ofrece pedir otro, sin dejar seguir', () => {
      crear('token-viejo');

      enviar();
      http
        .expectOne(URL)
        .flush({ status: 409, codigo: 'CODIGO_NO_VIGENTE' }, { status: 409, statusText: 'x' });
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'El enlace venció o ya se usó',
      );
      expect(raiz().querySelector('a[href="/recuperar-contrasena"]')).not.toBeNull();
      expect(raiz().querySelector('form')).toBeNull();
      expect(navegar).not.toHaveBeenCalled();
    });

    it('con una contraseña que la API no acepta, lo dice en el campo', () => {
      crear('token-del-correo');

      enviar();
      http
        .expectOne(URL)
        .flush(
          {
            status: 400,
            codigo: 'DATO_INVALIDO',
            detail: 'contrasena: debe tener al menos una letra y un número',
          },
          { status: 400, statusText: 'x' },
        );
      fixture.detectChanges();

      expect(raiz().querySelector('#contrasena-error')?.textContent).toContain(
        'Debe tener al menos una letra',
      );
      expect(boton().disabled).toBe(false);
    });

    it('sin conexión avisa y deja intentar de nuevo', () => {
      crear('token-del-correo');

      enviar();
      http.expectOne(URL).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos conectar con el servidor',
      );
      expect(boton().disabled).toBe(false);
    });
  });
});
