import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OfertaPrestador, TipoPrestador } from '../../core/api/modelos';
import { erroresInterceptor } from '../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../core/sesion/testing';
import { DetalleOferta } from './detalle-oferta/detalle-oferta';
import { FormularioOferta } from './formulario-oferta/formulario-oferta';
import { SelectorFotos } from './formulario-oferta/selector-fotos/selector-fotos';
import { MisOfertas } from './mis-ofertas/mis-ofertas';
import { PanelPrestador } from './panel/panel-prestador';

const LISTA = '/api/catalogo/prestador/ofertas';

const RECHAZADA: OfertaPrestador = {
  id: 'b00000000000000000000001',
  tipo: 'ACTIVIDAD',
  retirada: false,
  publicada: {
    numero: 1,
    estadoModeracion: 'APROBADA',
    enviadaEn: '2026-10-20T10:00:00',
    revisadaEn: '2026-10-21T10:00:00',
    nombre: 'Caminata a Marinka',
    precioReferencia: 70000,
    unidadPrecio: 'por persona',
  },
  pendiente: {
    numero: 2,
    estadoModeracion: 'RECHAZADA',
    enviadaEn: '2026-11-01T10:00:00',
    nombre: 'Caminata a Marinka',
    precioReferencia: 80000,
    unidadPrecio: 'por persona',
    motivoRechazo: 'Faltan fotos',
  },
};

const PAGINA = (contenido: OfertaPrestador[]) => ({
  contenido,
  pagina: 0,
  tamano: 100,
  totalElementos: contenido.length,
  totalPaginas: 1,
});

