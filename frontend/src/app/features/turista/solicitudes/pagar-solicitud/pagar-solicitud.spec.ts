import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EstadoIntento, IntentoPago, SolicitudTurista } from '../../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { Navegador } from '../../../../core/navegacion/navegador';
import { Plazo, plazoRestante } from '../plazo';
import { PagarSolicitud } from './pagar-solicitud';

const ID = '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20';
const PAGO = `/api/nucleo/turista/solicitudes/${ID}/pago`;
const AHORA = Date.parse('2026-10-26T15:00:00Z');
const HORA = 3_600_000;
const en = (ms: number) => new Date(AHORA + ms).toISOString();

const CONFIRMADA = {
  id: ID,
  oferta: { id: 'b1', tipo: 'ACTIVIDAD', nombre: 'Caminata a Marinka', zonaId: 'a05' },
  fechaInicio: '2026-10-28',
  horaSalida: '07:00:00',
  personas: 2,
  estado: 'CONFIRMADA',
  venceEn: en(3 * HORA),
  condiciones: { precioBase: 300000, politicaCancelacion: 'Sin reembolso' },
  creadaEn: en(-5 * HORA),
  actualizadaEn: en(-HORA),
  historial: [],
} as SolicitudTurista;

const VIGENTE: Plazo = plazoRestante(en(3 * HORA), AHORA);
const VENCIDO: Plazo = plazoRestante(en(-HORA), AHORA);

function intento(numero: number, estado: EstadoIntento): IntentoPago {
  return {
    id: `i${numero}`,
    numero,
    estado,
    referencia: `ST-${numero}`,
    montoCentavos: 30000000,
    creadoEn: en(-HORA),
  };
}

const SESION = (url: string) => ({
  intentoId: '2b7e3f10-5c4d-4a8e-9f61-0d2c3b4a5e6f',
  solicitudId: ID,
  numero: 1,
  estado: 'EN_CURSO',
  referencia: 'ST-2b7e3f10',
  montoCentavos: 30000000,
  moneda: 'COP',
  creadoEn: en(0),
  checkout: { pasarela: 'SIMULADA', url, urlRedireccion: `http://localhost:8080/turista/solicitudes/${ID}` },
});

