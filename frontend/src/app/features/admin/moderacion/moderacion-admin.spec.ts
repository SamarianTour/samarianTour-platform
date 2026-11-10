import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ContenidoVersion, OfertaPendiente, OpinionPendiente } from '../../../core/api/modelos';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { ModeracionAdmin } from './moderacion-admin';

const OFERTAS = '/api/catalogo/admin/moderacion/ofertas';
const OPINIONES = '/api/catalogo/admin/moderacion/opiniones';
const ZONAS = '/api/catalogo/admin/zonas';
const DECISION_OFERTA = '/api/nucleo/admin/moderacion/ofertas/b00000000000000000000017/decision';
const DECISION_OPINION = '/api/nucleo/admin/moderacion/opiniones/op-1/decision';

const foto = (id: string) => ({ id, url: `/api/catalogo/fotos/${id}/galeria`, urlMiniatura: `/api/catalogo/fotos/${id}/miniatura` });

function contenido(extra: Partial<ContenidoVersion> = {}): ContenidoVersion {
  return {
    nombre: 'Caminata a Marinka',
    descripcion: 'Una caminata por la Sierra.',
    precioReferencia: 90000,
    unidadPrecio: 'por persona',
    zonaId: 'z1',
    capacidadMaxima: 10,
    fotos: [foto('f1'), foto('f2')],
    ...extra,
  };
}

function oferta(extra: Partial<OfertaPendiente> = {}): OfertaPendiente {
  return {
    id: 'b00000000000000000000017',
    tipo: 'ACTIVIDAD',
    prestadorId: '10000000-0000-0000-0000-000000000001',
    rntPrestadorActivo: true,
    numeroVersion: 1,
    enviadaEn: '2026-10-26T15:00:00',
    pendiente: contenido(),
    ...extra,
  };
}

function opinion(extra: Partial<OpinionPendiente> = {}): OpinionPendiente {
  return {
    id: 'op-1',
    ofertaId: 'b00000000000000000000001',
    nombreOferta: 'Caminata a Marinka',
    calificacion: 4,
    comentario: 'Una experiencia muy bonita, volvería.',
    fecha: '2026-10-25T10:00:00',
    ...extra,
  };
}

const pagina = <T>(contenido: T[], extra: object = {}) => ({
  contenido,
  pagina: 0,
  tamano: 10,
  totalElementos: contenido.length,
  totalPaginas: 1,
  ...extra,
});

const ZONAS_DEMO = [
  { id: 'z1', nombre: 'Taganga', altaAfluencia: false, areaProtegida: false, activa: true, buenasPracticas: [] },
];