/** El panel del prestador en español y en inglés (RF-18, RF-19), con los mensajes que cambian en vivo. */
describe('prestador en español e inglés', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<unknown>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const cambiarA = (idioma: 'es' | 'en') => {
    establecerIdioma(idioma, { recordar: false });
    fixture.detectChanges();
  };

  function sesion(tipoPrestador?: TipoPrestador): void {
    const datos = respuestaDeSesion({ rol: 'PRESTADOR', tipoPrestador });
    localStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(
        aSesionGuardada({ ...datos, cuenta: { ...datos.cuenta, nombre: 'Hotel Sierra' } }),
      ),
    );
  }

  beforeEach(() => {
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

  afterEach(() => http.match(() => true).forEach((p) => p.error(new ProgressEvent('error'))));

  describe('barra lateral', () => {
    it('cambia el rol, el tipo y la navegación', () => {
      sesion('ALOJAMIENTO');
      fixture = TestBed.createComponent(PanelPrestador);
      fixture.detectChanges();
      expect(texto()).toContain('Prestador');
      expect(texto()).toContain('Panel de control');
      expect(texto()).toContain('Publicar oferta');

      cambiarA('en');

      expect(texto()).toContain('Provider');
      expect(texto()).toContain('Accommodation');
      expect(texto()).toContain('Control panel');
      expect(texto()).toContain('My offers');
      expect(texto()).toContain('Publish offer');
    });

    it('sin RNT, el aviso también está en inglés', () => {
      sesion();
      establecerIdioma('en', { recordar: false });
      fixture = TestBed.createComponent(PanelPrestador);
      fixture.detectChanges();

      expect(texto()).toContain('RNT not registered yet');
      expect(texto()).toContain("You haven't registered your RNT and provider type yet");
    });
  });

  describe('mis ofertas', () => {
    function crear(contenido: OfertaPrestador[]): void {
      sesion('AGENCIA_OPERADORA');
      fixture = TestBed.createComponent(MisOfertas);
      fixture.detectChanges();
      http.expectOne((p) => p.url === LISTA).flush({ ...PAGINA(contenido), tamano: 10 });
      fixture.detectChanges();
    }

    it('la tabla, el estado, el precio, la unidad y el motivo cambian de idioma', () => {
      crear([RECHAZADA]);
      expect(texto()).toContain('Nombre de la oferta');
      expect(texto()).toContain('Rechazada');
      expect(texto()).toContain('Motivo: Faltan fotos');
      expect(texto()).toContain('$ 80.000');

      cambiarA('en');

      expect(raiz().querySelector('h1')?.textContent).toContain('My offers');
      expect(texto()).toContain('Offer name');
      expect(texto()).toContain('Rejected');
      expect(texto()).toContain('Activity');
      expect(texto()).toContain('Reason: Faltan fotos');
      expect(texto()).toContain('COP 80,000 per person');
      expect(texto()).toContain('Your published version is still visible in the catalog.');
      expect(texto()).toContain('View details');
      expect(texto()).toContain('Withdraw');
      expect(raiz().querySelector('caption')?.textContent).toContain('Provider offers, 1 in total');
    });

    it('sin ofertas, el aviso está en inglés', () => {
      crear([]);
      expect(texto()).toContain('Todavía no tienes ofertas');

      cambiarA('en');

      expect(texto()).toContain("You don't have any offers yet");
      expect(raiz().querySelector('a.boton')?.textContent).toContain('Publish offer');
    });

    it('el nombre de respaldo de una oferta sin nombre sigue el idioma', () => {
      crear([{ id: 'b2', tipo: 'RESTAURANTE', retirada: false } as OfertaPrestador]);
      expect(texto()).toContain('Oferta sin nombre');

      cambiarA('en');

      expect(texto()).toContain('Unnamed offer');
    });
  });

  describe('detalle de una oferta', () => {
    it('las dos versiones, las fechas y el estado cambian a inglés', () => {
      sesion('AGENCIA_OPERADORA');
      const f = TestBed.createComponent(DetalleOferta);
      fixture = f;
      f.componentRef.setInput('id', RECHAZADA.id);
      f.detectChanges();
      http.expectOne((p) => p.url === LISTA).flush(PAGINA([RECHAZADA]));
      f.detectChanges();
      expect(texto()).toContain('Versión publicada');
      expect(texto()).toContain('Aún no');

      cambiarA('en');

      expect(texto()).toContain('Published version');
      expect(texto()).toContain('Pending version');
      expect(texto()).toContain('Moderation status');
      expect(texto()).toContain('Approved');
      expect(texto()).toContain('Rejected');
      expect(texto()).toContain('Oct 20, 2026, 10:00 AM');
      expect(texto()).toContain('Not yet');
      expect(texto()).toContain('Reason for rejection');
      expect(texto()).toContain('COP 70,000 per person');
      expect(texto()).toContain('View in the catalog');
    });

    it('una oferta que no es suya: el aviso cambia de idioma', () => {
      sesion('AGENCIA_OPERADORA');
      const f = TestBed.createComponent(DetalleOferta);
      fixture = f;
      f.componentRef.setInput('id', 'ajena');
      f.detectChanges();
      http.expectOne((p) => p.url === LISTA).flush(PAGINA([]));
      f.detectChanges();
      expect(texto()).toContain('No encontramos esta oferta entre las tuyas');

      cambiarA('en');

      expect(texto()).toContain("We couldn't find this offer among yours");
      expect(texto()).toContain('View my offers');
    });
  });

  describe('formulario de la oferta', () => {
    function crear(tipoPrestador: TipoPrestador): void {
      sesion(tipoPrestador);
      fixture = TestBed.createComponent(FormularioOferta);
      fixture.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush([]);
      http.expectOne((p) => p.url === LISTA).flush(PAGINA([]));
      fixture.detectChanges();
    }

    it('el título, las secciones y las ayudas cambian de idioma, con el glosario', () => {
      crear('GUIA_INDEPENDIENTE');
      expect(texto()).toContain('Datos de la actividad');
      expect(texto()).toContain('Como guía independiente');

      cambiarA('en');

      expect(raiz().querySelector('h1')?.textContent).toContain('Publish offer');
      expect(texto()).toContain('General information');
      expect(texto()).toContain('Offer type');
      expect(texto()).toContain('Activity details');
      expect(texto()).toContain('As an independent guide you must complete the tour details.');
      expect(texto()).toContain('Reference price (COP)');
      expect(texto()).toContain('Maximum capacity');
      expect(texto()).toContain('Choose an area');
      expect(texto()).toContain('Service languages');
      expect(texto()).toContain('Wheelchair accessible');
      expect(texto()).toContain('Fields marked with an');
      expect(texto()).toContain('Send to moderation');
      expect(texto()).toContain('Offer photos');
      expect(raiz().querySelector('#latitud')?.getAttribute('placeholder')).toBe('No location');
    });

    it('la unidad del precio en la ayuda sigue el idioma', () => {
      crear('ALOJAMIENTO');
      expect(texto()).toContain('Se muestra por noche y unidad.');

      cambiarA('en');

      expect(texto()).toContain('It is shown per night and unit.');
      expect(texto()).toContain('Accommodation details');
    });

    it('los avisos de campo ya mostrados cambian de idioma sin enviar de nuevo', () => {
      crear('ALOJAMIENTO');
      raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
      fixture.detectChanges();
      expect(texto()).toContain('Este dato es obligatorio.');
      expect(texto()).toContain('Agrega al menos una foto');

      cambiarA('en');

      expect(texto()).toContain('This field is required.');
      expect(texto()).toContain('Add at least one photo: the first one is the cover.');
      expect(texto()).not.toContain('Este dato es obligatorio.');
    });

    it('un prestador sin RNT: el bloqueo cambia de idioma', () => {
      sesion();
      fixture = TestBed.createComponent(FormularioOferta);
      fixture.detectChanges();
      expect(texto()).toContain('Todavía no registras tu RNT');

      cambiarA('en');

      expect(texto()).toContain("You haven't registered your RNT and provider type yet");
      expect(texto()).toContain('Back to my offers');
    });
  });

  describe('selector de fotos', () => {
    function crear(): void {
      sesion('ALOJAMIENTO');
      fixture = TestBed.createComponent(SelectorFotos);
      fixture.detectChanges();
    }

    function elegir(archivo: File): void {
      const entrada = raiz().querySelector<HTMLInputElement>('input[type="file"]')!;
      Object.defineProperty(entrada, 'files', { value: [archivo], configurable: true });
      entrada.dispatchEvent(new Event('change'));
      fixture.detectChanges();
    }

    it('los textos están en inglés', () => {
      establecerIdioma('en', { recordar: false });
      crear();

      expect(texto()).toContain('Offer photos');
      expect(texto()).toContain('Add 1 to 6 photos in JPG, PNG or WebP format, up to 5 MB each.');
      expect(texto()).toContain('Add photos');
    });

    it('el aviso de un archivo rechazado cambia de idioma con la pantalla abierta', () => {
      crear();
      elegir(new File(['x'], 'animada.gif', { type: 'image/gif' }));
      expect(texto()).toContain(
        'No se subió «animada.gif»: solo se aceptan fotos JPG, PNG o WebP.',
      );

      cambiarA('en');

      expect(texto()).toContain(
        'Could not upload “animada.gif”: only JPG, PNG or WebP photos are accepted.',
      );
      expect(texto()).not.toContain('No se subió');
    });
  });
});