describe('PagarSolicitud (P04.1)', () => {
  let fixture: ComponentFixture<PagarSolicitud>;
  let http: HttpTestingController;
  let irA: ReturnType<typeof vi.fn>;
  let reconsultas: number;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const boton = () => raiz().querySelector<HTMLButtonElement>('button')!;

  function crear(
    intentos: IntentoPago[] = [],
    plazo: Plazo = VIGENTE,
    solicitud: SolicitudTurista = CONFIRMADA,
  ): void {
    fixture = TestBed.createComponent(PagarSolicitud);
    fixture.componentRef.setInput('solicitud', solicitud);
    fixture.componentRef.setInput('intentos', intentos);
    fixture.componentRef.setInput('plazo', plazo);
    reconsultas = 0;
    fixture.componentInstance.reconsultar.subscribe(() => reconsultas++);
    fixture.detectChanges();
  }

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    irA = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: Navegador, useValue: { irA } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('el resumen', () => {
    it('muestra la oferta, la fecha con su hora, las personas y el total a pagar', () => {
      crear();

      expect(texto()).toContain('Caminata a Marinka');
      expect(texto()).toContain('28 de octubre de 2026, 07:00');
      expect(texto()).toContain('Total a pagar$ 300.000');
      expect(boton().textContent?.replace(/[  ]/g, ' ').trim()).toBe('Pagar $ 300.000');
    });

    it('aclara que el total no tiene cargos adicionales para el turista: la comisión se descuenta al prestador', () => {
      crear();

      expect(texto()).toContain(
        'Este total no tiene cargos adicionales para ti: la comisión de la plataforma se descuenta al prestador.',
      );
    });

    it('explica que el pago es en la pasarela externa y que no se piden datos de tarjeta', () => {
      crear();

      expect(texto()).toContain('pasarela de pago externa');
      expect(texto()).toContain('no pide ni guarda los datos de tu tarjeta');
    });

    it('NUNCA pide datos de tarjeta ni de cuenta: no hay ningún campo para escribir', () => {
      crear();

      expect(raiz().querySelector('input, select, textarea, form')).toBeNull();
    });

    it('dice cuántos intentos de pago quedan', () => {
      crear();

      expect(texto()).toContain('Te quedan 3 intentos de pago');
    });

    it('antes del último intento avisa que si se rechaza la solicitud vencerá', () => {
      crear([intento(1, 'RECHAZADO'), intento(2, 'ERROR')]);

      expect(texto()).toContain('Te queda 1 intento de pago');
      expect(raiz().querySelector('.alerta--aviso')?.textContent).toContain(
        'si el pago se rechaza, la solicitud vencerá',
      );
    });

    it('con dos intentos todavía no hace ese aviso', () => {
      crear([intento(1, 'RECHAZADO')]);

      expect(texto()).toContain('Te quedan 2 intentos de pago');
      expect(texto()).not.toContain('la solicitud vencerá');
    });
  });

  describe('pagar', () => {
    it('pide la sesión de cobro con su clave de idempotencia y sin cuerpo, y abre el checkout en esta pestaña', () => {
      crear();

      boton().click();
      fixture.detectChanges();

      const peticion = http.expectOne(PAGO);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toBeNull();
      expect(peticion.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
      peticion.flush(SESION('http://localhost:8080/api/nucleo/pasarela-simulada/ST-2b7e3f10?monto=30000000'));
      fixture.detectChanges();

      expect(irA).toHaveBeenCalledTimes(1);
      expect(irA).toHaveBeenCalledWith(
        'http://localhost:8080/api/nucleo/pasarela-simulada/ST-2b7e3f10?monto=30000000',
      );
    });

    it('abre la pasarela de Wompi con su dirección completa', () => {
      crear();
      const wompi =
        'https://checkout.wompi.co/p/?public-key=pub_test_X&currency=COP&amount-in-cents=30000000&reference=ST-1&signature:integrity=9a1f';

      boton().click();
      http.expectOne(PAGO).flush(SESION(wompi));

      expect(irA).toHaveBeenCalledWith(wompi);
    });

    it('mientras abre la pasarela deshabilita el botón y un doble clic envía una sola petición', () => {
      crear();

      boton().click();
      boton().click();
      fixture.detectChanges();

      expect(http.match(PAGO)).toHaveLength(1);
      expect(boton().disabled).toBe(true);
      expect(texto()).toContain('Abriendo la pasarela…');
    });

    it('deja un enlace a la pasarela por si el navegador no la abre solo', () => {
      crear();

      boton().click();
      http.expectOne(PAGO).flush(SESION('https://checkout.wompi.co/p/?x=1'));
      fixture.detectChanges();

      const enlace = raiz().querySelector<HTMLAnchorElement>('.pago__manual a')!;
      expect(enlace.getAttribute('href')).toBe('https://checkout.wompi.co/p/?x=1');
    });

    it('NO abre una dirección que no sea http(s): lo dice y deja reintentar', () => {
      crear();

      boton().click();
      http.expectOne(PAGO).flush(SESION('javascript:alert(1)'));
      fixture.detectChanges();

      expect(irA).not.toHaveBeenCalled();
      expect(raiz().querySelector('.alerta--error')?.textContent).toContain(
        'No pudimos abrir la pasarela de pago',
      );
      expect(boton().disabled).toBe(false);
    });

    it('si la petición falla avisa del error y deja reintentar, sin reconsultar la solicitud', () => {
      crear();

      boton().click();
      http.expectOne(PAGO).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
      expect(boton().disabled).toBe(false);
      expect(irA).not.toHaveBeenCalled();
      expect(reconsultas).toBe(0);
    });
  });

  describe('cuando el núcleo dice que ya no se puede pagar', () => {
    it.each(['SOLICITUD_NO_COBRABLE', 'INTENTOS_AGOTADOS', 'PLAZO_VENCIDO', 'CONFLICTO_CONCURRENCIA'])(
      '%s avisa y pide reconsultar la solicitud',
      (codigo) => {
        const avisos = TestBed.inject(AvisosService);
        const mostrar = vi.spyOn(avisos, 'mostrar');
        crear();

        boton().click();
        http.expectOne(PAGO).flush({ codigo }, { status: 409, statusText: 'Conflict' });
        fixture.detectChanges();

        expect(reconsultas).toBe(1);
        expect(mostrar).toHaveBeenCalledTimes(1);
        expect(raiz().querySelector('.alerta--error')).not.toBeNull();
        expect(irA).not.toHaveBeenCalled();
      },
    );
  });

  describe('cuando no se puede pagar', () => {
    it('con el plazo terminado deshabilita el botón y lo explica', () => {
      crear([], VENCIDO);

      expect(boton().disabled).toBe(true);
      expect(texto()).toContain('El plazo terminó');
      boton().click();
      http.expectNone(PAGO);
    });

    it('sin intentos deshabilita el botón y dice que no quedan', () => {
      crear([intento(1, 'RECHAZADO'), intento(2, 'RECHAZADO'), intento(3, 'ERROR')]);

      expect(boton().disabled).toBe(true);
      expect(texto()).toContain('No te quedan intentos de pago.');
      http.expectNone(PAGO);
    });
  });

  describe('en inglés', () => {
    it('muestra el resumen, el botón y el aviso de cargos en inglés', () => {
      establecerIdioma('en', { recordar: false });
      crear([intento(1, 'RECHAZADO'), intento(2, 'RECHAZADO')]);

      expect(texto()).toContain('Pay to secure your spot');
      expect(texto()).toContain('Total to pay');
      expect(texto()).toContain('COP 300,000');
      expect(boton().textContent?.replace(/[  ]/g, ' ').trim()).toBe('Pay COP 300,000');
      expect(texto()).toContain(
        'This total has no additional charges for you: the platform fee is deducted from the provider.',
      );
      expect(texto()).toContain('You have 1 payment attempt left');
      expect(texto()).toContain('if the payment is rejected, the request will expire');
    });
  });
});
