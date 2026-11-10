import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { EstadoSolicitud, SolicitudResumen } from '../../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../../core/sesion/testing';
import { ESTADOS_SOLICITUD } from '../estado-solicitud';
import { MisSolicitudes } from './mis-solicitudes';

const URL = '/api/nucleo/turista/solicitudes';
const AHORA = Date.parse('2026-10-26T15:00:00Z');
const HORA = 3_600_000;
const en = (ms: number) => new Date(AHORA + ms).toISOString();

function solicitud(
  i: number,
  estado: EstadoSolicitud,
  extra: Partial<SolicitudResumen> = {},
): SolicitudResumen {
  return {
    id: `6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a2${i}`,
    oferta: {
      id: `b0000000000000000000000${i}`,
      tipo: 'ACTIVIDAD',
      nombre: `Oferta ${i}`,
      zonaId: 'z1',
    },
    fechaInicio: '2026-10-28',
    horaSalida: '07:00:00',
    personas: 2,
    estado,
    creadaEn: en(-2 * HORA),
    actualizadaEn: en(-HORA),
    ...extra,
  };
}

const pagina = (contenido: SolicitudResumen[], extra: object = {}) => ({
  contenido,
  pagina: 0,
  tamano: 10,
  totalElementos: contenido.length,
  totalPaginas: 1,
  ...extra,
});

