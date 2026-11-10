import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Zona } from '../../../core/api/modelos';
import { Recomendacion, Recomendaciones } from '../../../core/api/modelos-recomendacion';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { ChatRecomendaciones } from './chat-recomendaciones';

const POST = '/api/recomendacion/recomendaciones';
const ZONAS_URL = '/api/catalogo/zonas';

const ZONAS = [
  { id: 'minca', nombre: 'Minca', areaProtegida: false, altaAfluencia: false, activa: true, buenasPracticas: [] },
  { id: 'taganga', nombre: 'Taganga', areaProtegida: false, altaAfluencia: true, activa: true, buenasPracticas: [] },
] as Zona[];

function recomendacion(posicion: number, nombre: string, extra: Partial<Recomendacion> = {}): Recomendacion {
  return {
    posicion,
    oferta: {
      id: `b${posicion}`,
      nombre,
      tipo: 'ACTIVIDAD',
      zonaNombre: 'Minca',
      precioReferencia: 120000,
      unidadPrecio: 'por persona',
    },
    criterios: ['TIPO_ACTIVIDAD'],
    explicacion: `Porque ${nombre} coincide con lo que buscas.`,
    ...extra,
  };
}

const TRES: Recomendaciones = {
  motor: 'REGLAS',
  recomendaciones: [recomendacion(1, 'Aves en Minca'), recomendacion(2, 'Café y cascada'), recomendacion(3, 'Sendero del río')],
};

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

