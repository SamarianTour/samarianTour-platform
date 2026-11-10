import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EstadoSolicitud, IntentoPago, SolicitudTurista } from '../../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../../core/sesion/testing';
import { DetalleSolicitud } from './detalle-solicitud';

const ID = '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20';
const URL = `/api/nucleo/turista/solicitudes/${ID}`;
const ZONAS = [
  { id: 'a05', nombre: 'Taganga', altaAfluencia: false, areaProtegida: false, buenasPracticas: [] },
];
const AHORA = Date.parse('2026-10-26T15:00:00Z');
const HORA = 3_600_000;
const en = (ms: number) => new Date(AHORA + ms).toISOString();

function solicitud(
  estado: EstadoSolicitud,
  extra: Partial<SolicitudTurista> = {},
): SolicitudTurista {
  return {
    id: ID,
    oferta: {
      id: 'b00000000000000000000001',
      tipo: 'ACTIVIDAD',
      nombre: 'Caminata a Marinka',
      zonaId: 'a05',
    },
    fechaInicio: '2026-10-28',
    horaSalida: '07:00:00',
    personas: 2,
    nota: 'Vamos con una niña de 8 años',
    estado,
    creadaEn: '2026-10-26T14:00:00',
    actualizadaEn: '2026-10-26T14:30:00',
    historial: [{ a: 'PENDIENTE', en: '2026-10-26T14:00:00', actor: 'TURISTA' }],
    ...extra,
  };
}

const ATENDIDA = solicitud('ATENDIDA', {
  venceEn: en(3 * HORA),
  condiciones: {
    precioBase: 300000,
    politicaCancelacion: 'Sin reembolso a menos de 24 horas del servicio',
    nota: 'Incluye el ingreso al parque',
    aparteHasta: en(3 * HORA),
  },
  historial: [
    { de: 'PENDIENTE', a: 'ATENDIDA', en: '2026-10-26T14:30:00', actor: 'OPERADOR' },
    { a: 'PENDIENTE', en: '2026-10-26T14:00:00', actor: 'TURISTA' },
  ],
});

