import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { SesionService } from '../../../core/sesion/sesion.service';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { VerificarWhatsapp } from './verificar-whatsapp';

/**
 * El mismo componente que verifica el WhatsApp del turista lo usa el prestador (P03.4) con otras rutas del núcleo. Estas pruebas
 * cubren el modo con entradas; el modo del turista, sin ellas, lo cubre `verificar-whatsapp.spec.ts`.
 */
describe('VerificarWhatsapp con las entradas del prestador', () => {
  let fixture: ComponentFixture<VerificarWhatsapp>;
  let http: HttpTestingController;
  let actualizarCuenta: ReturnType<typeof vi.spyOn>;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/\s+/g, ' ');

  function escribirCodigo(codigo: string): void {
    const entrada = raiz().querySelector<HTMLInputElement>('#codigo')!;
    entrada.value = codigo;
    entrada.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  const verificar = () => {
    raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
  };

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'PRESTADOR' }))));
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    actualizarCuenta = vi.spyOn(TestBed.inject(SesionService), 'actualizarCuenta');
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    fixture = TestBed.createComponent(VerificarWhatsapp);
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  it('muestra el número y el aviso que le dan, no los del turista', () => {
    fixture.componentRef.setInput('numero', '+573001110202');
    fixture.componentRef.setInput('aviso', 'prestador.whatsapp.avisoVerificar');
    fixture.detectChanges();

    expect(raiz().querySelector('strong')?.textContent).toBe('+573001110202');
    expect(texto()).toContain('el aviso de una solicitud pagada');
    expect(texto()).not.toContain('no podrás enviar solicitudes');
  });

  it('verifica con la función que le dan, avisa a su dueño y no toca la cuenta de la sesión', () => {
    const verificarCon = vi.fn().mockReturnValue(of({ whatsappVerificado: true }));
    const emitido = vi.fn();
    fixture.componentRef.setInput('verificarCon', verificarCon);
    fixture.componentInstance.verificado.subscribe(emitido);
    fixture.detectChanges();
    escribirCodigo('123456');

    verificar();

    expect(verificarCon).toHaveBeenCalledWith('123456');
    expect(emitido).toHaveBeenCalledWith({ whatsappVerificado: true });
    expect(actualizarCuenta).not.toHaveBeenCalled();
    expect(mostrar).toHaveBeenCalledWith('Verificaste tu WhatsApp');
    http.expectNone(() => true);
  });

  it('pide otro código con la función que le dan', () => {
    const reenviarCon = vi.fn().mockReturnValue(of(undefined));
    fixture.componentRef.setInput('reenviarCon', reenviarCon);
    fixture.detectChanges();

    raiz().querySelector<HTMLButtonElement>('.boton--secundario')!.click();
    fixture.detectChanges();

    expect(reenviarCon).toHaveBeenCalledTimes(1);
    expect(texto()).toContain('Te enviamos un código nuevo');
    http.expectNone(() => true);
  });

  it('un código incorrecto dice cuántos intentos quedan', () => {
    const fallo = new ErrorApi(400, 'CODIGO_INCORRECTO', 'errores.codigo.CODIGO_INCORRECTO', undefined, undefined, {
      intentosRestantes: 2,
    });
    fixture.componentRef.setInput('verificarCon', () => throwError(() => fallo));
    fixture.detectChanges();
    escribirCodigo('000000');

    verificar();

    expect(raiz().querySelector('.alerta--error')?.textContent).toContain('Te quedan 2 intentos');
  });

  it('mientras verifica no deja verificar de nuevo', () => {
    const pendiente = new Subject<unknown>();
    const verificarCon = vi.fn().mockReturnValue(pendiente);
    fixture.componentRef.setInput('verificarCon', verificarCon);
    fixture.detectChanges();
    escribirCodigo('123456');

    verificar();
    verificar();

    expect(verificarCon).toHaveBeenCalledTimes(1);
  });
});