describe('MisSolicitudes (P02.2)', () => {
  let fixture: ComponentFixture<MisSolicitudes>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const tarjetas = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.solicitud'));
  const filtro = (nombre: string) =>
    Array.from(raiz().querySelectorAll<HTMLButtonElement>('.filtros button')).find((b) =>
      b.textContent?.includes(nombre),
    )!;
  const lista = () => http.expectOne((p) => p.url === URL);

  /** Abre la lista con la sesión de un turista y responde la primera consulta. */
  function crear(contenido: SolicitudResumen[], extra: object = {}): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
    );
    fixture = TestBed.createComponent(MisSolicitudes);
    fixture.detectChanges();
    lista().flush(pagina(contenido, extra));
    fixture.detectChanges();
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
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  describe('la consulta', () => {
    it('pide la primera página de 10', () => {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
      );
      fixture = TestBed.createComponent(MisSolicitudes);
      fixture.detectChanges();

      const peticion = lista();
      expect(peticion.request.params.get('page')).toBe('0');
      expect(peticion.request.params.get('size')).toBe('10');
      peticion.flush(pagina([]));
    });

    it('mientras carga lo dice', () => {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
      );
      fixture = TestBed.createComponent(MisSolicitudes);
      fixture.detectChanges();

      expect(texto()).toContain('Cargando tus solicitudes…');
      lista().flush(pagina([]));
    });

    it('editar los datos personales no vuelve a pedir la lista', () => {
      crear([]);

      // No hay petición nueva: http.verify() del afterEach la detectaría.
      expect(texto()).toContain('Todavía no has enviado solicitudes');
    });
  });

  describe('cada solicitud', () => {
    it('muestra la oferta, la fecha con su hora, las personas y una referencia corta', () => {
      crear([solicitud(1, 'PENDIENTE')]);

      expect(raiz().querySelector('h2 a')?.textContent).toBe('Oferta 1');
      expect(texto()).toContain('28 oct 2026, 07:00');
      expect(texto()).toContain('2 personas');
      expect(texto()).toContain('Ref. 6F1C2A9E');
    });

    it('el nombre de la oferta lleva al detalle de la solicitud', () => {
      crear([solicitud(1, 'PENDIENTE')]);

      expect(raiz().querySelector('h2 a')?.getAttribute('href')).toBe(
        '/turista/solicitudes/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a21',
      );
    });

    it('una persona, en singular', () => {
      crear([solicitud(1, 'PENDIENTE', { personas: 1 })]);

      expect(texto()).toContain('1 persona');
      expect(texto()).not.toContain('1 personas');
    });

    it('un alojamiento muestra la salida y las unidades en lugar de la hora', () => {
      crear([
        solicitud(2, 'PENDIENTE', {
          oferta: { id: 'b2', tipo: 'ALOJAMIENTO', nombre: 'Cabañas', zonaId: 'z1' },
          horaSalida: undefined,
          fechaSalida: '2026-10-30',
          numeroUnidades: 2,
          personas: 3,
        }),
      ]);

      expect(texto()).toContain('salida 30 oct 2026');
      expect(texto()).toContain('2 unidades');
      expect(texto()).not.toContain(', 07:00');
    });

    it('un restaurante muestra la hora de la reserva', () => {
      crear([
        solicitud(3, 'PENDIENTE', {
          oferta: { id: 'b3', tipo: 'RESTAURANTE', nombre: 'La Terraza', zonaId: 'z1' },
          horaSalida: undefined,
          horaReserva: '19:30',
        }),
      ]);

      expect(texto()).toContain('28 oct 2026, 19:30');
    });
  });

  describe('los 9 estados', () => {
    const todas = () => ESTADOS_SOLICITUD.map((estado, i) => solicitud(i + 1, estado));

    it('cada uno tiene su propia etiqueta, con su color', () => {
      crear(todas());

      const etiquetas = tarjetas().map((t) => t.querySelector('.etiqueta')?.textContent?.trim());
      expect(etiquetas).toEqual([
        'Pendiente',
        'Disponible, confirma',
        'Por pagar',
        'No disponible',
        'Sin respuesta',
        'Declinada',
        'Pago fallido',
        'Pagada',
        'Vencida',
      ]);
      expect(new Set(etiquetas).size).toBe(9);
    });

    it('explica cada estado con una frase', () => {
      crear(todas());

      expect(texto()).toContain(
        'Hay cupo. Revisa las condiciones y confirma o declina antes del plazo.',
      );
      expect(texto()).toContain('El prestador no respondió a tiempo.');
      expect(texto()).toContain('Pasó el plazo para confirmar o pagar.');
    });

    it('en inglés, con las etiquetas del contrato', () => {
      crear(todas());

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();

      expect(tarjetas().map((t) => t.querySelector('.etiqueta')?.textContent?.trim())).toEqual([
        'Pending',
        'Available, confirm',
        'Awaiting payment',
        'Unavailable',
        'No response',
        'Declined',
        'Payment failed',
        'Paid',
        'Expired',
      ]);
    });
  });

  describe('qué puede hacer con cada una', () => {
    const accion = (indice: number) => {
      const lado = tarjetas()[indice].querySelector<HTMLAnchorElement>('.solicitud__lado a')!;
      return [lado.textContent?.trim(), lado.getAttribute('href')];
    };

    it('atendida: ver las condiciones, en el detalle', () => {
      crear([solicitud(1, 'ATENDIDA')]);

      expect(accion(0)).toEqual([
        'Ver condiciones',
        '/turista/solicitudes/6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a21',
      ]);
    });

    it.each(['NO_DISPONIBLE', 'SIN_RESPUESTA', 'VENCIDA'] as const)(
      '%s: volver a solicitar la misma oferta',
      (estado) => {
        crear([solicitud(1, estado)]);

        expect(accion(0)).toEqual([
          'Volver a solicitar',
          '/turista/solicitar/b00000000000000000000001',
        ]);
      },
    );

    it('pagada: dejar su opinión, en la ficha de la oferta', () => {
      crear([solicitud(1, 'PAGADA')]);

      expect(accion(0)).toEqual(['Dejar opinión', '/ofertas/b00000000000000000000001']);
    });

    it.each(['PENDIENTE', 'DECLINADA'] as const)('%s: ver la oferta', (estado) => {
      crear([solicitud(1, estado)]);

      expect(accion(0)).toEqual(['Ver oferta', '/ofertas/b00000000000000000000001']);
    });

    it('no hay botón de pagar: lo agrega la rama de pagos', () => {
      crear([solicitud(1, 'CONFIRMADA'), solicitud(2, 'PAGO_FALLIDO')]);

      // Ningún botón ni enlace lleva a pagar (los estados sí dicen "Por pagar" o "Pago fallido").
      const controles = Array.from(raiz().querySelectorAll('a, button')).map(
        (e) => e.textContent?.toLowerCase() ?? '',
      );
      expect(controles.some((texto) => texto.includes('pagar') || texto.includes('pago'))).toBe(
        false,
      );
      expect(raiz().querySelector('a[href*="pago"]')).toBeNull();
      expect(accion(0)[0]).toBe('Ver oferta');
      expect(accion(1)[0]).toBe('Ver oferta');
    });
  });

  describe('filtros', () => {
    const todas = () => ESTADOS_SOLICITUD.map((estado, i) => solicitud(i + 1, estado));

    it('empieza en "Todas" con las nueve', () => {
      crear(todas());

      expect(filtro('Todas').getAttribute('aria-pressed')).toBe('true');
      expect(tarjetas()).toHaveLength(9);
    });

    it('"Activas" deja las cuatro que siguen su curso', () => {
      crear(todas());

      filtro('Activas').click();
      fixture.detectChanges();

      expect(filtro('Activas').getAttribute('aria-pressed')).toBe('true');
      expect(filtro('Todas').getAttribute('aria-pressed')).toBe('false');
      expect(tarjetas().map((t) => t.querySelector('.etiqueta')?.textContent?.trim())).toEqual([
        'Pendiente',
        'Disponible, confirma',
        'Por pagar',
        'Pago fallido',
      ]);
    });

    it('"Finalizadas" deja las cinco terminales', () => {
      crear(todas());

      filtro('Finalizadas').click();
      fixture.detectChanges();

      expect(tarjetas()).toHaveLength(5);
    });

    it('filtrar no pide nada al servidor', () => {
      crear(todas());

      filtro('Activas').click();
      fixture.detectChanges();

      http.expectNone((p) => p.url === URL);
    });

    it('un filtro sin resultados lo dice y avisa si hay más páginas', () => {
      crear([solicitud(1, 'PENDIENTE')], { totalPaginas: 2, totalElementos: 11 });

      filtro('Finalizadas').click();
      fixture.detectChanges();

      expect(texto()).toContain('No tienes solicitudes en este filtro.');
      expect(texto()).toContain('Hay más solicitudes en otras páginas.');
    });

    it('el grupo de filtros tiene nombre', () => {
      crear([]);

      expect(raiz().querySelector('.filtros')?.getAttribute('aria-label')).toBe(
        'Filtrar solicitudes',
      );
    });
  });

  describe('sin solicitudes', () => {
    it('invita a elegir una oferta del catálogo', () => {
      crear([]);

      expect(texto()).toContain('Todavía no has enviado solicitudes');
      expect(raiz().querySelector('.vacio a')?.getAttribute('href')).toBe('/');
    });
  });

  describe('páginas', () => {
    it('con más de una página aparece la paginación y pasar de página cambia la dirección', () => {
      crear([solicitud(1, 'PENDIENTE')], { totalPaginas: 3, totalElementos: 25 });

      expect(texto()).toContain('Página 1 de 3');
      Array.from(raiz().querySelectorAll<HTMLButtonElement>('app-paginacion button'))
        .find((b) => b.textContent?.includes('Siguiente'))!
        .click();

      expect(navegar).toHaveBeenCalledWith(
        [],
        expect.objectContaining({ queryParams: { page: 1 } }),
      );
    });
  });

  describe('plazos', () => {
    it('pendiente: cuánto tiene el operador para responder', () => {
      crear([solicitud(1, 'PENDIENTE', { venceEn: en(3 * HORA) })]);

      expect(texto()).toContain('El operador tiene 3 h 0 min para responder.');
    });

    it('atendida: cuánto queda para confirmar o declinar, en minutos cuando es poco', () => {
      crear([solicitud(1, 'ATENDIDA', { venceEn: en(45 * 60_000) })]);

      expect(texto()).toContain('Te quedan 45 min para confirmar o declinar.');
    });

    it('con más de un día, se dice en días y horas', () => {
      crear([solicitud(1, 'PENDIENTE', { venceEn: en(26 * HORA) })]);

      expect(texto()).toContain('El operador tiene 1 d 2 h para responder.');
    });

    it('una solicitud cerrada no tiene plazo', () => {
      crear([solicitud(1, 'PAGADA')]);

      expect(raiz().querySelector('.solicitud__plazo')).toBeNull();
    });

    it('el plazo se actualiza solo con el paso del tiempo', () => {
      crear([solicitud(1, 'ATENDIDA', { venceEn: en(45 * 60_000) })]);

      vi.advanceTimersByTime(30 * 60_000);
      fixture.detectChanges();

      expect(texto()).toContain('Te quedan 15 min para confirmar o declinar.');
    });

    it('cuando un plazo termina consulta el estado real, una sola vez', () => {
      crear([solicitud(1, 'ATENDIDA', { venceEn: en(-HORA) })]);

      // Al abrir ya había vencido: pregunta de nuevo por su estado.
      lista().flush(pagina([solicitud(1, 'ATENDIDA', { venceEn: en(-HORA) })]));
      fixture.detectChanges();
      expect(texto()).toContain('El plazo terminó. Estamos actualizando el estado.');

      // Si el núcleo todavía no la cerró, no insiste en bucle.
      vi.advanceTimersByTime(60_000);
      fixture.detectChanges();
      http.expectNone((p) => p.url === URL);
    });

    it('un plazo que vence con la pantalla abierta dispara la consulta', () => {
      crear([solicitud(1, 'PENDIENTE', { venceEn: en(10 * 60_000) })]);

      vi.advanceTimersByTime(11 * 60_000);
      fixture.detectChanges();

      lista().flush(pagina([solicitud(1, 'SIN_RESPUESTA')]));
      fixture.detectChanges();
      expect(tarjetas()[0].querySelector('.etiqueta')?.textContent?.trim()).toBe('Sin respuesta');
    });
  });

  describe('reconsulta (RF-33)', () => {
    it('al volver a la pestaña pregunta el estado real', () => {
      crear([solicitud(1, 'PENDIENTE', { venceEn: en(5 * HORA) })]);

      document.dispatchEvent(new Event('visibilitychange'));

      lista().flush(pagina([solicitud(1, 'ATENDIDA', { venceEn: en(8 * HORA) })]));
      fixture.detectChanges();
      expect(tarjetas()[0].querySelector('.etiqueta')?.textContent?.trim()).toBe(
        'Disponible, confirma',
      );
    });

    it('al reconectarse pregunta de nuevo', () => {
      crear([solicitud(1, 'PENDIENTE', { venceEn: en(5 * HORA) })]);

      window.dispatchEvent(new Event('online'));

      lista().flush(pagina([solicitud(1, 'PENDIENTE', { venceEn: en(5 * HORA) })]));
    });

    it('mientras reconsulta no borra lo que ya se veía', () => {
      crear([solicitud(1, 'PENDIENTE', { venceEn: en(5 * HORA) })]);

      window.dispatchEvent(new Event('online'));
      fixture.detectChanges();

      expect(tarjetas()).toHaveLength(1);
      lista().flush(pagina([solicitud(1, 'PENDIENTE', { venceEn: en(5 * HORA) })]));
    });
  });

  describe('errores', () => {
    it('si la consulta falla lo dice', () => {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
      );
      fixture = TestBed.createComponent(MisSolicitudes);
      fixture.detectChanges();

      lista().error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos conectar con el servidor',
      );
    });

    it('si una reconsulta falla, conserva la lista y avisa; y el aviso cambia de idioma', () => {
      crear([solicitud(1, 'PENDIENTE', { venceEn: en(5 * HORA) })]);

      window.dispatchEvent(new Event('online'));
      lista().error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(tarjetas()).toHaveLength(1);
      expect(texto()).toContain('No pudimos conectar con el servidor');

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();
      expect(texto()).toContain("We couldn't connect to the server");
    });
  });

  describe('inglés', () => {
    it('la pantalla, los filtros, las fechas y las acciones cambian al instante', () => {
      crear([solicitud(1, 'ATENDIDA', { venceEn: en(3 * HORA) }), solicitud(2, 'NO_DISPONIBLE')]);

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();

      expect(raiz().querySelector('h1')?.textContent).toContain('My requests');
      expect(filtro('Active')).not.toBeUndefined();
      expect(filtro('Finished')).not.toBeUndefined();
      expect(texto()).toContain('Oct 28, 2026, 07:00');
      expect(texto()).toContain('2 people');
      expect(texto()).toContain('You have 3 h 0 min left to confirm or decline.');
      expect(texto()).toContain('View terms');
      expect(texto()).toContain('Request again');
    });
  });
});