const normal = (texto: string | null | undefined) => (texto ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ').trim();

describe('ChatRecomendaciones (P08.1)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => normal(raiz().textContent);
  const mensajes = () => Array.from(raiz().querySelectorAll<HTMLElement>('.mensaje'));
  const ultimo = () => mensajes()[mensajes().length - 1];
  const tarjetas = (mensaje: Element = ultimo()) => Array.from(mensaje.querySelectorAll('app-tarjeta-recomendada'));
  const campo = () => raiz().querySelector<HTMLInputElement>('#chat-texto')!;
  const botonEnviar = () => raiz().querySelector<HTMLButtonElement>('.chat__enviar')!;
  const ideas = () => Array.from(raiz().querySelectorAll<HTMLButtonElement>('.idea'));

  async function abrir(zonas: Zona[] | 'falla' = ZONAS): Promise<void> {
    await harness.navigateByUrl('/recomendaciones', ChatRecomendaciones);
    harness.detectChanges();
    const peticion = http.expectOne(ZONAS_URL);
    if (zonas === 'falla') {
      peticion.error(new ProgressEvent('error'));
    } else {
      peticion.flush(zonas);
    }
    harness.detectChanges();
  }

  function escribir(valor: string): void {
    campo().value = valor;
    campo().dispatchEvent(new Event('input'));
    harness.detectChanges();
  }

  function enviar(): void {
    botonEnviar().click();
    harness.detectChanges();
  }

  /** Escribe, envía y devuelve la petición que sale. */
  function preguntar(consulta: string): TestRequest {
    escribir(consulta);
    enviar();
    return http.expectOne(POST);
  }

  function responder(peticion: TestRequest, respuesta: Recomendaciones = TRES): void {
    peticion.flush(respuesta);
    harness.detectChanges();
  }

  function marcar(etiqueta: string): void {
    const caja = Array.from(raiz().querySelectorAll<HTMLLabelElement>('label.opcion'))
      .find((l) => l.textContent?.trim() === etiqueta)!
      .querySelector<HTMLInputElement>('input')!;
    caja.checked = true;
    caja.dispatchEvent(new Event('change'));
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'recomendaciones', component: ChatRecomendaciones }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    vi.useRealTimers();
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  describe('al abrir', () => {
    it('saluda y ofrece cuatro ideas para empezar, sin pedir nada al servicio de recomendación', async () => {
      await abrir();

      expect(mensajes()).toHaveLength(1);
      expect(mensajes()[0].textContent).toContain('Cuéntame qué te gustaría vivir en Santa Marta');
      expect(ideas().map((i) => i.textContent?.trim())).toEqual([
        'Planes de bajo esfuerzo cerca del mar',
        'Qué hacer en Minca en 1 día',
        'Opciones para ir con niños',
        'Experiencias de buceo',
      ]);
      http.expectNone(POST);
    });

    it('lleva un enlace visible a las alertas del destino', async () => {
      await abrir();

      const enlace = raiz().querySelector<HTMLAnchorElement>('a[href="/alertas"]');
      expect(enlace?.textContent?.trim()).toBe('Ver alertas del destino');
    });

    it('avisa que son sugerencias y que no se escriban datos personales', async () => {
      await abrir();

      expect(texto()).toContain('Son sugerencias automáticas, no publicidad');
      expect(texto()).toContain('No escribas datos personales');
      expect(texto()).toContain('si la recargas, se borra');
    });

    it('la conversación es un registro que se anuncia a los lectores de pantalla', async () => {
      await abrir();

      const registro = raiz().querySelector('.chat__registro')!;
      expect(registro.getAttribute('role')).toBe('log');
      expect(registro.getAttribute('aria-live')).toBe('polite');
    });

    it('el campo acepta hasta 500 caracteres y no se autocompleta', async () => {
      await abrir();

      expect(campo().getAttribute('maxlength')).toBe('500');
      expect(campo().getAttribute('autocomplete')).toBe('off');
      expect(raiz().querySelector('label[for="chat-texto"]')?.textContent).toBe('Escribe lo que buscas');
    });
  });

  describe('enviar lo que se busca', () => {
    it('manda el texto con POST en el cuerpo, con el idioma, y no en la dirección', async () => {
      await abrir();

      const peticion = preguntar('  Quiero ver aves en la montaña  ');

      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({ texto: 'Quiero ver aves en la montaña', idioma: 'es' });
      expect(peticion.request.urlWithParams).toBe(POST);
      responder(peticion);
    });

    it('muestra lo que escribió en su burbuja, vacía el campo y dice "Buscando opciones…"', async () => {
      await abrir();

      preguntar('aves');

      expect(mensajes()).toHaveLength(3);
      expect(mensajes()[1].classList).toContain('mensaje--turista');
      expect(mensajes()[1].textContent).toContain('aves');
      expect(campo().value).toBe('');
      expect(normal(ultimo().textContent)).toContain('Buscando opciones…');
      expect(normal(ultimo().textContent)).toContain('Puede tardar hasta unos 10 segundos.');
      expect(ultimo().querySelector('[role="status"]')).not.toBeNull();
    });

    it('mientras busca no deja enviar otra vez', async () => {
      await abrir();
      preguntar('aves');
      expect(botonEnviar().disabled).toBe(true);
      expect(ideas().every((i) => i.disabled)).toBe(true);

      escribir('montaña');
      enviar();

      http.expectNone(POST);
      expect(mensajes()).toHaveLength(3);
    });

    it('al responder reemplaza el "Buscando" por las ofertas, con su explicación', async () => {
      await abrir();

      responder(preguntar('aves'));

      expect(mensajes()).toHaveLength(3);
      expect(normal(ultimo().textContent)).not.toContain('Buscando opciones');
      expect(tarjetas()).toHaveLength(3);
      expect(ultimo().textContent).toContain('Estas ofertas verificadas pueden encajar');
      expect(tarjetas()[0].textContent).toContain('Porque Aves en Minca coincide con lo que buscas.');
      expect(tarjetas()[1].querySelector('h3 a')?.getAttribute('href')).toBe('/ofertas/b2');
      expect(botonEnviar().disabled).toBe(false);
    });

    it('dice quién eligió: criterios explícitos (reglas) o inteligencia artificial', async () => {
      await abrir();
      responder(preguntar('aves'));
      expect(ultimo().textContent).toContain('Elegidas con criterios explícitos');

      responder(preguntar('mar'), { ...TRES, motor: 'IA' });

      expect(ultimo().textContent).toContain('Elegidas con ayuda de inteligencia artificial');
    });

    it('una idea se envía como si se hubiera escrito', async () => {
      await abrir();

      ideas()[1].click();
      harness.detectChanges();

      const peticion = http.expectOne(POST);
      expect(peticion.request.body).toEqual({ texto: 'Qué hacer en Minca en 1 día', idioma: 'es' });
      responder(peticion);
      expect(mensajes()[1].textContent).toContain('Qué hacer en Minca en 1 día');
    });

    it('se puede seguir preguntando: cada respuesta se queda en la conversación', async () => {
      await abrir();
      responder(preguntar('aves'));

      responder(preguntar('buceo'), { motor: 'REGLAS', recomendaciones: [recomendacion(1, 'Buceo en Taganga'), recomendacion(2, 'Snorkel'), recomendacion(3, 'Bautizo')] });

      expect(mensajes()).toHaveLength(5);
      expect(tarjetas(mensajes()[2])).toHaveLength(3);
      expect(tarjetas(mensajes()[4])).toHaveLength(3);
    });
  });

  describe('sin nada que enviar', () => {
    it('no envía nada y pide escribir algo o elegir un filtro', async () => {
      await abrir();

      enviar();

      http.expectNone(POST);
      expect(raiz().querySelector('#chat-error')?.textContent).toContain('Escribe lo que buscas o elige al menos un filtro.');
      expect(campo().getAttribute('aria-invalid')).toBe('true');
      expect(document.activeElement).toBe(campo());
      expect(mensajes()).toHaveLength(1);
    });

    it('un texto de solo espacios cuenta como vacío', async () => {
      await abrir();
      escribir('    ');

      enviar();

      http.expectNone(POST);
      expect(raiz().querySelector('#chat-error')).not.toBeNull();
    });

    it('el aviso desaparece al escribir', async () => {
      await abrir();
      enviar();
      expect(raiz().querySelector('#chat-error')).not.toBeNull();

      escribir('a');

      expect(raiz().querySelector('#chat-error')).toBeNull();
    });

    it('con solo un filtro, sin texto, sí envía', async () => {
      await abrir();
      marcar('Accesible en silla de ruedas');

      enviar();

      const peticion = http.expectOne(POST);
      expect(peticion.request.body).toEqual({ accesibleSillaRuedas: true, idioma: 'es' });
      responder(peticion);
    });
  });

  describe('los filtros', () => {
    it('viajan en la consulta y se ven en la burbuja del turista', async () => {
      await abrir();
      // Como en el uso real, la vista se refresca entre un cambio y el siguiente.
      marcar('Actividad');
      marcar('Accesible en silla de ruedas');
      const precio = raiz().querySelector<HTMLInputElement>('#filtro-precio')!;
      precio.value = '150000';
      precio.dispatchEvent(new Event('input'));
      harness.detectChanges();
      const esfuerzo = raiz().querySelector<HTMLSelectElement>('#filtro-esfuerzo')!;
      esfuerzo.value = 'BAJO';
      esfuerzo.dispatchEvent(new Event('change'));
      harness.detectChanges();
      const zona = raiz().querySelector<HTMLSelectElement>('#filtro-zona')!;
      zona.value = 'minca';
      zona.dispatchEvent(new Event('change'));
      harness.detectChanges();

      const peticion = preguntar('aves');

      expect(peticion.request.body).toEqual({
        texto: 'aves',
        tiposOferta: ['ACTIVIDAD'],
        zonaId: 'minca',
        accesibleSillaRuedas: true,
        nivelEsfuerzoMaximo: 'BAJO',
        precioMaximo: 150000,
        idioma: 'es',
      });
      const burbuja = Array.from(mensajes()[1].querySelectorAll('.mensaje__filtros li')).map((f) => normal(f.textContent));
      expect(burbuja).toEqual(['Actividad', 'Minca', 'Accesible en silla de ruedas', 'esfuerzo hasta bajo', 'hasta $ 150.000']);
      responder(peticion);
    });

    it('se conservan para la siguiente pregunta', async () => {
      await abrir();
      marcar('Accesible en silla de ruedas');
      responder(preguntar('aves'));

      const peticion = preguntar('mar');

      expect(peticion.request.body.accesibleSillaRuedas).toBe(true);
      responder(peticion);
    });

    it('un precio máximo de cero no se envía y lo explica', async () => {
      await abrir();
      const precio = raiz().querySelector<HTMLInputElement>('#filtro-precio')!;
      precio.value = '0';
      precio.dispatchEvent(new Event('input'));
      escribir('aves');

      enviar();

      http.expectNone(POST);
      expect(raiz().querySelector('#chat-error')?.textContent).toContain('El precio máximo debe ser mayor que cero.');
    });

    it('si la lista de zonas falla, no se ofrece el filtro de zona y lo demás sigue funcionando', async () => {
      await abrir('falla');

      expect(raiz().querySelector('#filtro-zona')).toBeNull();
      responder(preguntar('aves'));
      expect(tarjetas()).toHaveLength(3);
    });
  });

  describe('pocas coincidencias y ninguna', () => {
    it('con pocas ofertas las muestra con el aviso de que los filtros duros no se relajan', async () => {
      await abrir();

      responder(preguntar('aves'), {
        motor: 'REGLAS',
        aviso: 'POCAS_COINCIDENCIAS',
        recomendaciones: [recomendacion(1, 'Aves en Minca'), recomendacion(2, 'Café y cascada')],
      });

      expect(tarjetas()).toHaveLength(2);
      expect(ultimo().querySelector('.alerta--aviso')?.textContent).toContain('Encontramos pocas ofertas que cumplan todo lo que pediste');
      expect(ultimo().textContent).toContain('La accesibilidad, el esfuerzo y el precio no se relajan');
    });

    it('sin ninguna oferta lo dice y lleva al catálogo', async () => {
      await abrir();

      responder(preguntar('aves'), { motor: 'REGLAS', aviso: 'POCAS_COINCIDENCIAS', recomendaciones: [] });

      expect(tarjetas()).toHaveLength(0);
      expect(ultimo().textContent).toContain('No encontramos ofertas que cumplan lo que pediste');
      expect(ultimo().querySelector('a[href="/"]')?.textContent?.trim()).toBe('Explorar el catálogo');
    });
  });

  describe('si la recomendación no está disponible (degradación)', () => {
    it.each([502, 503])('un %s avisa que el servicio no está disponible, sin romper la página', async (estado) => {
      await abrir();
      const peticion = preguntar('aves');

      const { body, init } = problema(estado, 'RECOMENDACION_NO_DISPONIBLE');
      peticion.flush(body, init);
      harness.detectChanges();

      expect(ultimo().textContent).toContain('el servicio de recomendaciones no está disponible');
      expect(ultimo().textContent).toContain('explorar el catálogo y filtrar por zona o tipo de oferta');
      expect(ultimo().querySelector('a[href="/"]')?.textContent?.trim()).toBe('Explorar el catálogo');
      expect(Array.from(ultimo().querySelectorAll('button')).map((b) => b.textContent?.trim())).toEqual(['Intentar de nuevo']);
      // La página sigue: se puede escribir de nuevo y el resto de lo que ya estaba no se pierde.
      expect(botonEnviar().disabled).toBe(false);
      expect(mensajes()[1].textContent).toContain('aves');
    });

    it('sin conexión avisa que no hay conexión', async () => {
      await abrir();
      const peticion = preguntar('aves');

      peticion.error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(ultimo().textContent).toContain('Parece que no hay conexión a internet.');
    });

    it('a los 10 s sin respuesta deja de esperar, cancela la consulta y avisa', async () => {
      await abrir();
      vi.useFakeTimers();
      const peticion = preguntar('aves');

      vi.advanceTimersByTime(9_999);
      harness.detectChanges();
      expect(normal(ultimo().textContent)).toContain('Buscando opciones…');

      vi.advanceTimersByTime(1);
      harness.detectChanges();

      expect(peticion.cancelled).toBe(true);
      expect(ultimo().textContent).toContain('Está tardando más de lo normal y dejé de esperar.');
      expect(normal(ultimo().textContent)).not.toContain('Buscando opciones');
      expect(botonEnviar().disabled).toBe(false);
    });

    it('una respuesta lenta pero a tiempo (9 s, el peor caso del servicio) sí se muestra', async () => {
      await abrir();
      vi.useFakeTimers();
      const peticion = preguntar('aves');

      vi.advanceTimersByTime(9_000);
      responder(peticion);

      expect(tarjetas()).toHaveLength(3);
      expect(ultimo().textContent).not.toContain('dejé de esperar');
    });

    it('"Intentar de nuevo" repite la misma consulta, con sus filtros, en el mismo lugar', async () => {
      await abrir();
      marcar('Accesible en silla de ruedas');
      const primera = preguntar('aves');
      const { body, init } = problema(503, 'RECOMENDACION_NO_DISPONIBLE');
      primera.flush(body, init);
      harness.detectChanges();
      const cantidad = mensajes().length;

      ultimo().querySelector<HTMLButtonElement>('button')!.click();
      harness.detectChanges();

      expect(normal(ultimo().textContent)).toContain('Buscando opciones…');
      const segunda = http.expectOne(POST);
      expect(segunda.request.body).toEqual({ texto: 'aves', accesibleSillaRuedas: true, idioma: 'es' });
      responder(segunda);
      expect(mensajes()).toHaveLength(cantidad);
      expect(tarjetas()).toHaveLength(3);
    });

    it('un dato inválido (400) se explica con el mensaje del servicio, no como "no disponible"', async () => {
      await abrir();
      const peticion = preguntar('aves');

      const { body, init } = problema(400, 'DATO_INVALIDO');
      peticion.flush(body, init);
      harness.detectChanges();

      expect(ultimo().textContent).not.toContain('no está disponible');
      expect(ultimo().querySelector('button')).toBeNull();
    });

    it('si falla una respuesta, las anteriores siguen en la conversación', async () => {
      await abrir();
      responder(preguntar('aves'));
      const peticion = preguntar('mar');

      const { body, init } = problema(503, 'RECOMENDACION_NO_DISPONIBLE');
      peticion.flush(body, init);
      harness.detectChanges();

      expect(tarjetas(mensajes()[2])).toHaveLength(3);
      expect(ultimo().textContent).toContain('no está disponible');
    });
  });

  describe('la conversación solo vive en memoria', () => {
    it('lo que escribe el turista no se guarda en el navegador ni sale en la dirección', async () => {
      await abrir();
      const secreto = 'me llamo Laura y mi teléfono es 3001234567';

      responder(preguntar(secreto));

      const guardado = JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage });
      expect(guardado).not.toContain('Laura');
      expect(guardado).not.toContain('3001234567');
      expect(harness.fixture.nativeElement.ownerDocument.location.href).not.toContain('Laura');
    });
  });

  it('en inglés cambian las ideas, las filas, el aviso y piden las explicaciones en inglés', async () => {
    establecerIdioma('en', { recordar: false });
    await abrir();

    expect(raiz().querySelector('h1')?.textContent).toBe('Travel assistant');
    expect(ideas().map((i) => i.textContent?.trim())[1]).toBe('What to do in Minca in 1 day');
    expect(raiz().querySelector('a[href="/alertas"]')?.textContent?.trim()).toBe('See destination alerts');

    const peticion = preguntar('birds');
    expect(peticion.request.body.idioma).toBe('en');
    expect(normal(ultimo().textContent)).toContain('Looking for options…');
    responder(peticion, { ...TRES, motor: 'IA' });

    expect(ultimo().textContent).toContain('These verified offers may be a good fit');
    expect(ultimo().textContent).toContain('Chosen with the help of artificial intelligence');
    expect(texto()).toContain('These are automatic suggestions, not advertising');
  });
});
