import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { erroresInterceptor } from '../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../core/sesion/testing';
import { IniciarSesion } from './iniciar-sesion/iniciar-sesion';
import { RecuperarContrasena } from './recuperar-contrasena/recuperar-contrasena';
import { Registro } from './registro/registro';
import { RestablecerContrasena } from './restablecer-contrasena/restablecer-contrasena';
import { SinPermiso } from './sin-permiso/sin-permiso';
import { VerificarWhatsapp } from './mis-datos/verificar-whatsapp';

/**
 * Las pantallas de cuentas en los dos idiomas (RF-18, RF-19), y que un mensaje que ya está en pantalla cambia de idioma
 * junto con la interfaz sin repetir la acción.
 */
describe('cuentas en español e inglés', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<unknown>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';
  const cambiarA = (idioma: 'es' | 'en') => {
    establecerIdioma(idioma, { recordar: false });
    fixture.detectChanges();
  };

  function crear<T>(componente: new () => T, entradas: Record<string, unknown> = {}): void {
    const f = TestBed.createComponent(componente);
    for (const [nombre, valor] of Object.entries(entradas)) {
      f.componentRef.setInput(nombre, valor);
    }
    f.detectChanges();
    fixture = f;
  }

  function conSesion(rol: 'TURISTA' | 'PRESTADOR', whatsappVerificado = true): void {
    const sesion = respuestaDeSesion({ rol });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(
        aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, whatsappVerificado } }),
      ),
    );
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
  });

  afterEach(() => http.match(() => true).forEach((p) => p.error(new ProgressEvent('error'))));

  describe('iniciar sesión', () => {
    it('sus textos están en inglés', () => {
      establecerIdioma('en', { recordar: false });
      crear(IniciarSesion);

      expect(raiz().querySelector('h1')?.textContent).toContain('Log in');
      expect(texto()).toContain('Remember me');
      expect(texto()).toContain('Forgot your password?');
      expect(texto()).toContain("Don't have an account?");
      expect(raiz().querySelector('label[for="correo"]')?.textContent).toContain('Email');
      expect(raiz().querySelector('label[for="contrasena"]')?.textContent).toContain('Password');
      expect(texto()).not.toContain('Contraseña');
    });

    it('los avisos de campo cambian de idioma sin volver a enviar', () => {
      crear(IniciarSesion);
      raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
      fixture.detectChanges();
      expect(texto()).toContain('Escribe tu correo.');

      cambiarA('en');

      expect(texto()).toContain('Enter your email.');
      expect(texto()).toContain('Enter your password.');
      expect(texto()).not.toContain('Escribe tu correo.');
    });

    it('un error de la API que ya se mostró cambia de idioma al instante', () => {
      crear(IniciarSesion);
      const entrada = (id: string, valor: string) => {
        const campo = raiz().querySelector<HTMLInputElement>(`#${id}`)!;
        campo.value = valor;
        campo.dispatchEvent(new Event('input'));
      };
      entrada('correo', 'laura@demo.samariantour.co');
      entrada('contrasena', 'mala');
      raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
      http
        .expectOne('/api/nucleo/cuentas/sesion')
        .flush(
          { status: 401, codigo: 'CREDENCIALES_INVALIDAS' },
          { status: 401, statusText: 'Unauthorized' },
        );
      fixture.detectChanges();
      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'El correo o la contraseña no son correctos.',
      );

      cambiarA('en');

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        "The email or password isn't correct.",
      );
    });
  });

  describe('recuperar y restablecer la contraseña', () => {
    it('recuperar está en inglés', () => {
      establecerIdioma('en', { recordar: false });
      crear(RecuperarContrasena);

      expect(raiz().querySelector('h1')?.textContent).toContain('Recover access');
      expect(texto()).toContain('Send the link');
      expect(texto()).toContain('Remembered it?');
    });

    it('restablecer está en inglés, con la ayuda de la contraseña y sin enlace válido', () => {
      establecerIdioma('en', { recordar: false });
      crear(RestablecerContrasena, { token: 'abc' });

      expect(raiz().querySelector('h1')?.textContent).toContain('Choose a new password');
      expect(texto()).toContain('At least 8 characters, with at least one letter and one number.');
      expect(texto()).toContain('Change password');

      crear(RestablecerContrasena);
      expect(texto()).toContain("This link isn't valid");
    });
  });

  describe('registro', () => {
    it('está en inglés y usa el glosario (tourist, provider)', () => {
      establecerIdioma('en', { recordar: false });
      crear(Registro);

      expect(raiz().querySelector('h1')?.textContent).toContain('Create an account');
      expect(texto()).toContain('Tourist');
      expect(texto()).toContain('Provider');
      expect(texto()).toContain('Full name');
      expect(texto()).toContain('Fields marked with an');
      expect(texto()).toContain('Data Processing Policy');
      expect(texto()).not.toContain('Turista');
    });

    it('al elegir prestador cambia el nombre del campo en cada idioma', () => {
      crear(Registro, { tipo: 'prestador' });
      expect(raiz().querySelector('label[for="nombre"]')?.textContent).toContain(
        'Nombre comercial del negocio',
      );

      cambiarA('en');

      expect(raiz().querySelector('label[for="nombre"]')?.textContent).toContain('Business name');
    });

    it('el error al cargar la política cambia de idioma al instante', () => {
      crear(Registro);
      http.expectOne((p) => p.url.includes('politica')).error(new ProgressEvent('error'));
      fixture.detectChanges();
      expect(texto()).toContain('No pudimos conectar con el servidor');

      cambiarA('en');

      expect(texto()).toContain("We couldn't connect to the server");
      expect(texto()).toContain('Try again');
    });
  });

  describe('verificar el WhatsApp', () => {
    it('está en inglés', () => {
      conSesion('TURISTA', false);
      establecerIdioma('en', { recordar: false });
      crear(VerificarWhatsapp);

      expect(texto()).toContain('Verify your WhatsApp');
      expect(texto()).toContain('We sent a 6-digit code to');
      expect(texto()).toContain("You won't be able to send requests");
    });

    it('los intentos restantes se dicen en singular y plural en cada idioma', () => {
      conSesion('TURISTA', false);
      crear(VerificarWhatsapp);
      const enviar = (intentosRestantes: number) => {
        const campo = raiz().querySelector<HTMLInputElement>('#codigo')!;
        campo.value = '123456';
        campo.dispatchEvent(new Event('input'));
        raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
        http
          .expectOne('/api/nucleo/turista/whatsapp/verificacion')
          .flush(
            { status: 400, codigo: 'CODIGO_INCORRECTO', intentosRestantes },
            { status: 400, statusText: 'Bad Request' },
          );
        fixture.detectChanges();
      };

      enviar(2);
      expect(texto()).toContain('Te quedan 2 intentos.');
      cambiarA('en');
      expect(texto()).toContain('You have 2 attempts left.');

      enviar(1);
      expect(texto()).toContain('You have 1 attempt left.');
      cambiarA('es');
      expect(texto()).toContain('Te queda 1 intento.');
    });
  });

  describe('no tienes permiso', () => {
    it('está en inglés y nombra el rol con el glosario', () => {
      conSesion('PRESTADOR');
      establecerIdioma('en', { recordar: false });
      crear(SinPermiso);

      expect(raiz().querySelector('h1')?.textContent).toContain(
        "You don't have permission to view this page",
      );
      expect(texto()).toContain('You logged in as Provider, and this section');
      expect(texto()).toContain('Log in with another account');
    });

    it('el rol cambia de idioma con la pantalla abierta', () => {
      conSesion('TURISTA');
      crear(SinPermiso);
      expect(texto()).toContain('Entraste como Turista');

      cambiarA('en');

      expect(texto()).toContain('You logged in as Tourist');
    });
  });
});
