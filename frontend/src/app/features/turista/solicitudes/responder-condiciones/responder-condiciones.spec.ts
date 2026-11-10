import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SolicitudTurista } from '../../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { ResponderCondiciones } from './responder-condiciones';

const ID = '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20';
const RESPUESTA = `/api/nucleo/turista/solicitudes/${ID}/respuesta`;
const AHORA = Date.parse('2026-10-26T15:00:00Z');

const ATENDIDA = {
  id: ID,
  oferta: { id: 'b1', tipo: 'ACTIVIDAD', nombre: 'Caminata a Marinka', zonaId: 'a05' },
  fechaInicio: '2026-10-28',
  personas: 2,
  estado: 'ATENDIDA',
  // Sin zona horaria: se muestra la misma hora en cualquier máquina que corra la prueba.
  venceEn: '2026-10-26T18:00:00',
  creadaEn: '2026-10-26T14:00:00Z',
  actualizadaEn: '2026-10-26T14:30:00Z',
  historial: [],
} as SolicitudTurista;

describe('ResponderCondiciones (P02.3)', () => {
  let fixture: ComponentFixture<ResponderCondiciones>;
  let http: HttpTestingController;
  let respondidas: SolicitudTurista[];
  let reconsultas: number;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const botones = () =>
    Array.from(raiz().querySelectorAll<HTMLButtonElement>('.decision__acciones button'));
  const confirmar = () => botones()[0];
  const declinar = () => botones()[1];
  const dialogo = () => raiz().querySelector<HTMLDialogElement>('dialog')!;
  const aceptarDialogo = () => dialogo().querySelector<HTMLButtonElement>('.boton--peligro')!;
  const cancelarDialogo = () => dialogo().querySelector<HTMLButtonElement>('.boton--secundario')!;

  function crear(plazoVencido = false): void {
    fixture = TestBed.createComponent(ResponderCondiciones);
    fixture.componentRef.setInput('solicitud', ATENDIDA);
    fixture.componentRef.setInput('plazoVencido', plazoVencido);
    fixture.componentInstance.respondida.subscribe((s) => respondidas.push(s));
    fixture.componentInstance.reconsultar.subscribe(() => reconsultas++);
    fixture.detectChanges();
  }

  const responder = (estado: string) => ({ ...ATENDIDA, estado }) as SolicitudTurista;
  const rechazar = (estadoHttp: number, codigo: string) =>
    http
      .expectOne(RESPUESTA)
      .flush({ status: estadoHttp, codigo }, { status: estadoHttp, statusText: 'Error' });

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
    vi.useFakeTimers({ now: AHORA, toFake: ['Date'] });
    respondidas = [];
    reconsultas = 0;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  describe('qué muestra', () => {
    it('explica qué pasa al confirmar y al declinar, y hasta cuándo se puede decidir', () => {
      crear();

      expect(raiz().querySelector('h2')?.textContent).toBe('Tu decisión');
      expect(texto()).toContain(
        'Si confirmas, la solicitud queda confirmada y podrás pagar dentro del plazo.',
      );
      expect(texto()).toContain('Si declinas, el cupo queda libre y no se cobra nada.');
      expect(texto()).toContain(
        'Tienes hasta el 26 oct 2026, 6:00 p. m. para decidir. Después, la solicitud vence.',
      );
    });

    it('tiene dos botones: confirmar y declinar', () => {
      crear();

      expect(botones().map((b) => b.textContent?.trim())).toEqual([
        'Confirmar condiciones',
        'Declinar',
      ]);
      expect(botones().every((b) => !b.disabled)).toBe(true);
    });

    it('con el plazo vencido no deja decidir', () => {
      crear(true);

      expect(botones().every((b) => b.disabled)).toBe(true);
    });
  });

  describe('confirmar', () => {
    it('envía CONFIRMAR con su clave, avisa y entrega la solicitud con su estado nuevo', () => {
      crear();

      confirmar().click();
      fixture.detectChanges();
      const peticion = http.expectOne(RESPUESTA);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({ decision: 'CONFIRMAR' });
      expect(peticion.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
      peticion.flush(responder('CONFIRMADA'));

      expect(respondidas.map((s) => s.estado)).toEqual(['CONFIRMADA']);
      expect(TestBed.inject(AvisosService).avisos()[0].texto).toBe('Confirmaste las condiciones.');
    });

    it('mientras se envía, ningún botón deja enviar otra vez', () => {
      crear();

      confirmar().click();
      fixture.detectChanges();

      expect(botones().every((b) => b.disabled)).toBe(true);
      expect(confirmar().textContent).toContain('Confirmando…');
      http.expectOne(RESPUESTA).flush(responder('CONFIRMADA'));
    });

    it('confirmar no abre el diálogo de declinar', () => {
      crear();

      confirmar().click();
      fixture.detectChanges();

      expect(dialogo().hasAttribute('open')).toBe(false);
      http.expectOne(RESPUESTA).flush(responder('CONFIRMADA'));
    });
  });

  describe('declinar', () => {
    it('pide confirmación antes de enviar nada', () => {
      crear();

      declinar().click();
      fixture.detectChanges();

      expect(dialogo().hasAttribute('open')).toBe(true);
      expect(dialogo().textContent).toContain('¿Declinar esta solicitud?');
      expect(dialogo().textContent).toContain('No se generará ningún cobro.');
      http.expectNone(RESPUESTA);
    });

    it('cancelar el diálogo no envía nada', () => {
      crear();
      declinar().click();
      fixture.detectChanges();

      cancelarDialogo().click();
      fixture.detectChanges();

      http.expectNone(RESPUESTA);
      expect(respondidas).toEqual([]);
    });

    it('aceptar envía DECLINAR, avisa y entrega la solicitud cerrada', () => {
      crear();
      declinar().click();
      fixture.detectChanges();

      aceptarDialogo().click();
      fixture.detectChanges();
      const peticion = http.expectOne(RESPUESTA);
      expect(peticion.request.body).toEqual({ decision: 'DECLINAR' });
      expect(peticion.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
      peticion.flush(responder('DECLINADA'));

      expect(respondidas.map((s) => s.estado)).toEqual(['DECLINADA']);
      expect(TestBed.inject(AvisosService).avisos()[0].texto).toBe(
        'Declinaste la solicitud. El cupo quedó libre.',
      );
    });
  });

  describe('idempotencia ante un fallo', () => {
    const clave = (peticion: ReturnType<HttpTestingController['expectOne']>) =>
      peticion.request.headers.get('Idempotency-Key');

    it('reintentar la misma decisión repite la clave', () => {
      crear();

      confirmar().click();
      const primera = http.expectOne(RESPUESTA);
      primera.error(new ProgressEvent('error'));
      fixture.detectChanges();
      confirmar().click();
      const reintento = http.expectOne(RESPUESTA);

      expect(clave(reintento)).toBe(clave(primera));
      reintento.flush(responder('CONFIRMADA'));
    });

    it('cambiar de decisión usa otra clave (el cuerpo es distinto)', () => {
      crear();

      confirmar().click();
      const primera = http.expectOne(RESPUESTA);
      primera.error(new ProgressEvent('error'));
      fixture.detectChanges();
      declinar().click();
      fixture.detectChanges();
      aceptarDialogo().click();
      const otra = http.expectOne(RESPUESTA);

      expect(clave(otra)).not.toBe(clave(primera));
      otra.flush(responder('DECLINADA'));
    });
  });

  describe('cuando el núcleo rechaza', () => {
    it('un fallo de conexión lo dice, deja volver a intentar y no reconsulta', () => {
      crear();

      confirmar().click();
      http.expectOne(RESPUESTA).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos conectar con el servidor',
      );
      expect(botones().every((b) => !b.disabled)).toBe(true);
      expect(reconsultas).toBe(0);
    });

    it.each([
      ['PLAZO_VENCIDO', 409, 'El plazo para esta acción ya terminó.'],
      ['TRANSICION_INVALIDA', 409, 'La solicitud cambió de estado mientras la atendías.'],
      ['SIN_TIEMPO_PARA_CONFIRMAR', 409, 'No alcanza el tiempo para confirmar antes del servicio.'],
    ] as const)(
      '%s: lo explica, deja el motivo en un aviso y pide reconsultar',
      (codigo, estadoHttp, mensaje) => {
        crear();

        confirmar().click();
        rechazar(estadoHttp, codigo);
        fixture.detectChanges();

        expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(mensaje);
        expect(TestBed.inject(AvisosService).avisos()[0].texto).toContain(mensaje);
        expect(reconsultas).toBe(1);
        expect(respondidas).toEqual([]);
      },
    );

    it('después de un cambio de estado, la siguiente decisión usa una clave nueva', () => {
      crear();
      confirmar().click();
      const primera = http.expectOne(RESPUESTA);
      primera.flush(
        { status: 409, codigo: 'TRANSICION_INVALIDA' },
        { status: 409, statusText: 'Conflict' },
      );
      fixture.detectChanges();

      confirmar().click();
      const segunda = http.expectOne(RESPUESTA);

      expect(segunda.request.headers.get('Idempotency-Key')).not.toBe(
        primera.request.headers.get('Idempotency-Key'),
      );
      segunda.flush(responder('CONFIRMADA'));
    });

    it('el mensaje de error cambia de idioma con la pantalla abierta', () => {
      crear();
      confirmar().click();
      http.expectOne(RESPUESTA).error(new ProgressEvent('error'));
      fixture.detectChanges();

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        "We couldn't connect to the server",
      );
    });
  });

  describe('inglés', () => {
    it('los textos, los botones y el diálogo', () => {
      crear();

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();

      expect(raiz().querySelector('h2')?.textContent).toBe('Your decision');
      expect(botones().map((b) => b.textContent?.trim())).toEqual(['Confirm terms', 'Decline']);
      expect(texto()).toContain(
        'You have until Oct 26, 2026, 6:00 PM to decide. After that, the request expires.',
      );
      expect(dialogo().textContent).toContain('Decline this request?');
      expect(dialogo().textContent).toContain('Nothing will be charged.');
    });
  });
});