describe('DetalleSolicitud (P02.2)', () => {
  let fixture: ComponentFixture<DetalleSolicitud>;
  let http: HttpTestingController;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const consulta = () => http.expectOne(URL);
  const bloqueDePago = () => raiz().querySelector('app-pagar-solicitud');

  /**
   * Abre el detalle con la sesión de un turista y responde las zonas y la solicitud. Una solicitud por pagar, con el pago
   * fallido o pagada también pide sus intentos de pago, que se responden con `intentos` (ninguno por defecto).
   */
  function crear(contenido: SolicitudTurista, id = ID, intentos: IntentoPago[] = []): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
    );
    fixture = TestBed.createComponent(DetalleSolicitud);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    http.expectOne('/api/catalogo/zonas').flush(ZONAS);
    http.expectOne(`/api/nucleo/turista/solicitudes/${id}`).flush(contenido);
    fixture.detectChanges();
    if (
      contenido.estado === 'CONFIRMADA' ||
      contenido.estado === 'PAGO_FALLIDO' ||
      contenido.estado === 'PAGADA'
    ) {
      http.expectOne(`/api/nucleo/turista/solicitudes/${id}/pagos`).flush(intentos);
      fixture.detectChanges();
    }
  }

  beforeEach(() => {
    vi.useFakeTimers({ now: AHORA, toFake: ['Date', 'setInterval', 'clearInterval'] });
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

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  describe('la solicitud', () => {
    it('muestra la oferta, el estado y lo que significa', () => {
      crear(ATENDIDA);

      expect(raiz().querySelector('h1')?.textContent).toBe('Caminata a Marinka');
      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe(
        'Disponible, confirma',
      );
      expect(texto()).toContain(
        'Hay cupo. Revisa las condiciones y confirma o declina antes del plazo.',
      );
      expect(texto()).toContain('Actividad');
    });

    it('muestra lo que pidió: fecha, hora sin segundos, personas, comentarios y zona', () => {
      crear(ATENDIDA);

      expect(texto()).toContain('28 de octubre de 2026');
      expect(texto()).toContain('Hora de salida07:00');
      expect(texto()).toContain('Personas2');
      expect(texto()).toContain('Vamos con una niña de 8 años');
      expect(texto()).toContain('ZonaTaganga'.replace('Zona', 'Zona'));
      expect(texto()).toContain('Referencia6F1C2A9E');
    });

    it('un alojamiento muestra entrada, salida y unidades', () => {
      crear(
        solicitud('PENDIENTE', {
          oferta: { id: 'b2', tipo: 'ALOJAMIENTO', nombre: 'Cabañas', zonaId: 'a05' },
          horaSalida: undefined,
          fechaSalida: '2026-10-30',
          numeroUnidades: 2,
          personas: 3,
        }),
      );

      expect(texto()).toContain('Fecha de entrada28 de octubre de 2026');
      expect(texto()).toContain('Fecha de salida30 de octubre de 2026');
      expect(texto()).toContain('Unidades2');
    });

    it('un restaurante muestra la hora de la reserva', () => {
      crear(
        solicitud('PENDIENTE', {
          oferta: { id: 'b3', tipo: 'RESTAURANTE', nombre: 'La Terraza', zonaId: 'a05' },
          horaSalida: undefined,
          horaReserva: '19:30',
        }),
      );

      expect(texto()).toContain('Hora de la reserva19:30');
    });

    it('sin comentarios no muestra el bloque', () => {
      crear(solicitud('PENDIENTE', { nota: undefined }));

      expect(texto()).not.toContain('Tus comentarios');
    });
  });

  describe('condiciones del operador', () => {
    it('muestra el precio, la política de cancelación, la nota y hasta cuándo se aparta el cupo', () => {
      crear(ATENDIDA);

      expect(texto()).toContain('Total a pagar$ 300.000');
      expect(texto()).toContain('Sin reembolso a menos de 24 horas del servicio');
      expect(texto()).toContain('Nota del operador');
      expect(texto()).toContain('Incluye el ingreso al parque');
      expect(texto()).toContain('Cupo apartado hasta');
    });

    it('una solicitud pendiente todavía no tiene condiciones', () => {
      crear(solicitud('PENDIENTE'));

      expect(texto()).not.toContain('Total a pagar');
      expect(raiz().querySelector('#condiciones-titulo')).toBeNull();
    });

    it('sin nota del operador, no muestra ese renglón', () => {
      crear(
        solicitud('CONFIRMADA', {
          condiciones: { precioBase: 300000, politicaCancelacion: 'Sin reembolso' },
        }),
      );

      expect(texto()).toContain('Sin reembolso');
      expect(texto()).not.toContain('Nota del operador');
    });
  });

  describe('historial', () => {
    it('va del primer paso al último, con quién lo hizo y cuándo', () => {
      crear(ATENDIDA);

      const pasos = Array.from(raiz().querySelectorAll('.historial__paso')).map((p) =>
        p.querySelector('.historial__estado')?.textContent?.replace(/\s+/g, ' ').trim(),
      );
      expect(pasos).toEqual(['Pendiente · Tú', 'Disponible, confirma · Operador']);
      expect(raiz().querySelector('.historial time')?.getAttribute('datetime')).toBe(
        '2026-10-26T14:00:00',
      );
    });

    it('muestra el motivo de un cambio cuando lo hay', () => {
      crear(
        solicitud('SIN_RESPUESTA', {
          historial: [
            { a: 'PENDIENTE', en: '2026-10-26T14:00:00', actor: 'TURISTA' },
            {
              de: 'PENDIENTE',
              a: 'SIN_RESPUESTA',
              en: '2026-10-27T14:00:00',
              actor: 'SISTEMA',
              motivo: 'El plazo de 24 h terminó',
            },
          ],
        }),
      );

      expect(texto()).toContain('Sin respuesta · Sistema');
      expect(texto()).toContain('Motivo: El plazo de 24 h terminó');
    });
  });

  describe('pasos del flujo', () => {
    const actual = () =>
      Array.from(raiz().querySelectorAll('app-pasos-solicitud li.paso')).map((p) =>
        p.getAttribute('aria-current'),
      );

    it('atendida está en "Confirmar"', () => {
      crear(ATENDIDA);

      expect(actual()).toEqual([null, 'step', null, null]);
    });

    it.each([
      ['PENDIENTE', [0]],
      ['CONFIRMADA', [2]],
      ['PAGO_FALLIDO', [2]],
      ['PAGADA', [3]],
    ] as const)('%s marca el paso %j', (estado, [indice]) => {
      crear(solicitud(estado));

      expect(actual()[indice]).toBe('step');
    });

    it.each(['NO_DISPONIBLE', 'SIN_RESPUESTA', 'DECLINADA', 'VENCIDA'] as const)(
      '%s ya no avanza en el flujo: no muestra pasos',
      (estado) => {
        crear(solicitud(estado));

        expect(raiz().querySelector('app-pasos-solicitud')).toBeNull();
      },
    );
  });

  describe('plazo', () => {
    it('dice cuánto queda y se marca urgente con 6 horas o menos', () => {
      crear(ATENDIDA);

      expect(raiz().querySelector('.plazo')?.textContent).toContain(
        'Te quedan 3 h 0 min para confirmar o declinar.',
      );
      expect(raiz().querySelector('.plazo')?.classList).toContain('plazo--urgente');
    });

    it('con más de 6 horas es un aviso informativo', () => {
      crear(solicitud('PENDIENTE', { venceEn: en(20 * HORA) }));

      expect(raiz().querySelector('.plazo')?.textContent).toContain(
        'El operador tiene 20 h 0 min para responder.',
      );
      expect(raiz().querySelector('.plazo')?.classList).not.toContain('plazo--urgente');
    });

    it('una solicitud cerrada no tiene plazo', () => {
      crear(solicitud('PAGADA'));

      expect(raiz().querySelector('.plazo')).toBeNull();
    });

    it('se actualiza solo con el paso del tiempo', () => {
      crear(ATENDIDA);

      vi.advanceTimersByTime(30 * 60_000);
      fixture.detectChanges();

      expect(raiz().querySelector('.plazo')?.textContent).toContain('Te quedan 2 h 30 min');
    });

    it('cuando termina consulta el estado real, una sola vez', () => {
      crear(ATENDIDA);

      vi.advanceTimersByTime(3 * HORA + 60_000);
      fixture.detectChanges();
      consulta().flush(ATENDIDA);
      fixture.detectChanges();

      expect(raiz().querySelector('.plazo')?.textContent).toContain(
        'El plazo terminó. Estamos actualizando el estado.',
      );
      vi.advanceTimersByTime(5 * 60_000);
      fixture.detectChanges();
      http.expectNone(URL);
    });

    it('si al volver a consultar el núcleo ya la cerró, el detalle lo refleja', () => {
      crear(solicitud('PENDIENTE', { venceEn: en(10 * 60_000) }));

      vi.advanceTimersByTime(11 * 60_000);
      fixture.detectChanges();
      consulta().flush(solicitud('SIN_RESPUESTA'));
      fixture.detectChanges();

      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe('Sin respuesta');
      expect(raiz().querySelector('.plazo')).toBeNull();
    });
  });

  describe('qué puede hacer', () => {
    const lateral = () => raiz().querySelector<HTMLAnchorElement>('.detalle__lateral > a')!;

    it.each(['NO_DISPONIBLE', 'SIN_RESPUESTA', 'VENCIDA'] as const)(
      '%s: volver a solicitar',
      (estado) => {
        crear(solicitud(estado));

        expect(lateral().textContent).toContain('Volver a solicitar');
        expect(lateral().getAttribute('href')).toBe('/turista/solicitar/b00000000000000000000001');
      },
    );

    it('pagada: dejar su opinión', () => {
      crear(solicitud('PAGADA'));

      expect(lateral().textContent).toContain('Dejar opinión');
      expect(lateral().getAttribute('href')).toBe('/ofertas/b00000000000000000000001');
    });

    it.each(['PENDIENTE', 'ATENDIDA', 'DECLINADA'] as const)('%s: ver la oferta', (estado) => {
      crear(solicitud(estado));

      expect(lateral().textContent).toContain('Ver oferta');
    });

    it('una solicitud por pagar ofrece pagar, no solo ver la oferta', () => {
      crear(
        solicitud('CONFIRMADA', { condiciones: { precioBase: 300000, politicaCancelacion: 'x' } }),
      );

      expect(bloqueDePago()).not.toBeNull();
    });
  });

  describe('cuando no se puede ver', () => {
    it('la solicitud de otro turista (404) se explica', () => {
      fixture = TestBed.createComponent(DetalleSolicitud);
      fixture.componentRef.setInput('id', 'ajena');
      fixture.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush(ZONAS);

      http
        .expectOne('/api/nucleo/turista/solicitudes/ajena')
        .flush(
          { status: 404, codigo: 'SOLICITUD_NO_ENCONTRADA' },
          { status: 404, statusText: 'Not Found' },
        );
      fixture.detectChanges();

      expect(texto()).toContain('No encontramos esta solicitud');
      expect(raiz().querySelector('.vacio a')?.getAttribute('href')).toBe('/turista/solicitudes');
    });

    it('un fallo del servidor se muestra y cambia de idioma', () => {
      fixture = TestBed.createComponent(DetalleSolicitud);
      fixture.componentRef.setInput('id', ID);
      fixture.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush(ZONAS);

      consulta().error(new ProgressEvent('error'));
      fixture.detectChanges();
      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos conectar con el servidor',
      );

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();
      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        "We couldn't connect to the server",
      );
    });

    it('mientras carga lo dice', () => {
      fixture = TestBed.createComponent(DetalleSolicitud);
      fixture.componentRef.setInput('id', ID);
      fixture.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush(ZONAS);

      expect(texto()).toContain('Cargando la solicitud…');
      consulta().flush(ATENDIDA);
    });

    it('si las zonas no cargan, el detalle igual se ve, sin la zona', () => {
      fixture = TestBed.createComponent(DetalleSolicitud);
      fixture.componentRef.setInput('id', ID);
      fixture.detectChanges();
      http.expectOne('/api/catalogo/zonas').error(new ProgressEvent('error'));
      consulta().flush(ATENDIDA);
      fixture.detectChanges();

      expect(raiz().querySelector('h1')?.textContent).toBe('Caminata a Marinka');
      expect(texto()).not.toContain('Taganga');
    });
  });

  describe('reconsulta (RF-33)', () => {
    it('al volver a la pestaña actualiza el estado sin mostrar "cargando"', () => {
      crear(solicitud('PENDIENTE', { venceEn: en(5 * HORA) }));

      document.dispatchEvent(new Event('visibilitychange'));
      expect(texto()).not.toContain('Cargando la solicitud…');
      consulta().flush(ATENDIDA);
      fixture.detectChanges();

      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe(
        'Disponible, confirma',
      );
      expect(texto()).toContain('Sin reembolso a menos de 24 horas del servicio');
    });

    it('al reconectarse consulta de nuevo', () => {
      crear(ATENDIDA);

      window.dispatchEvent(new Event('online'));

      consulta().flush(ATENDIDA);
    });

    it('si la reconsulta falla, conserva lo que ya se veía y avisa', () => {
      crear(ATENDIDA);

      window.dispatchEvent(new Event('online'));
      consulta().error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('h1')?.textContent).toBe('Caminata a Marinka');
      expect(texto()).toContain('No pudimos actualizar el estado.');

      // Cuando una reconsulta sale bien, el aviso desaparece.
      window.dispatchEvent(new Event('online'));
      consulta().flush(ATENDIDA);
      fixture.detectChanges();
      expect(texto()).not.toContain('No pudimos actualizar el estado.');
    });

    it('si la solicitud ya no existe al reconsultar, lo dice', () => {
      crear(ATENDIDA);

      window.dispatchEvent(new Event('online'));
      consulta().flush(
        { status: 404, codigo: 'SOLICITUD_NO_ENCONTRADA' },
        { status: 404, statusText: 'Not Found' },
      );
      fixture.detectChanges();

      expect(texto()).toContain('No encontramos esta solicitud');
    });

    it('al cambiar de solicitud vuelve a cargar desde cero', () => {
      crear(ATENDIDA);

      fixture.componentRef.setInput('id', 'otra');
      fixture.detectChanges();

      expect(texto()).toContain('Cargando la solicitud…');
      http
        .expectOne('/api/nucleo/turista/solicitudes/otra')
        .flush(solicitud('PAGADA', { id: 'otra' }));
      fixture.detectChanges();
      // Pagada: se piden también sus intentos de pago, para mostrar la referencia de la transacción.
      http.expectOne('/api/nucleo/turista/solicitudes/otra/pagos').flush([]);
    });
  });

  describe('confirmar o declinar (P02.3)', () => {
    const bloque = () => raiz().querySelector('app-responder-condiciones');
    const boton = (n: number) =>
      raiz().querySelectorAll<HTMLButtonElement>(
        'app-responder-condiciones .decision__acciones button',
      )[n];
    const respuesta = () => http.expectOne(`${URL}/respuesta`);

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

    it('solo una solicitud atendida se puede responder', () => {
      crear(ATENDIDA);
      expect(bloque()).not.toBeNull();
    });

    it.each(['PENDIENTE', 'CONFIRMADA', 'DECLINADA', 'PAGO_FALLIDO', 'PAGADA', 'VENCIDA'] as const)(
      '%s no muestra la decisión',
      (estado) => {
        crear(solicitud(estado));

        expect(bloque()).toBeNull();
      },
    );

    it('muestra lo que propuso el operador y el plazo junto a los botones', () => {
      crear(ATENDIDA);

      expect(texto()).toContain('Total a pagar$ 300.000');
      expect(texto()).toContain('Incluye el ingreso al parque');
      expect(raiz().querySelector('.plazo')?.textContent).toContain('Te quedan 3 h 0 min');
      expect(boton(0).textContent).toContain('Confirmar condiciones');
    });

    it('confirmar deja la solicitud "Por pagar", sin el bloque de decisión y con el foco en el título', () => {
      crear(ATENDIDA);

      boton(0).click();
      fixture.detectChanges();
      respuesta().flush({
        ...ATENDIDA,
        estado: 'CONFIRMADA',
        actualizadaEn: '2026-10-26T15:05:00',
      });
      fixture.detectChanges();
      // Ya por pagar, el detalle pide los intentos de pago.
      http.expectOne(`${URL}/pagos`).flush([]);
      fixture.detectChanges();

      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe('Por pagar');
      expect(bloque()).toBeNull();
      expect(texto()).toContain('Total a pagar$ 300.000');
      expect(
        Array.from(raiz().querySelectorAll('app-pasos-solicitud li.paso')).map((p) =>
          p.getAttribute('aria-current'),
        ),
      ).toEqual([null, null, 'step', null]);
      expect(document.activeElement?.id).toBe('titulo-solicitud');
    });

    it('declinar, tras confirmarlo en el diálogo, cierra la solicitud', () => {
      crear(ATENDIDA);

      boton(1).click();
      fixture.detectChanges();
      raiz().querySelector<HTMLButtonElement>('dialog .boton--peligro')!.click();
      fixture.detectChanges();
      respuesta().flush({ ...ATENDIDA, estado: 'DECLINADA', venceEn: undefined });
      fixture.detectChanges();

      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe('Declinada');
      expect(bloque()).toBeNull();
      expect(raiz().querySelector('.plazo')).toBeNull();
      expect(raiz().querySelector('app-pasos-solicitud')).toBeNull();
    });

    it('si el núcleo dice que el plazo venció, consulta el estado real y lo muestra', () => {
      crear(ATENDIDA);

      boton(0).click();
      respuesta().flush(
        { status: 409, codigo: 'PLAZO_VENCIDO' },
        { status: 409, statusText: 'Conflict' },
      );
      fixture.detectChanges();
      consulta().flush(solicitud('VENCIDA', { venceEn: undefined }));
      fixture.detectChanges();

      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe('Vencida');
      expect(bloque()).toBeNull();
    });

    it('con el plazo vencido en pantalla, los botones están deshabilitados mientras se consulta', () => {
      crear(ATENDIDA);

      vi.advanceTimersByTime(3 * HORA + 60_000);
      fixture.detectChanges();

      expect(boton(0).disabled).toBe(true);
      expect(boton(1).disabled).toBe(true);
      consulta().flush(ATENDIDA);
    });
  });

  describe('inglés', () => {
    it('todo cambia al instante: estado, datos, condiciones, historial y plazo', () => {
      crear(ATENDIDA);

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();

      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe(
        'Available, confirm',
      );
      expect(texto()).toContain('Request details');
      expect(texto()).toContain('Your request');
      expect(texto()).toContain('Departure time07:00');
      expect(texto()).toContain('October 28, 2026');
      expect(texto()).toContain('Terms');
      expect(texto()).toContain('Total to pay');
      expect(texto()).toContain('COP 300,000');
      expect(texto()).toContain('Cancellation policy');
      expect(texto()).toContain('History');
      expect(texto()).toContain('Pending · You');
      expect(texto()).toContain('Available, confirm · Operator');
      expect(texto()).toContain('You have 3 h 0 min left to confirm or decline.');
      expect(texto()).toContain('View offer');
    });
  });

  describe('pagar la solicitud (P04.1)', () => {
    const CONFIRMADA = solicitud('CONFIRMADA', {
      venceEn: en(3 * HORA),
      condiciones: { precioBase: 300000, politicaCancelacion: 'Sin reembolso' },
    });
    const rechazo = (numero: number): IntentoPago => ({
      id: `i${numero}`,
      numero,
      estado: 'RECHAZADO',
      referencia: `ST-${numero}`,
      montoCentavos: 30000000,
      creadoEn: en(-HORA),
    });

    it('una solicitud confirmada muestra el bloque de pago con el total y pide sus intentos', () => {
      crear(CONFIRMADA);

      expect(bloqueDePago()).not.toBeNull();
      expect(bloqueDePago()!.textContent!.replace(/[  ]/g, ' ')).toContain('Pagar $ 300.000');
    });

    it.each(['PENDIENTE', 'ATENDIDA', 'DECLINADA', 'PAGADA', 'VENCIDA'] as const)(
      '%s no muestra el bloque de pago ni pide intentos',
      (estado) => {
        crear(solicitud(estado));

        expect(bloqueDePago()).toBeNull();
        http.expectNone(`${URL}/pagos`);
      },
    );

    it('con intentos usados dice cuántos quedan', () => {
      crear(CONFIRMADA, ID, [rechazo(1)]);

      expect(bloqueDePago()!.textContent).toContain('Te quedan 2 intentos de pago');
    });

    it('si falla la consulta de intentos el bloque sigue ofreciendo pagar', () => {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
      );
      fixture = TestBed.createComponent(DetalleSolicitud);
      fixture.componentRef.setInput('id', ID);
      fixture.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush(ZONAS);
      http.expectOne(URL).flush(CONFIRMADA);
      fixture.detectChanges();
      http.expectOne(`${URL}/pagos`).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(bloqueDePago()).not.toBeNull();
      expect(bloqueDePago()!.textContent).toContain('Te quedan 3 intentos de pago');
    });

    it('confirmar las condiciones lleva al pago: pide los intentos y muestra el bloque', () => {
      crear(ATENDIDA);

      Array.from(raiz().querySelectorAll<HTMLButtonElement>('button.boton'))
        .find((b) => b.textContent?.includes('Confirmar condiciones'))!
        .click();
      fixture.detectChanges();
      http.expectOne(`${URL}/respuesta`).flush({ ...ATENDIDA, estado: 'CONFIRMADA', actualizadaEn: en(0) });
      fixture.detectChanges();
      http.expectOne(`${URL}/pagos`).flush([]);
      fixture.detectChanges();

      expect(bloqueDePago()).not.toBeNull();
    });
  });

  describe('esperar la confirmación del pago (RF-50, RF-51)', () => {
    const CONFIRMADA = solicitud('CONFIRMADA', {
      venceEn: en(3 * HORA),
      condiciones: { precioBase: 300000, politicaCancelacion: 'Sin reembolso' },
    });
    const intentoPago = (
      numero: number,
      estado: IntentoPago['estado'],
      extra: Partial<IntentoPago> = {},
    ): IntentoPago => ({
      id: `i${numero}`,
      numero,
      estado,
      referencia: `ST-${numero}`,
      montoCentavos: 30000000,
      creadoEn: en(-HORA),
      ...extra,
    });
    const pagos = () => http.expectOne(`${URL}/pagos`);
    const confirmando = () => raiz().querySelector('app-confirmando-pago');
    const pagoConfirmado = () => raiz().querySelector('app-pago-confirmado');

    /** El tiempo corre de verdad solo aquí: la espera usa temporizadores, que las demás pruebas no necesitan. */
    beforeEach(() => {
      vi.useRealTimers();
      vi.useFakeTimers({
        now: AHORA,
        toFake: ['Date', 'setTimeout', 'clearTimeout', 'setInterval', 'clearInterval'],
      });
    });

    /** Pasan los segundos indicados y el servidor responde la solicitud y sus intentos. */
    function reconsultar(
      segundos: number,
      solicitudNueva: SolicitudTurista,
      intentos: IntentoPago[],
    ): void {
      vi.advanceTimersByTime(segundos * 1000);
      consulta().flush(solicitudNueva);
      fixture.detectChanges();
      pagos().flush(intentos);
      fixture.detectChanges();
    }

    it('al volver de la pasarela con un intento en curso dice "Confirmando tu pago", no que se pagó', () => {
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);

      expect(confirmando()).not.toBeNull();
      expect(pagoConfirmado()).toBeNull();
      expect(texto()).toContain('Confirmando tu pago');
      // Mientras tanto se puede continuar el mismo pago, no empezar otro.
      expect(texto()).toContain('Continúa tu pago');
    });

    it('una solicitud por pagar sin un intento en curso no espera nada ni reconsulta', () => {
      crear(CONFIRMADA, ID, [intentoPago(1, 'ABANDONADO')]);

      expect(confirmando()).toBeNull();
      vi.advanceTimersByTime(60_000);
      http.expectNone(URL);
    });

    it('reconsulta a los 3 segundos y, cuando el servidor dice pagada, confirma el pago con la referencia', () => {
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);
      http.expectNone(URL);

      reconsultar(3, solicitud('PAGADA', { condiciones: CONFIRMADA.condiciones, actualizadaEn: en(0) }), [
        intentoPago(1, 'APROBADO', { transaccionId: '1234-1610641025-49201', resueltoEn: en(0) }),
      ]);

      expect(confirmando()).toBeNull();
      expect(pagoConfirmado()).not.toBeNull();
      expect(texto()).toContain('Pago confirmado');
      expect(texto()).toContain('1234-1610641025-49201');
      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe('Pagada');
      // El turista esperaba: el foco va al aviso para que el cambio se anuncie.
      expect(document.activeElement?.id).toBe('confirmado-titulo');
    });

    it('el estado lo decide el servidor: si sigue en curso, sigue esperando y vuelve a preguntar', () => {
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);

      reconsultar(3, CONFIRMADA, [intentoPago(1, 'EN_CURSO')]);
      expect(confirmando()).not.toBeNull();
      expect(pagoConfirmado()).toBeNull();

      reconsultar(3, CONFIRMADA, [intentoPago(1, 'EN_CURSO')]);
      expect(confirmando()).not.toBeNull();
    });

    it('si la pasarela rechaza, deja de esperar y no dice que se pagó', () => {
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);

      reconsultar(3, solicitud('PAGO_FALLIDO', { condiciones: CONFIRMADA.condiciones, venceEn: en(2 * HORA) }), [
        intentoPago(1, 'RECHAZADO'),
      ]);

      expect(confirmando()).toBeNull();
      expect(pagoConfirmado()).toBeNull();
      expect(raiz().querySelector('header .etiqueta')?.textContent?.trim()).toBe('Pago fallido');
      vi.advanceTimersByTime(60_000);
      http.expectNone(URL);
    });

    it('si el intento se abandona deja de esperar y vuelve a ofrecer pagar', () => {
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);

      reconsultar(3, CONFIRMADA, [intentoPago(1, 'ABANDONADO')]);

      expect(confirmando()).toBeNull();
      expect(texto()).toContain('Paga para asegurar tu cupo');
      expect(texto()).toContain('Te quedan 3 intentos de pago');
    });

    it('una vez pagada deja de reconsultar', () => {
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);
      reconsultar(3, solicitud('PAGADA', { condiciones: CONFIRMADA.condiciones }), [
        intentoPago(1, 'APROBADO', { transaccionId: 'T-1' }),
      ]);

      vi.advanceTimersByTime(120_000);

      http.expectNone(URL);
    });

    it('si falla la consulta de los intentos conserva la espera en vez de dar el pago por hecho', () => {
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);

      vi.advanceTimersByTime(3_000);
      consulta().flush(CONFIRMADA);
      fixture.detectChanges();
      pagos().error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(confirmando()).not.toBeNull();
      expect(pagoConfirmado()).toBeNull();
    });

    it('con la pestaña oculta no reconsulta', () => {
      const visibilidad = vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden');
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);

      vi.advanceTimersByTime(30_000);

      http.expectNone(URL);
      visibilidad.mockRestore();
    });

    it('una solicitud ya pagada al abrirla muestra el pago confirmado sin quitarle el foco', () => {
      crear(solicitud('PAGADA', { condiciones: CONFIRMADA.condiciones }), ID, [
        intentoPago(1, 'APROBADO', { transaccionId: 'T-9' }),
      ]);

      expect(pagoConfirmado()).not.toBeNull();
      expect(texto()).toContain('T-9');
      expect(confirmando()).toBeNull();
      expect(document.activeElement?.id).not.toBe('confirmado-titulo');
    });

    it('está en inglés', () => {
      establecerIdioma('en', { recordar: false });
      crear(CONFIRMADA, ID, [intentoPago(1, 'EN_CURSO')]);

      expect(texto()).toContain('Confirming your payment');
      expect(texto()).toContain('Continue your payment');
    });
  });
});
