import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { VerificarWhatsapp } from './verificar-whatsapp';

const BASE = '/api/nucleo/turista/whatsapp';

describe('VerificarWhatsapp', () => {
  let fixture: ComponentFixture<VerificarWhatsapp>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => raiz().textContent ?? '';
  const codigo = () => raiz().querySelector<HTMLInputElement>('#codigo')!;
  const verificar = () => raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!;
  // Se busca por su lugar y no por su texto, que cambia a "Enviando…" mientras espera.
  const reenviar = () =>
    raiz().querySelector<HTMLButtonElement>('.verificar__acciones button[type="button"]')!;

  function escribirYEnviar(valor: string): void {
    codigo().value = valor;
    codigo().dispatchEvent(new Event('input'));
    verificar().click();
    fixture.detectChanges();
  }

  const rechazar = (estado: number, cuerpo: object) =>
    http.expectOne(`${BASE}/verificacion`).flush(cuerpo, { status: estado, statusText: 'Error' });

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    const sesion = respuestaDeSesion({ rol: 'TURISTA' });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(
        aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, whatsappVerificado: false } }),
      ),
    );
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(VerificarWhatsapp);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('dice a qué número se envió el código', () => {
    expect(texto()).toContain('Verifica tu WhatsApp');
    expect(texto()).toContain('+573105550007');
    expect(texto()).toContain('no podrás enviar solicitudes');
  });

  it('pide un código de 6 dígitos con el teclado numérico y el autocompletado de códigos', () => {
    expect(codigo().getAttribute('inputmode')).toBe('numeric');
    expect(codigo().getAttribute('autocomplete')).toBe('one-time-code');
    expect(codigo().maxLength).toBe(6);
  });

  it('no envía si el código está vacío o no tiene 6 dígitos', () => {
    verificar().click();
    fixture.detectChanges();
    expect(texto()).toContain('Escribe el código que te llegó.');

    escribirYEnviar('12ab56');
    expect(texto()).toContain('El código tiene 6 dígitos, solo números.');

    escribirYEnviar('12345');
    http.expectNone(`${BASE}/verificacion`);
  });

  it('envía el código y, si es correcto, deja el número verificado en la sesión', () => {
    escribirYEnviar('048213');

    const peticion = http.expectOne(`${BASE}/verificacion`);
    expect(peticion.request.body).toEqual({ codigo: '048213' });
    const sesion = TestBed.inject(SesionService);
    peticion.flush({ ...sesion.cuenta()!, whatsappVerificado: true });

    expect(sesion.cuenta()?.whatsappVerificado).toBe(true);
    expect(TestBed.inject(AvisosService).avisos()[0].texto).toBe('Verificaste tu WhatsApp');
  });

  it('el cero inicial del código no se pierde', () => {
    escribirYEnviar('000123');

    const peticion = http.expectOne(`${BASE}/verificacion`);
    expect(peticion.request.body).toEqual({ codigo: '000123' });
    peticion.flush(
      { status: 400, codigo: 'CODIGO_INCORRECTO', intentosRestantes: 2 },
      { status: 400, statusText: 'Error' },
    );
  });

  it('mientras espera deshabilita el botón y no envía dos veces', () => {
    escribirYEnviar('048213');

    verificar().click();
    fixture.detectChanges();

    expect(verificar().disabled).toBe(true);
    expect(verificar().textContent).toContain('Verificando…');
    rechazar(400, { status: 400, codigo: 'CODIGO_INCORRECTO', intentosRestantes: 1 });
  });

  describe('cuando el código no sirve', () => {
    it.each([
      [2, 'Te quedan 2 intentos.'],
      [1, 'Te queda 1 intento.'],
      [0, 'ya no admite más intentos. Pide uno nuevo.'],
    ])('con %i intentos restantes dice: %s', (restantes, esperado) => {
      escribirYEnviar('111111');

      rechazar(400, { status: 400, codigo: 'CODIGO_INCORRECTO', intentosRestantes: restantes });
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'El código no es correcto',
      );
      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(esperado);
    });

    it('vacía el campo y deja el foco para escribir otro', () => {
      escribirYEnviar('111111');

      rechazar(400, { status: 400, codigo: 'CODIGO_INCORRECTO', intentosRestantes: 2 });
      fixture.detectChanges();

      expect(codigo().value).toBe('');
      expect(document.activeElement).toBe(codigo());
      expect(verificar().disabled).toBe(false);
    });

    it('si el código venció, lo dice', () => {
      escribirYEnviar('111111');

      rechazar(409, { status: 409, codigo: 'CODIGO_NO_VIGENTE' });
      fixture.detectChanges();

      expect(texto()).toContain('El código o el enlace venció o ya se usó. Pide uno nuevo.');
    });

    it('no cambia el estado de la verificación', () => {
      escribirYEnviar('111111');

      rechazar(400, { status: 400, codigo: 'CODIGO_INCORRECTO', intentosRestantes: 2 });

      expect(TestBed.inject(SesionService).cuenta()?.whatsappVerificado).toBe(false);
    });
  });

  describe('enviar otro código', () => {
    it('lo pide al servidor y avisa que el anterior ya no sirve', () => {
      reenviar().click();
      fixture.detectChanges();

      const peticion = http.expectOne(`${BASE}/codigo`);
      expect(peticion.request.method).toBe('POST');
      peticion.flush(null, { status: 202, statusText: 'Accepted' });
      fixture.detectChanges();

      expect(raiz().querySelector('[role="status"]')?.textContent).toContain(
        'Te enviamos un código nuevo. El anterior ya no sirve.',
      );
    });

    it('no envía dos pedidos a la vez', () => {
      reenviar().click();
      reenviar().click();
      fixture.detectChanges();

      expect(reenviar().disabled).toBe(true);
      http.expectOne(`${BASE}/codigo`).flush(null, { status: 202, statusText: 'Accepted' });
    });

    it('si falla, lo dice y no afirma que se envió', () => {
      reenviar().click();

      http.expectOne(`${BASE}/codigo`).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos conectar con el servidor',
      );
      expect(raiz().querySelector('[role="status"]')).toBeNull();
    });
  });
});
