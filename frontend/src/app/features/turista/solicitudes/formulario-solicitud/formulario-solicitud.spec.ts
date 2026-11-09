import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AvisosService } from '../../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../../core/sesion/testing';
import { FormularioSolicitud } from './formulario-solicitud';

const SOLICITUDES = '/api/nucleo/turista/solicitudes';
const ZONA = {
  id: 'z1',
  nombre: 'Taganga',
  altaAfluencia: false,
  areaProtegida: false,
  buenasPracticas: [],
};

const BASE = {
  descripcion: 'Una oferta.',
  precioReferencia: 70000,
  unidadPrecio: 'por persona',
  capacidadMaxima: 10,
  accesibleSillaRuedas: false,
  fotos: [],
  zona: ZONA,
};

const ACTIVIDAD = {
  ...BASE,
  id: 'b1',
  tipo: 'ACTIVIDAD',
  nombre: 'Caminata a Marinka',
  actividad: {
    tipoActividad: 'SENDERISMO',
    nivelEsfuerzo: 'BAJO',
    horariosSalida: ['07:00:00', '14:00:00'],
  },
};
const ACTIVIDAD_UNA_HORA = {
  ...ACTIVIDAD,
  actividad: { ...ACTIVIDAD.actividad, horariosSalida: ['07:00'] },
};
const ALOJAMIENTO = {
  ...BASE,
  id: 'b2',
  tipo: 'ALOJAMIENTO',
  nombre: 'Cabañas del mar',
  precioReferencia: 200000,
  unidadPrecio: 'por noche y unidad',
  alojamiento: { tipoUnidad: 'cabaña' },
};
const RESTAURANTE = {
  ...BASE,
  id: 'b3',
  tipo: 'RESTAURANTE',
  nombre: 'La Terraza',
  precioReferencia: 50000,
  unidadPrecio: 'promedio por persona',
  horarios: 'Todos los días, 12:00 a 22:00',
};

