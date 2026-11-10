import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SolicitudOperador } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { AtenderSolicitud } from './atender-solicitud';

const ID = '6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20';
const SOLICITUD_URL = `/api/nucleo/operador/solicitudes/${ID}`;
const ATENCION_URL = `${SOLICITUD_URL}/atencion`;
const AHORA = Date.parse('2026-10-26T15:00:00Z');
const HORA = 3_600_000;
const en = (ms: number) => new Date(AHORA + ms).toISOString();

const ZONA = { id: 'z1', nombre: 'Taganga', altaAfluencia: false, areaProtegida: false, buenasPracticas: [] };
const FICHA_ACTIVIDAD = {
  id: 'b00000000000000000000001',
  tipo: 'ACTIVIDAD',
  nombre: 'Caminata a Cabo San Juan del Guía',
  descripcion: 'Una caminata.',
  precioReferencia: 90000,
  unidadPrecio: 'por persona',
  capacidadMaxima: 10,
  accesibleSillaRuedas: false,
  fotos: [],
  zona: ZONA,
  actividad: { tipoActividad: 'SENDERISMO', nivelEsfuerzo: 'BAJO', horariosSalida: ['07:00:00'] },
};
const FICHA_ALOJAMIENTO = {
  ...FICHA_ACTIVIDAD,
  id: 'b00000000000000000000002',
  tipo: 'ALOJAMIENTO',
  nombre: 'Cabañas del mar',
  precioReferencia: 200000,
  unidadPrecio: 'por noche y unidad',
  actividad: undefined,
  alojamiento: { tipoUnidad: 'cabaña' },
};

const PENDIENTE: SolicitudOperador = {
  id: ID,
  oferta: {
    id: 'b00000000000000000000001',
    tipo: 'ACTIVIDAD',
    nombre: 'Caminata a Cabo San Juan del Guía',
    zonaId: 'z1',
  },
  prestadorId: '10000000-0000-0000-0000-000000000001',
  whatsappPrestador: '+573001110101',
  fechaInicio: '2026-10-28',
  horaSalida: '07:00:00',
  personas: 2,
  nota: 'Vamos con una niña de 8 años',
  estado: 'PENDIENTE',
  venceEn: en(20 * HORA),
  proximaAVencer: false,
  creadaEn: en(-4 * HORA),
  actualizadaEn: en(-4 * HORA),
  historial: [{ a: 'PENDIENTE', en: en(-4 * HORA), actor: 'TURISTA' }],
};

const ATENDIDA: SolicitudOperador = {
  ...PENDIENTE,
  estado: 'ATENDIDA',
  condiciones: {
    precioBase: 300000,
    politicaCancelacion: 'Sin reembolso a menos de 24 horas del servicio',
    nota: 'Incluye el ingreso al parque',
    aparteHasta: en(3 * HORA),
  },
  historial: [
    { a: 'PENDIENTE', en: en(-4 * HORA), actor: 'TURISTA' },
    { de: 'PENDIENTE', a: 'ATENDIDA', en: en(-HORA), actor: 'OPERADOR' },
  ],
};