describe('ModeracionAdmin (P05.2)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  let mostrar: ReturnType<typeof vi.spyOn>;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const tarjetas = () => Array.from(raiz().querySelectorAll<HTMLElement>('li.elemento'));
  const boton = (tarjeta: number, nombre: string) =>
    Array.from(tarjetas()[tarjeta].querySelectorAll<HTMLButtonElement>('button')).find((b) =>
      b.textContent?.trim().startsWith(nombre),
    )!;

  /** Navega a la dirección y devuelve la consulta de la cola y la de las zonas. */
  async function abrir(url: string, cola: string): Promise<{ cola: TestRequest; zonas: TestRequest }> {
    await harness.navigateByUrl(url, ModeracionAdmin);
    harness.detectChanges();
    return {
      cola: http.expectOne((p) => p.url === cola),
      zonas: http.expectOne(ZONAS),
    };
  }

  async function crearOfertas(items: OfertaPendiente[], extra: object = {}): Promise<void> {
    const consultas = await abrir('/admin/moderacion', OFERTAS);
    consultas.cola.flush(pagina(items, extra));
    consultas.zonas.flush(ZONAS_DEMO);
    harness.detectChanges();
  }

  async function crearOpiniones(items: OpinionPendiente[], extra: object = {}): Promise<void> {
    const consultas = await abrir('/admin/moderacion?tipo=opiniones', OPINIONES);
    consultas.cola.flush(pagina(items, extra));
    consultas.zonas.flush(ZONAS_DEMO);
    harness.detectChanges();
  }

  /** Pasa a otra cola con la pantalla ya abierta: las zonas no se vuelven a pedir. */
  async function irAOpiniones(items: OpinionPendiente[], extra: object = {}): Promise<void> {
    await harness.navigateByUrl('/admin/moderacion?tipo=opiniones', ModeracionAdmin);
    harness.detectChanges();
    http.expectOne((p) => p.url === OPINIONES).flush(pagina(items, extra));
    harness.detectChanges();
  }

  function escribirMotivo(tarjeta: number, valor: string): void {
    const campo = tarjetas()[tarjeta].querySelector<HTMLTextAreaElement>('textarea')!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'ADMINISTRADOR' }))),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'admin/moderacion', component: ModeracionAdmin }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mostrar = vi.spyOn(TestBed.inject(AvisosService), 'mostrar');
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  describe('las colas', () => {
    it('por defecto abre la de ofertas y pide la primera página y las zonas', async () => {
      const { cola, zonas } = await abrir('/admin/moderacion', OFERTAS);

      expect(cola.request.params.get('page')).toBe('0');
      expect(cola.request.params.get('size')).toBe('10');
      cola.flush(pagina([]));
      zonas.flush([]);
    });

    it('?tipo=opiniones abre la de opiniones', async () => {
      const { cola, zonas } = await abrir('/admin/moderacion?tipo=opiniones', OPINIONES);

      cola.flush(pagina([]));
      zonas.flush([]);
      harness.detectChanges();

      const enlaces = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.filtros a'));
      expect(enlaces.map((a) => [a.textContent?.trim(), a.getAttribute('aria-current')])).toEqual([
        ['Ofertas', null],
        ['Opiniones', 'page'],
      ]);
      expect(enlaces[1].getAttribute('href')).toBe('/admin/moderacion?tipo=opiniones');
    });

    it('dice cuántas hay pendientes, en singular y plural', async () => {
      await crearOfertas([oferta()]);
      expect(texto()).toContain('1 oferta pendiente');

      await irAOpiniones([opinion(), opinion({ id: 'op-2' })], { totalElementos: 2 });
      expect(texto()).toContain('2 opiniones pendientes');
    });

    it('sin nada por revisar lo dice', async () => {
      await crearOfertas([]);
      expect(texto()).toContain('No hay ofertas por revisar');

      await irAOpiniones([]);
      expect(texto()).toContain('No hay opiniones por revisar');
    });

    it('si la consulta falla avisa del error', async () => {
      const { cola, zonas } = await abrir('/admin/moderacion', OFERTAS);
      cola.error(new ProgressEvent('error'));
      zonas.flush([]);
      harness.detectChanges();

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    });

    it('"Actualizar la lista" vuelve a leer la cola', async () => {
      await crearOfertas([oferta()]);

      Array.from(raiz().querySelectorAll<HTMLButtonElement>('.barra button'))[0].click();

      http.expectOne((p) => p.url === OFERTAS).flush(pagina([]));
      harness.detectChanges();
      expect(texto()).toContain('No hay ofertas por revisar');
    });

    it('pagina conservando la cola elegida', async () => {
      await crearOpiniones([opinion()], { totalPaginas: 3 });

      Array.from(raiz().querySelectorAll<HTMLButtonElement>('.paginacion button'))
        .find((b) => b.textContent?.includes('Siguiente'))!
        .click();
      await harness.fixture.whenStable();

      expect(TestBed.inject(Router).url).toBe('/admin/moderacion?tipo=opiniones&page=1');
      const peticion = http.expectOne((p) => p.url === OPINIONES);
      expect(peticion.request.params.get('page')).toBe('1');
      peticion.flush(pagina([], { pagina: 1, totalPaginas: 3 }));
    });
  });

  describe('revisar una oferta', () => {
    it('muestra la oferta con su versión, que es nueva, y el contenido que envió el prestador', async () => {
      await crearOfertas([oferta()]);

      const t = tarjetas()[0].textContent!.replace(/[  ]/g, ' ');
      expect(tarjetas()[0].querySelector('h2')?.textContent).toBe('Caminata a Marinka');
      expect(t).toContain('Actividad');
      expect(t).toContain('Versión 1');
      expect(t).toContain('Oferta nueva');
      expect(t).toContain('Prestador 10000000');
      expect(t).toContain('$ 90.000 por persona');
      expect(t).toContain('Taganga');
      expect(t).toContain('Una caminata por la Sierra.');
    });

    it('muestra las fotos con un texto alternativo que dice cuál es', async () => {
      await crearOfertas([oferta()]);

      const fotos = Array.from(tarjetas()[0].querySelectorAll<HTMLImageElement>('.miniaturas img'));
      expect(fotos.map((f) => f.getAttribute('src'))).toEqual([
        '/api/catalogo/fotos/f1/miniatura',
        '/api/catalogo/fotos/f2/miniatura',
      ]);
      expect(fotos[0].getAttribute('alt')).toBe('Foto 1 de 2');
    });

    it('si no se conoce la zona deja su identificador', async () => {
      const consultas = await abrir('/admin/moderacion', OFERTAS);
      consultas.cola.flush(pagina([oferta()]));
      consultas.zonas.error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(tarjetas()[0].textContent).toContain('z1');
    });

    it('en una edición marca lo que cambió frente a la versión publicada', async () => {
      await crearOfertas([
        oferta({
          numeroVersion: 2,
          pendiente: contenido({ precioReferencia: 95000, fotos: [foto('f2'), foto('f1')] }),
          publicada: contenido(),
        }),
      ]);

      const t = tarjetas()[0];
      expect(t.textContent).toContain('Edición de una oferta publicada');
      expect(t.textContent).toContain('Las líneas marcadas cambiaron');
      const cambiadas = Array.from(t.querySelectorAll('.linea--cambio'));
      expect(cambiadas.map((c) => c.textContent)).toEqual([
        expect.stringContaining('Precio de referencia'),
        expect.stringContaining('2 fotos'),
      ]);
      expect(t.querySelector('.lineas .etiqueta')?.textContent?.trim()).toBe('Cambió');
    });

    it('una oferta cuyo prestador perdió el RNT solo se puede rechazar (DN-35)', async () => {
      await crearOfertas([oferta({ rntPrestadorActivo: false })]);

      expect(tarjetas()[0].textContent).toContain('no se puede aprobar esta oferta, solo rechazarla');
      expect(boton(0, 'Aprobar').disabled).toBe(true);
      expect(boton(0, 'Rechazar').disabled).toBe(false);
    });
  });

  describe('aprobar', () => {
    it('envía la decisión al núcleo con el número de la versión revisada', async () => {
      await crearOfertas([oferta({ numeroVersion: 3 })]);

      boton(0, 'Aprobar').click();

      const peticion = http.expectOne(DECISION_OFERTA);
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({ numeroVersion: 3, decision: 'APROBAR' });
      peticion.flush({ tipo: 'OFERTA', moderadoId: 'b00000000000000000000017', decision: 'APROBAR' });
    });

    it('avisa que la decisión llega al catálogo en unos segundos y deja de ofrecer los botones', async () => {
      await crearOfertas([oferta()]);

      boton(0, 'Aprobar').click();
      http.expectOne(DECISION_OFERTA).flush({ decision: 'APROBAR' });
      harness.detectChanges();

      const t = tarjetas()[0];
      expect(t.querySelector('.enviada')?.textContent).toContain('Aprobación enviada');
      expect(t.querySelector('.enviada')?.textContent).toContain('llega al catálogo en unos segundos');
      expect(t.querySelectorAll('.acciones button')).toHaveLength(0);
      expect(mostrar).toHaveBeenCalledWith(
        'Enviaste la aprobación. El catálogo la aplicará en unos segundos.',
      );
    });

    it('mientras se envía no deja decidir otra cosa, y un doble clic envía una sola petición', async () => {
      await crearOfertas([oferta(), oferta({ id: 'b2', numeroVersion: 1 })]);

      boton(0, 'Aprobar').click();
      boton(0, 'Aprobar').click();
      harness.detectChanges();

      expect(http.match(DECISION_OFERTA)).toHaveLength(1);
      expect(boton(1, 'Aprobar').disabled).toBe(true);
      expect(boton(1, 'Rechazar').disabled).toBe(true);
    });

    it('lo decidido sigue marcado aunque se vuelva a leer la cola antes de que el catálogo lo aplique', async () => {
      await crearOfertas([oferta()]);
      boton(0, 'Aprobar').click();
      http.expectOne(DECISION_OFERTA).flush({ decision: 'APROBAR' });

      Array.from(raiz().querySelectorAll<HTMLButtonElement>('.barra button'))[0].click();
      http.expectOne((p) => p.url === OFERTAS).flush(pagina([oferta()]));
      harness.detectChanges();

      expect(tarjetas()[0].querySelector('.enviada')).not.toBeNull();
      expect(tarjetas()[0].querySelectorAll('.acciones button')).toHaveLength(0);
    });
  });

  describe('rechazar', () => {
    it('pide el motivo antes de enviar y no manda nada sin él', async () => {
      await crearOfertas([oferta()]);

      boton(0, 'Rechazar').click();
      harness.detectChanges();
      boton(0, 'Confirmar rechazo').click();
      harness.detectChanges();

      http.expectNone(DECISION_OFERTA);
      expect(tarjetas()[0].querySelector('.campo__error')?.textContent).toContain('Escribe el motivo del rechazo.');
      expect(tarjetas()[0].querySelector('textarea')?.getAttribute('aria-invalid')).toBe('true');
    });

    it('con motivo envía el rechazo, con el motivo recortado, y avisa que se le notifica al prestador', async () => {
      await crearOfertas([oferta()]);
      boton(0, 'Rechazar').click();
      harness.detectChanges();
      expect(tarjetas()[0].textContent).toContain('Se le notifica al prestador con este texto. (0 de 1000)');

      escribirMotivo(0, '  Falta el punto de encuentro  ');
      boton(0, 'Confirmar rechazo').click();
      harness.detectChanges();

      const peticion = http.expectOne(DECISION_OFERTA);
      expect(peticion.request.body).toEqual({
        numeroVersion: 1,
        decision: 'RECHAZAR',
        motivo: 'Falta el punto de encuentro',
      });
      peticion.flush({ decision: 'RECHAZAR' });
      harness.detectChanges();
      expect(tarjetas()[0].querySelector('.enviada')?.textContent).toContain('Rechazo enviado');
      expect(mostrar).toHaveBeenCalledWith(
        'Enviaste el rechazo con su motivo. El catálogo lo aplicará en unos segundos.',
      );
    });

    it('un motivo de más de 1000 caracteres no se envía', async () => {
      await crearOfertas([oferta()]);
      boton(0, 'Rechazar').click();
      harness.detectChanges();

      escribirMotivo(0, 'a'.repeat(1001));
      boton(0, 'Confirmar rechazo').click();
      harness.detectChanges();

      http.expectNone(DECISION_OFERTA);
      expect(tarjetas()[0].querySelector('.campo__error')?.textContent).toContain('hasta 1000 caracteres');
    });

    it('cancelar cierra el formulario sin enviar nada', async () => {
      await crearOfertas([oferta()]);
      boton(0, 'Rechazar').click();
      harness.detectChanges();

      boton(0, 'Cancelar').click();
      harness.detectChanges();

      expect(tarjetas()[0].querySelector('textarea')).toBeNull();
      http.expectNone(DECISION_OFERTA);
    });
  });

  describe('cuando el núcleo rechaza la decisión', () => {
    it.each(['MODERACION_YA_DECIDIDA', 'VERSION_NO_VIGENTE', 'NO_ESTA_EN_MODERACION', 'PRESTADOR_NO_HABILITADO'])(
      '%s lo explica en la tarjeta y vuelve a leer la cola',
      async (codigo) => {
        await crearOfertas([oferta()]);

        boton(0, 'Aprobar').click();
        http.expectOne(DECISION_OFERTA).flush({ codigo }, { status: 409, statusText: 'Conflict' });
        harness.detectChanges();

        expect(tarjetas()[0].querySelector('.alerta--error')).not.toBeNull();
        http.expectOne((p) => p.url === OFERTAS).flush(pagina([oferta()]));
        harness.detectChanges();
        expect(tarjetas()[0].querySelector('.alerta--error')).not.toBeNull();
        expect(boton(0, 'Aprobar').disabled).toBe(false);
      },
    );

    it('si el catálogo no está disponible lo avisa y deja reintentar, sin volver a leer la cola', async () => {
      await crearOfertas([oferta()]);

      boton(0, 'Aprobar').click();
      http
        .expectOne(DECISION_OFERTA)
        .flush({ codigo: 'CATALOGO_NO_DISPONIBLE' }, { status: 503, statusText: 'Service Unavailable' });
      harness.detectChanges();

      expect(tarjetas()[0].querySelector('.alerta--error')?.textContent).toContain('catálogo');
      expect(boton(0, 'Aprobar').disabled).toBe(false);
      http.expectNone((p) => p.url === OFERTAS);
    });
  });

  describe('revisar una opinión', () => {
    it('muestra la oferta, la calificación y el comentario, sin datos del turista', async () => {
      await crearOpiniones([opinion()]);

      const t = tarjetas()[0];
      expect(t.querySelector('h2')?.textContent?.trim()).toBe('Sobre Caminata a Marinka');
      expect(t.textContent).toContain('Calificación: 4 de 5');
      expect(t.querySelector('blockquote')?.textContent).toBe('Una experiencia muy bonita, volvería.');
      expect(t.textContent).not.toMatch(/turista:|correo|WhatsApp/i);
    });

    it('si la opinión no trae el nombre de la oferta usa su código', async () => {
      await crearOpiniones([opinion({ nombreOferta: undefined })]);

      expect(tarjetas()[0].querySelector('h2')?.textContent?.trim()).toBe('Oferta B0000000');
    });

    it('aprobar manda solo la decisión, sin número de versión', async () => {
      await crearOpiniones([opinion()]);

      boton(0, 'Aprobar').click();

      const peticion = http.expectOne(DECISION_OPINION);
      expect(peticion.request.body).toEqual({ decision: 'APROBAR' });
      peticion.flush({ decision: 'APROBAR' });
      harness.detectChanges();
      expect(tarjetas()[0].querySelector('.enviada')).not.toBeNull();
    });

    it('rechazar exige el motivo y avisa que se le notifica al turista', async () => {
      await crearOpiniones([opinion()]);
      boton(0, 'Rechazar').click();
      harness.detectChanges();
      expect(tarjetas()[0].textContent).toContain('Se le notifica al turista con este texto');

      boton(0, 'Confirmar rechazo').click();
      harness.detectChanges();
      http.expectNone(DECISION_OPINION);

      escribirMotivo(0, 'Lenguaje ofensivo');
      boton(0, 'Confirmar rechazo').click();
      const peticion = http.expectOne(DECISION_OPINION);
      expect(peticion.request.body).toEqual({ decision: 'RECHAZAR', motivo: 'Lenguaje ofensivo' });
      peticion.flush({ decision: 'RECHAZAR' });
    });
  });

  describe('en inglés', () => {
    it('todo cambia al idioma: colas, contenido, botones y el aviso de la bandeja', async () => {
      establecerIdioma('en', { recordar: false });
      await crearOfertas([oferta({ numeroVersion: 2, publicada: contenido({ nombre: 'Otra' }) })]);

      const t = tarjetas()[0].textContent!.replace(/[  ]/g, ' ');
      expect(texto()).toContain('Moderation');
      expect(texto()).toContain('1 offer pending');
      expect(t).toContain('Edit of a published offer');
      expect(t).toContain('Reference price');
      expect(t).toContain('COP 90,000 per person');
      expect(t).toContain('Changed');
      expect(boton(0, 'Approve')).toBeDefined();

      boton(0, 'Approve').click();
      http.expectOne(DECISION_OFERTA).flush({ decision: 'APROBAR' });
      harness.detectChanges();
      expect(tarjetas()[0].querySelector('.enviada')?.textContent).toContain('Approval sent');
      expect(tarjetas()[0].querySelector('.enviada')?.textContent).toContain('a few seconds');
    });
  });
});