describe('FormularioSolicitud (P02.1)', () => {
  let fixture: ComponentFixture<FormularioSolicitud>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const campo = (id: string) => raiz().querySelector<HTMLInputElement>(`#${id}`)!;
  const boton = () => raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!;

  function escribir(id: string, valor: string): void {
    const entrada = campo(id);
    entrada.value = valor;
    entrada.dispatchEvent(new Event(entrada.tagName === 'SELECT' ? 'change' : 'input'));
    fixture.detectChanges();
  }

  function enviar(): void {
    boton().click();
    fixture.detectChanges();
  }

  /** Abre el formulario de una oferta con la sesión de un turista y responde la ficha que pide al abrirse. */
  function crear(oferta: object, whatsappVerificado = true): void {
    const sesion = respuestaDeSesion({ rol: 'TURISTA' });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(
        aSesionGuardada({ ...sesion, cuenta: { ...sesion.cuenta, whatsappVerificado } }),
      ),
    );
    fixture = TestBed.createComponent(FormularioSolicitud);
    fixture.componentRef.setInput('ofertaId', (oferta as { id: string }).id);
    fixture.detectChanges();
    http.expectOne(`/api/catalogo/ofertas/${(oferta as { id: string }).id}`).flush(oferta);
    fixture.detectChanges();
  }

  const rechazar = (estado: number, cuerpo: object) =>
    http.expectOne(SOLICITUDES).flush(cuerpo, { status: estado, statusText: 'Error' });

  beforeEach(() => {
    // Hoy es el 26 de octubre de 2026 a las 10:00 en Colombia: la primera fecha posible es el 28.
    vi.useFakeTimers({ now: new Date('2026-10-26T15:00:00Z'), toFake: ['Date'] });
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
    // La pantalla de detalle llega en otro commit: aquí solo se comprueba a dónde se manda.
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  describe('qué pide cada tipo de oferta', () => {
    it('actividad: fecha, hora de salida, personas y nota', () => {
      crear(ACTIVIDAD);

      expect(raiz().querySelector('h1')?.textContent).toContain('Solicita disponibilidad');
      expect(campo('fechaInicio').type).toBe('date');
      expect(campo('horaSalida').tagName).toBe('SELECT');
      expect(campo('personas')).not.toBeNull();
      expect(campo('nota')).not.toBeNull();
      expect(campo('fechaSalida')).toBeNull();
      expect(campo('horaReserva')).toBeNull();
      expect(campo('numeroUnidades')).toBeNull();
    });

    it('las horas de salida son las de la oferta, sin segundos', () => {
      crear(ACTIVIDAD);

      const opciones = Array.from(campo('horaSalida').querySelectorAll('option')).map(
        (o) => o.value,
      );
      expect(opciones).toEqual(['', '07:00', '14:00']);
    });

    it('alojamiento: entrada, salida, unidades y huéspedes', () => {
      crear(ALOJAMIENTO);

      expect(texto()).toContain('Fecha de entrada');
      expect(texto()).toContain('Fecha de salida');
      expect(texto()).toContain('Huéspedes');
      expect(campo('fechaSalida')).not.toBeNull();
      expect(campo('numeroUnidades')).not.toBeNull();
      expect(campo('horaSalida')).toBeNull();
    });

    it('restaurante: hora de la reserva, con el horario del restaurante de ayuda', () => {
      crear(RESTAURANTE);

      expect(campo('horaReserva').type).toBe('time');
      expect(texto()).toContain('Horario del restaurante: Todos los días, 12:00 a 22:00');
      expect(campo('horaSalida')).toBeNull();
    });

    it('no pide nombre, teléfono ni correo: salen de la cuenta', () => {
      crear(ACTIVIDAD);

      expect(campo('nombre')).toBeNull();
      expect(campo('telefono')).toBeNull();
      expect(raiz().querySelector('input[type="email"]')).toBeNull();
    });
  });

  describe('en menos de 2 minutos (QAS-08)', () => {
    it('con una sola hora de salida y las personas en 1, solo hay que elegir la fecha', () => {
      crear(ACTIVIDAD_UNA_HORA);
      expect(campo('horaSalida').value).toBe('07:00');
      expect(campo('personas').value).toBe('1');

      escribir('fechaInicio', '2026-10-28');
      enviar();

      const peticion = http.expectOne(SOLICITUDES);
      expect(peticion.request.body).toEqual({
        ofertaId: 'b1',
        fechaInicio: '2026-10-28',
        horaSalida: '07:00',
        personas: 1,
      });
      peticion.flush({ id: 's1', estado: 'PENDIENTE' }, { status: 201, statusText: 'Created' });
    });

    it('el formulario empieza con el foco listo: los errores llevan al primer campo que falta', () => {
      crear(ACTIVIDAD);

      enviar();

      expect(document.activeElement).toBe(campo('fechaInicio'));
    });
  });

  describe('validaciones', () => {
    it('sin llenar nada no envía y marca lo que falta', () => {
      crear(ACTIVIDAD);

      enviar();

      http.expectNone(SOLICITUDES);
      expect(texto()).toContain('Elige una fecha.');
      expect(texto()).toContain('Elige una hora.');
      expect(campo('fechaInicio').getAttribute('aria-invalid')).toBe('true');
    });

    it('los errores no aparecen antes de tocar el campo ni de enviar', () => {
      crear(ACTIVIDAD);

      expect(texto()).not.toContain('Elige una fecha.');
    });

    it('la fecha mínima es dos días después de hoy en hora de Colombia', () => {
      crear(ACTIVIDAD);

      expect(campo('fechaInicio').getAttribute('min')).toBe('2026-10-28');
      expect(texto()).toContain('desde el 28 de octubre de 2026');
    });

    it('una fecha demasiado cercana se rechaza con su motivo', () => {
      crear(ACTIVIDAD_UNA_HORA);
      escribir('fechaInicio', '2026-10-27');

      enviar();

      http.expectNone(SOLICITUDES);
      expect(texto()).toContain('La fecha debe ser desde el 28 de octubre de 2026');
    });

    it('más personas que la capacidad de la oferta se rechaza', () => {
      crear(ACTIVIDAD_UNA_HORA);
      escribir('fechaInicio', '2026-10-28');
      escribir('personas', '11');

      enviar();

      http.expectNone(SOLICITUDES);
      expect(texto()).toContain('Esta oferta admite hasta 10 personas.');
    });

    it('en alojamiento la salida debe ser después de la entrada', () => {
      crear(ALOJAMIENTO);
      escribir('fechaInicio', '2026-10-30');
      escribir('fechaSalida', '2026-10-30');
      escribir('numeroUnidades', '1');

      enviar();

      http.expectNone(SOLICITUDES);
      expect(texto()).toContain('La salida debe ser después de la entrada.');
    });

    it('la nota no puede pasar de 500 caracteres', () => {
      crear(ACTIVIDAD_UNA_HORA);
      escribir('fechaInicio', '2026-10-28');
      escribir('nota', 'a'.repeat(501));

      enviar();

      http.expectNone(SOLICITUDES);
      expect(texto()).toContain('La nota puede tener hasta 500 caracteres.');
    });
  });

  describe('lo que se envía', () => {
    it('alojamiento con la fecha de salida y las unidades', () => {
      crear(ALOJAMIENTO);
      escribir('fechaInicio', '2026-10-28');
      escribir('fechaSalida', '2026-10-30');
      escribir('numeroUnidades', '1');
      escribir('personas', '3');

      enviar();

      const peticion = http.expectOne(SOLICITUDES);
      expect(peticion.request.body).toEqual({
        ofertaId: 'b2',
        fechaInicio: '2026-10-28',
        fechaSalida: '2026-10-30',
        numeroUnidades: 1,
        personas: 3,
      });
      peticion.flush({ id: 's2' }, { status: 201, statusText: 'Created' });
    });

    it('restaurante con la hora de la reserva y la nota', () => {
      crear(RESTAURANTE);
      escribir('fechaInicio', '2026-10-28');
      escribir('horaReserva', '19:30');
      escribir('personas', '4');
      escribir('nota', '  Mesa en la terraza  ');

      enviar();

      const peticion = http.expectOne(SOLICITUDES);
      expect(peticion.request.body).toEqual({
        ofertaId: 'b3',
        fechaInicio: '2026-10-28',
        horaReserva: '19:30',
        personas: 4,
        nota: 'Mesa en la terraza',
      });
      peticion.flush({ id: 's3' }, { status: 201, statusText: 'Created' });
    });

    it('lleva una Idempotency-Key', () => {
      crear(ACTIVIDAD_UNA_HORA);
      escribir('fechaInicio', '2026-10-28');

      enviar();

      const peticion = http.expectOne(SOLICITUDES);
      expect(peticion.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
      peticion.flush({ id: 's1' }, { status: 201, statusText: 'Created' });
    });

    it('mientras se envía, el botón no deja enviar otra vez', () => {
      crear(ACTIVIDAD_UNA_HORA);
      escribir('fechaInicio', '2026-10-28');

      enviar();

      expect(boton().disabled).toBe(true);
      expect(boton().textContent).toContain('Enviando…');
      http.expectOne(SOLICITUDES).flush({ id: 's1' }, { status: 201, statusText: 'Created' });
    });
  });

  describe('al terminar', () => {
    it('avisa y lleva al detalle de la solicitud nueva', () => {
      crear(ACTIVIDAD_UNA_HORA);
      escribir('fechaInicio', '2026-10-28');

      enviar();
      http
        .expectOne(SOLICITUDES)
        .flush({ id: 's1', estado: 'PENDIENTE' }, { status: 201, statusText: 'Created' });

      expect(navegar).toHaveBeenCalledWith(['/turista/solicitudes', 's1']);
      expect(TestBed.inject(AvisosService).avisos()[0].texto).toContain('Solicitud enviada');
    });
  });

  describe('idempotencia ante un fallo', () => {
    function claveDelEnvio(): string {
      return http.expectOne(SOLICITUDES).request.headers.get('Idempotency-Key')!;
    }

    it('reintentar lo mismo repite la clave; corregir un dato la cambia', () => {
      crear(ACTIVIDAD_UNA_HORA);
      escribir('fechaInicio', '2026-10-28');

      enviar();
      const primera = http.expectOne(SOLICITUDES);
      primera.error(new ProgressEvent('error'));
      fixture.detectChanges();
      expect(texto()).toContain('No pudimos conectar con el servidor');

      enviar();
      const reintento = http.expectOne(SOLICITUDES);
      expect(reintento.request.headers.get('Idempotency-Key')).toBe(
        primera.request.headers.get('Idempotency-Key'),
      );
      reintento.error(new ProgressEvent('error'));
      fixture.detectChanges();

      escribir('personas', '2');
      enviar();
      expect(claveDelEnvio()).not.toBe(primera.request.headers.get('Idempotency-Key'));
    });
  });

  describe('cuando el núcleo rechaza', () => {
    beforeEach(() => {
      crear(ACTIVIDAD_UNA_HORA);
      escribir('fechaInicio', '2026-10-28');
      enviar();
    });

    it('un dato inválido se marca en su campo', () => {
      rechazar(400, {
        status: 400,
        codigo: 'DATO_INVALIDO',
        detail: 'personas: supera la capacidad máxima',
      });
      fixture.detectChanges();

      expect(raiz().querySelector('#personas-error')?.textContent).toContain(
        'Supera la capacidad máxima',
      );
      expect(campo('personas').getAttribute('aria-invalid')).toBe('true');
      expect(document.activeElement).toBe(campo('personas'));
    });

    it('una solicitud repetida lo dice y ofrece ver mis solicitudes', () => {
      rechazar(409, { status: 409, codigo: 'SOLICITUD_DUPLICADA' });
      fixture.detectChanges();

      expect(texto()).toContain(
        'Ya tienes una solicitud abierta para esta oferta en esa fecha y hora.',
      );
      expect(raiz().querySelector('a[href="/turista/solicitudes"]')).not.toBeNull();
    });

    it('un WhatsApp sin verificar explica qué hacer y lleva a verificarlo', () => {
      rechazar(409, { status: 409, codigo: 'WHATSAPP_NO_VERIFICADO' });
      fixture.detectChanges();

      expect(texto()).toContain('Verifica tu WhatsApp para enviar solicitudes');
      expect(raiz().querySelector('a[href*="/mis-datos"]')?.getAttribute('href')).toContain(
        'verificar=whatsapp',
      );
    });

    it('el catálogo caído se explica y se puede reintentar', () => {
      rechazar(503, { status: 503, codigo: 'CATALOGO_NO_DISPONIBLE' });
      fixture.detectChanges();

      expect(texto()).toContain('El catálogo no responde en este momento.');
      expect(boton().disabled).toBe(false);
    });
  });

  describe('WhatsApp sin verificar en la cuenta', () => {
    it('avisa antes de enviar y no manda nada', () => {
      crear(ACTIVIDAD_UNA_HORA, false);
      expect(texto()).toContain('Verifica tu WhatsApp para enviar solicitudes');
      escribir('fechaInicio', '2026-10-28');

      enviar();

      http.expectNone(SOLICITUDES);
    });
  });

  describe('resumen de la oferta', () => {
    it('muestra la oferta y el precio de referencia con la nota del operador', () => {
      crear(ACTIVIDAD);
      escribir('personas', '3');

      expect(raiz().querySelector('aside h2')?.textContent).toContain('Caminata a Marinka');
      expect(texto()).toContain('Taganga');
      expect(texto()).toContain('Precio de referencia');
      expect(texto()).toContain('$ 210.000');
      expect(texto()).toContain('El precio final lo confirma el operador.');
    });

    it('alojamiento: precio por noche, unidad y noches', () => {
      crear(ALOJAMIENTO);
      escribir('fechaInicio', '2026-10-28');
      escribir('fechaSalida', '2026-10-30');
      escribir('numeroUnidades', '2');

      expect(texto()).toContain('$ 800.000');
    });

    it('sin datos suficientes no inventa un precio', () => {
      crear(ALOJAMIENTO);

      expect(raiz().querySelector('.resumen__total strong')?.textContent).toBe('—');
    });
  });

  describe('la oferta no se puede pedir', () => {
    it('una oferta que ya no está visible se explica', () => {
      fixture = TestBed.createComponent(FormularioSolicitud);
      fixture.componentRef.setInput('ofertaId', 'nada');
      fixture.detectChanges();

      http
        .expectOne('/api/catalogo/ofertas/nada')
        .flush(
          { status: 404, codigo: 'OFERTA_NO_ENCONTRADA' },
          { status: 404, statusText: 'Not Found' },
        );
      fixture.detectChanges();

      expect(texto()).toContain('Esta oferta ya no está disponible');
      expect(raiz().querySelector('form')).toBeNull();
    });
  });

  describe('inglés', () => {
    it('el formulario, el resumen y los avisos cambian de idioma al instante', () => {
      crear(ACTIVIDAD);
      enviar();
      expect(texto()).toContain('Elige una fecha.');

      establecerIdioma('en', { recordar: false });
      fixture.detectChanges();

      expect(raiz().querySelector('h1')?.textContent).toContain('Request availability');
      expect(texto()).toContain('Preferred date');
      expect(texto()).toContain('Departure time');
      expect(texto()).toContain('Number of people');
      expect(texto()).toContain('Choose a date.');
      expect(texto()).toContain('Reference price');
      expect(texto()).toContain('The operator confirms the final price.');
      expect(texto()).toContain('Send request');
      expect(texto()).toContain('COP 70,000 per person');
      expect(texto()).not.toContain('Elige una fecha.');
    });
  });
});
