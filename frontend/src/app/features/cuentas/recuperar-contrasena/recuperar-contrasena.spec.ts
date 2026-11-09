import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { RecuperarContrasena } from './recuperar-contrasena';

const URL = '/api/nucleo/cuentas/contrasena/restablecimiento';

describe('RecuperarContrasena', () => {
  let fixture: ComponentFixture<RecuperarContrasena>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';
  const correo = () => raiz().querySelector<HTMLInputElement>('#correo')!;
  const boton = () => raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!;

  function escribirYEnviar(valor: string): void {
    correo().value = valor;
    correo().dispatchEvent(new Event('input'));
    boton().click();
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RecuperarContrasena);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('pide el correo de la cuenta', () => {
    expect(raiz().querySelector('h1')?.textContent).toContain('Recuperar el acceso');
    expect(correo().type).toBe('email');
    expect(raiz().querySelector('a[href="/iniciar-sesion"]')).not.toBeNull();
  });

  it('no envía nada sin correo y lo explica', () => {
    boton().click();
    fixture.detectChanges();

    http.expectNone(URL);
    expect(texto()).toContain('Escribe el correo de tu cuenta.');
    expect(document.activeElement).toBe(correo());
  });

  it('no envía un correo mal escrito', () => {
    escribirYEnviar('laura@');

    http.expectNone(URL);
    expect(texto()).toContain('Escribe un correo válido');
  });

  it('envía el correo sin espacios sobrantes y confirma sin decir si la cuenta existe', () => {
    escribirYEnviar('  laura@demo.samariantour.co ');

    const peticion = http.expectOne(URL);
    expect(peticion.request.body).toEqual({ correo: 'laura@demo.samariantour.co' });
    peticion.flush(null, { status: 202, statusText: 'Accepted' });
    fixture.detectChanges();

    expect(raiz().querySelector('[role="status"]')?.textContent).toContain(
      'Si hay una cuenta activa con laura@demo.samariantour.co',
    );
    expect(texto()).toContain('vence a los 30 minutos y sirve una sola vez');
    expect(raiz().querySelector('form')).toBeNull();
  });

  it('mientras espera deshabilita el botón y no envía dos veces', () => {
    escribirYEnviar('laura@demo.samariantour.co');

    boton().click();
    fixture.detectChanges();

    expect(boton().disabled).toBe(true);
    expect(boton().textContent).toContain('Enviando…');
    http.expectOne(URL).flush(null, { status: 202, statusText: 'Accepted' });
  });

  it('"Pedir otro enlace" vuelve al formulario con el correo ya escrito', () => {
    escribirYEnviar('laura@demo.samariantour.co');
    http.expectOne(URL).flush(null, { status: 202, statusText: 'Accepted' });
    fixture.detectChanges();

    const pedirOtro = Array.from(raiz().querySelectorAll<HTMLButtonElement>('button')).find((b) =>
      b.textContent?.includes('Pedir otro enlace'),
    )!;
    pedirOtro.click();
    fixture.detectChanges();

    expect(correo().value).toBe('laura@demo.samariantour.co');
  });

  it('si el servidor de correo no responde, lo dice y deja intentar de nuevo', () => {
    escribirYEnviar('laura@demo.samariantour.co');

    http
      .expectOne(URL)
      .flush({ status: 503, codigo: 'CORREO_NO_DISPONIBLE' }, { status: 503, statusText: 'x' });
    fixture.detectChanges();

    expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos enviar el correo',
    );
    expect(boton().disabled).toBe(false);
    expect(raiz().querySelector('[role="status"]')).toBeNull();
  });

  it('sin conexión avisa y deja intentar de nuevo', () => {
    escribirYEnviar('laura@demo.samariantour.co');

    http.expectOne(URL).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(texto()).toContain('No pudimos conectar con el servidor');
    expect(boton().disabled).toBe(false);
  });
});