describe('AtenderSolicitud (P02.5)', () => {
  let fixture: ComponentFixture<AtenderSolicitud>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#${id}`)!;
  const boton = () => raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!;

  function escribir(id: string, valor: string): void {
    const entrada = campo(id);
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function elegir(valor: 'ATENDIDA' | 'NO_DISPONIBLE'): void {
    const radio = raiz().querySelector<HTMLInputElement>(`input[type="radio"][value="${valor}"]`)!;
    radio.click();
    fixture.detectChanges();
  }

  function marcarAparte(): void {
    campo('aparteAcordado').click();
    fixture.detectChanges();
  }

  /** Abre la pantalla y responde la solicitud; si queda pendiente, también la ficha de la oferta en el catálogo. */
  function crear(
    solicitud: SolicitudOperador = PENDIENTE,
    ficha: object | 'falla' = FICHA_ACTIVIDAD,
  ): void {
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'OPERADOR_DISPONIBILIDAD' }))),
    );
    fixture = TestBed.createComponent(AtenderSolicitud);
    fixture.componentRef.setInput('id', ID);
    fixture.detectChanges();
    http.expectOne(SOLICITUD_URL).flush(solicitud);
    fixture.detectChanges();
    if (solicitud.estado === 'PENDIENTE') {
      const peticion = http.expectOne(`/api/catalogo/ofertas/${solicitud.oferta.id}`);
      if (ficha === 'falla') {
        peticion.error(new ProgressEvent('error'));
      } else {
        peticion.flush(ficha);
      }
      fixture.detectChanges();
    }
  }

  /** Llena lo mínimo para enviar "hay cupo". */
  function llenarConCupo(): void {
    escribir('precioTotal', '300000');
    escribir('politicaCancelacion', 'Sin reembolso a menos de 24 horas del servicio');
    marcarAparte();
  }

  beforeEach(() => {
    vi.useFakeTimers({ now: AHORA, toFake: ['Date', 'setInterval', 'clearInterval'] });
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
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

  describe('la solicitud', () => {
    it('muestra la oferta, la fecha, la hora, las personas y los comentarios del turista', () => {
      crear();

      expect(texto()).toContain('Caminata a Cabo San Juan del Guía');
      expect(texto()).toContain('28 de octubre de 2026');
      expect(texto()).toContain('07:00');
      expect(texto()).toContain('Vamos con una niña de 8 años');
      expect(texto()).toContain('6F1C2A9E');
    });

    it('no muestra datos personales del turista: solo lo que el operador necesita con el prestador', () => {
      crear();

      const etiquetas = Array.from(raiz().querySelectorAll('dt')).map((dt) => dt.textContent?.trim());
      expect(etiquetas).not.toContain('Turista');
      expect(etiquetas).not.toContain('Nombre');
      expect(etiquetas).not.toContain('WhatsApp del turista');
      expect(raiz().textContent).not.toMatch(/@demo\.samariantour/);
    });

    it('dice cuánto queda para atenderla', () => {
      crear();

      expect(texto()).toContain('Tienes 20 h 0 min para atenderla.');
    });

    it('avisa si el plazo ya terminó', () => {
      crear({ ...PENDIENTE, venceEn: en(-HORA) });

      expect(texto()).toContain('El plazo para atenderla terminó');
    });

    it('si no existe o es de otro operador, lo dice', () => {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'OPERADOR_DISPONIBILIDAD' }))),
      );
      fixture = TestBed.createComponent(AtenderSolicitud);
      fixture.componentRef.setInput('id', ID);
      fixture.detectChanges();
      http
        .expectOne(SOLICITUD_URL)
        .flush({ codigo: 'SOLICITUD_NO_ENCONTRADA' }, { status: 404, statusText: 'Not Found' });
      fixture.detectChanges();

      expect(texto()).toContain('No encontramos esta solicitud');
      expect(raiz().querySelector('form')).toBeNull();
    });
  });

  describe('contacto con el prestador', () => {
    it('ofrece escribirle por WhatsApp en otra pestaña, con el número solo en dígitos', () => {
      crear();

      const enlace = raiz().querySelector<HTMLAnchorElement>('a[href^="https://wa.me/"]')!;
      expect(enlace.getAttribute('href')).toBe('https://wa.me/573001110101');
      expect(enlace.getAttribute('target')).toBe('_blank');
      expect(enlace.getAttribute('rel')).toContain('noopener');
      expect(enlace.textContent).toContain('(se abre en otra pestaña)');
    });

    it('si el prestador no tiene WhatsApp registrado, lo avisa y no ofrece el enlace', () => {
      crear({ ...PENDIENTE, whatsappPrestador: undefined });

      expect(raiz().querySelector('a[href^="https://wa.me/"]')).toBeNull();
      expect(texto()).toContain('no tiene un WhatsApp registrado');
    });
  });

  describe('referencia junto al precio', () => {
    it('muestra lo que pidieron y el precio de la oferta en el catálogo, solo como referencia', () => {
      crear();

      const referencia = raiz().querySelector('.referencia')!.textContent!.replace(/[  ]/g, ' ');
      expect(referencia).toContain('Pidieron: 2 personas');
      expect(referencia).toContain('Precio en el catálogo: $ 90.000 por persona');
      expect(referencia).toContain('Cálculo de referencia: $ 180.000');
      expect(referencia).toContain('el total lo escribes tú');
    });

    it('no rellena el precio total: lo escribe el operador', () => {
      crear();

      expect(campo('precioTotal').value).toBe('');
    });

    it('en alojamiento muestra noches, unidades y personas', () => {
      crear(
        {
          ...PENDIENTE,
          oferta: { id: 'b00000000000000000000002', tipo: 'ALOJAMIENTO', nombre: 'Cabañas del mar', zonaId: 'z1' },
          horaSalida: undefined,
          fechaInicio: '2026-10-28',
          fechaSalida: '2026-10-31',
          numeroUnidades: 2,
          personas: 4,
        },
        FICHA_ALOJAMIENTO,
      );

      const referencia = raiz().querySelector('.referencia')!.textContent!.replace(/[  ]/g, ' ');
      expect(referencia).toContain('Pidieron: 3 noches · 2 unidades · 4 personas');
      expect(referencia).toContain('Precio en el catálogo: $ 200.000 por noche y unidad');
      // 200.000 × 3 noches × 2 unidades
      expect(referencia).toContain('Cálculo de referencia: $ 1.200.000');
    });

    it('si el catálogo no responde lo avisa y se puede seguir atendiendo', () => {
      crear(PENDIENTE, 'falla');

      expect(texto()).toContain('No pudimos consultar el precio de referencia');
      expect(raiz().querySelector('.referencia')!.textContent).toContain('Pidieron: 2 personas');
      expect(campo('precioTotal')).not.toBeNull();
    });
  });

  describe('validación antes de enviar', () => {
    it('con el formulario vacío no envía y señala el precio, la política y el aparte', () => {
      crear();

      boton().click();
      fixture.detectChanges();

      http.expectNone(ATENCION_URL);
      expect(texto()).toContain('Escribe el precio total.');
      expect(texto()).toContain('Escribe la política de cancelación.');
      expect(texto()).toContain('Confirma con el prestador que el cupo queda apartado');
      expect(document.activeElement).toBe(campo('precioTotal')) ;
    });

    it('la casilla del aparte dice exactamente lo que debe decir (RF-38)', () => {
      crear();

      const etiqueta = raiz().querySelector<HTMLLabelElement>('label[for="aparteAcordado"]')!;
      expect(etiqueta.textContent?.trim()).toBe(
        'Confirmé con el prestador que el cupo queda apartado durante el plazo',
      );
    });

    it('sin marcar el aparte no envía aunque lo demás esté bien', () => {
      crear();
      escribir('precioTotal', '300000');
      escribir('politicaCancelacion', 'Sin reembolso');

      boton().click();
      fixture.detectChanges();

      http.expectNone(ATENCION_URL);
      expect(campo('aparteAcordado').getAttribute('aria-invalid')).toBe('true');
    });

    it('un precio con decimales o menor que 1 se rechaza', () => {
      crear();
      llenarConCupo();

      escribir('precioTotal', '0');
      boton().click();
      fixture.detectChanges();
      expect(texto()).toContain('El precio debe ser mayor que cero.');

      escribir('precioTotal', '1500.5');
      boton().click();
      fixture.detectChanges();
      expect(texto()).toContain('número entero');
      http.expectNone(ATENCION_URL);
    });
  });

  describe('enviar el resultado', () => {
    it('hay cupo manda el precio total, la política, el aparte y la nota (RF-37 a RF-39)', () => {
      const avisos = TestBed.inject(AvisosService);
      const mostrar = vi.spyOn(avisos, 'mostrar');
      crear();
      llenarConCupo();
      escribir('nota', 'Incluye el ingreso al parque');

      boton().click();
      fixture.detectChanges();

      const peticion = http.expectOne(ATENCION_URL);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({
        resultado: 'ATENDIDA',
        precioBase: 300000,
        politicaCancelacion: 'Sin reembolso a menos de 24 horas del servicio',
        aparteAcordado: true,
        nota: 'Incluye el ingreso al parque',
      });
      peticion.flush({ ...ATENDIDA });

      expect(mostrar).toHaveBeenCalledWith(expect.stringContaining('Enviaste las condiciones'));
      expect(navegar).toHaveBeenCalledWith(['/operador']);
    });

    it('no hay cupo oculta las condiciones y manda solo el resultado y el motivo (RF-40)', () => {
      const avisos = TestBed.inject(AvisosService);
      const mostrar = vi.spyOn(avisos, 'mostrar');
      crear();
      llenarConCupo();
      elegir('NO_DISPONIBLE');

      expect(campo('precioTotal')).toBeNull();
      expect(campo('aparteAcordado')).toBeNull();
      expect(raiz().querySelector('label[for="nota"]')?.textContent).toContain('Motivo');

      escribir('nota', 'Sin cupo esa semana');
      boton().click();
      fixture.detectChanges();

      const peticion = http.expectOne(ATENCION_URL);
      expect(peticion.request.body).toEqual({ resultado: 'NO_DISPONIBLE', nota: 'Sin cupo esa semana' });
      peticion.flush({ ...PENDIENTE, estado: 'NO_DISPONIBLE' });

      expect(mostrar).toHaveBeenCalledWith(expect.stringContaining('no hay cupo'));
      expect(navegar).toHaveBeenCalledWith(['/operador']);
    });

    it('no hay cupo sin motivo se puede enviar y no manda nota', () => {
      crear();
      elegir('NO_DISPONIBLE');

      boton().click();
      fixture.detectChanges();

      const peticion = http.expectOne(ATENCION_URL);
      expect(peticion.request.body).toEqual({ resultado: 'NO_DISPONIBLE' });
      peticion.flush({ ...PENDIENTE, estado: 'NO_DISPONIBLE' });
    });

    it('un doble clic envía una sola vez', () => {
      crear();
      llenarConCupo();

      boton().click();
      boton().click();
      fixture.detectChanges();

      const peticiones = http.match(ATENCION_URL);
      expect(peticiones).toHaveLength(1);
      expect(boton().disabled).toBe(true);
      peticiones[0].flush({ ...ATENDIDA });
    });
  });

  describe('cuando el núcleo rechaza', () => {
    function enviarConCupo(): void {
      crear();
      llenarConCupo();
      boton().click();
      fixture.detectChanges();
    }

    it('SIN_TIEMPO_PARA_CONFIRMAR deja solo "no hay cupo" (RF-39)', () => {
      enviarConCupo();

      http
        .expectOne(ATENCION_URL)
        .flush(
          { codigo: 'SIN_TIEMPO_PARA_CONFIRMAR' },
          { status: 409, statusText: 'Conflict' },
        );
      fixture.detectChanges();

      expect(texto()).toContain('No alcanza el tiempo para confirmar');
      const conCupo = raiz().querySelector<HTMLInputElement>('input[value="ATENDIDA"]')!;
      const sinCupo = raiz().querySelector<HTMLInputElement>('input[value="NO_DISPONIBLE"]')!;
      expect(conCupo.disabled).toBe(true);
      expect(sinCupo.checked).toBe(true);
      expect(campo('precioTotal')).toBeNull();
      expect(navegar).not.toHaveBeenCalled();
    });

    it('TRANSICION_INVALIDA vuelve a consultar y muestra el estado real, ya sin formulario', () => {
      enviarConCupo();

      http
        .expectOne(ATENCION_URL)
        .flush({ codigo: 'TRANSICION_INVALIDA' }, { status: 409, statusText: 'Conflict' });
      http.expectOne(SOLICITUD_URL).flush({ ...PENDIENTE, estado: 'VENCIDA', venceEn: undefined });
      fixture.detectChanges();

      expect(texto()).toContain('cambió de estado mientras la atendías');
      expect(raiz().querySelector('form')).toBeNull();
      expect(texto()).toContain('Esta solicitud ya fue tramitada');
    });

    it('PLAZO_VENCIDO también vuelve a consultar', () => {
      enviarConCupo();

      http
        .expectOne(ATENCION_URL)
        .flush({ codigo: 'PLAZO_VENCIDO' }, { status: 409, statusText: 'Conflict' });
      http.expectOne(SOLICITUD_URL).flush({ ...PENDIENTE, estado: 'VENCIDA', venceEn: undefined });
      fixture.detectChanges();

      expect(texto()).toContain('El plazo para esta acción ya terminó.');
      expect(raiz().querySelector('form')).toBeNull();
    });

    it('un dato inválido que señala el precio se muestra en ese campo', () => {
      enviarConCupo();

      http.expectOne(ATENCION_URL).flush(
        { codigo: 'DATO_INVALIDO', detail: 'precioBase: debe ser mayor que cero' },
        { status: 400, statusText: 'Bad Request' },
      );
      fixture.detectChanges();

      expect(raiz().querySelector('#precioTotal-error')?.textContent).toContain(
        'Debe ser mayor que cero',
      );
      expect(document.activeElement).toBe(campo('precioTotal'));
    });

    it('un error cualquiera se avisa y deja reintentar', () => {
      enviarConCupo();

      http.expectOne(ATENCION_URL).error(new ProgressEvent('error'));
      fixture.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
      expect(boton().disabled).toBe(false);
    });
  });

  describe('una solicitud que ya no está pendiente', () => {
    it('no ofrece el formulario y muestra lo que se registró (una vez atendida, sus datos no cambian)', () => {
      crear(ATENDIDA);

      expect(raiz().querySelector('form')).toBeNull();
      expect(texto()).toContain('Esta solicitud ya fue tramitada');
      expect(texto()).toContain('$ 300.000');
      expect(texto()).toContain('Sin reembolso a menos de 24 horas del servicio');
      expect(texto()).toContain('Incluye el ingreso al parque');
      expect(texto()).toContain('Cupo apartado hasta');
    });

    it('muestra el historial de la más antigua a la más reciente, con quién hizo cada cambio', () => {
      crear(ATENDIDA);

      const pasos = Array.from(raiz().querySelectorAll('.historial li')).map((li) =>
        li.textContent!.replace(/\s+/g, ' ').trim(),
      );
      expect(pasos).toHaveLength(2);
      expect(pasos[0]).toContain('Turista');
      expect(pasos[1]).toContain('Operador');
    });

    it('no pide el precio de la oferta al catálogo', () => {
      crear(ATENDIDA);

      http.expectNone((p) => p.url.startsWith('/api/catalogo'));
    });
  });

  describe('en inglés', () => {
    it('muestra la pantalla y la casilla del aparte en inglés', () => {
      establecerIdioma('en', { recordar: false });
      crear();

      expect(texto()).toContain('Handle request');
      expect(texto()).toContain('Total price (COP)');
      expect(texto()).toContain('Catalog price: COP 90,000 per person');
      expect(raiz().querySelector('label[for="aparteAcordado"]')?.textContent?.trim()).toBe(
        'I confirmed with the provider that the spot is held during the deadline',
      );
    });
  });
});
