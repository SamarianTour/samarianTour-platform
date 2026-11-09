import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { erroresInterceptor } from '../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../core/sesion/testing';
import { Catalogo } from './catalogo/catalogo';
import { Ficha } from './ficha/ficha';
import { OpinionesDeLaOferta } from './ficha/opiniones/opiniones-oferta';
import { MisOpiniones } from './mis-opiniones/mis-opiniones';

const ZONAS = [
  {
    id: 'z1',
    nombre: 'Taganga',
    altaAfluencia: true,
    areaProtegida: false,
    buenasPracticas: [],
  },
  {
    id: 'z2',
    nombre: 'Tayrona',
    altaAfluencia: false,
    areaProtegida: true,
    buenasPracticas: [],
  },
];

const OFERTA = {
  id: 'o1',
  tipo: 'ACTIVIDAD',
  nombre: 'Caminata a Marinka',
  descripcion: 'Un recorrido por la sierra.',
  zonaId: 'z1',
  precioReferencia: 150000,
  unidadPrecio: 'por persona',
  portada: null,
};

const FICHA = {
  ...OFERTA,
  fotos: [],
  capacidadMaxima: 1,
  accesibleSillaRuedas: false,
  zona: {
    ...ZONAS[1],
    descripcion: 'Parque.',
    condicionesVisita: 'Entrada con reserva.',
    buenasPracticas: ['No dejes basura'],
  },
  actividad: {
    tipoActividad: 'SENDERISMO',
    nivelEsfuerzo: 'MODERADO',
    horariosSalida: ['08:00'],
    duracionMinutos: 150,
    modalidad: 'CAMINATA',
    idiomasAtencion: ['es', 'en'],
  },
  perfilPrestador: { nombreComercial: null, descripcion: 'Guías locales.', redesSociales: [] },
};

describe('turista en español e inglés', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<unknown>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const cambiarA = (idioma: 'es' | 'en') => {
    establecerIdioma(idioma, { recordar: false });
    fixture.detectChanges();
  };

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

  describe('catálogo', () => {
    function crear(zonaElegida = false): void {
      const f = TestBed.createComponent(Catalogo);
      fixture = f;
      f.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush(ZONAS);
      http
        .expectOne((p) => p.url === '/api/catalogo/ofertas')
        .flush({
          contenido: [OFERTA, { ...OFERTA, id: 'o2', nombre: 'Buceo' }],
          pagina: 0,
          tamano: 12,
          totalElementos: 2,
          totalPaginas: 1,
        });
      f.detectChanges();
      expect(zonaElegida).toBe(false);
    }

    it('en español dice lo de siempre', () => {
      crear();

      expect(texto()).toContain('Catálogo turístico');
      expect(texto()).toContain('2 ofertas encontradas');
      expect(texto()).toContain('Todas las zonas');
      expect(texto()).toContain('RNT verificado');
      expect(texto()).toContain('$ 150.000 por persona');
    });

    it('cambia a inglés con la pantalla abierta: títulos, filtros, resultados, precio y unidad', () => {
      crear();

      cambiarA('en');

      expect(raiz().querySelector('h1')?.textContent).toContain('Tourism catalog');
      expect(texto()).toContain('2 offers found');
      expect(texto()).toContain('Offer type');
      expect(texto()).toContain('All areas');
      expect(texto()).toContain('Maximum physical effort');
      expect(texto()).toContain('Up to moderate effort');
      expect(texto()).toContain('Wheelchair accessible');
      expect(texto()).toContain('RNT verified');
      expect(texto()).toContain('COP 150,000 per person');
      expect(texto()).toContain('Activity');
      expect(texto()).not.toContain('Filtros');
    });

    it('usa el singular y el plural en cada idioma', () => {
      const f = TestBed.createComponent(Catalogo);
      fixture = f;
      f.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush(ZONAS);
      http
        .expectOne((p) => p.url === '/api/catalogo/ofertas')
        .flush({ contenido: [OFERTA], pagina: 0, tamano: 12, totalElementos: 1, totalPaginas: 1 });
      f.detectChanges();
      expect(texto()).toContain('1 oferta encontrada');

      cambiarA('en');

      expect(texto()).toContain('1 offer found');
    });

    it('sin resultados, el aviso también cambia de idioma', () => {
      const f = TestBed.createComponent(Catalogo);
      fixture = f;
      f.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush(ZONAS);
      http
        .expectOne((p) => p.url === '/api/catalogo/ofertas')
        .flush({ contenido: [], pagina: 0, tamano: 12, totalElementos: 0, totalPaginas: 0 });
      f.detectChanges();
      expect(texto()).toContain('No encontramos ofertas con esos filtros');

      cambiarA('en');

      expect(texto()).toContain("We couldn't find any offers with those filters");
      expect(texto()).toContain('Clear filters');
    });

    it('un error de la API que ya se mostró cambia de idioma', () => {
      const f = TestBed.createComponent(Catalogo);
      fixture = f;
      f.detectChanges();
      http.expectOne('/api/catalogo/zonas').flush(ZONAS);
      http
        .expectOne((p) => p.url === '/api/catalogo/ofertas')
        .flush(
          { status: 503, codigo: 'CATALOGO_NO_DISPONIBLE' },
          { status: 503, statusText: 'Unavailable' },
        );
      f.detectChanges();
      expect(texto()).toContain('El catálogo no responde en este momento.');

      cambiarA('en');

      expect(texto()).toContain("The catalog isn't responding right now.");
    });
  });

  describe('ficha de la oferta', () => {
    function crear(): void {
      const f = TestBed.createComponent(Ficha);
      fixture = f;
      f.componentRef.setInput('id', 'o1');
      f.detectChanges();
      http.expectOne('/api/catalogo/ofertas/o1').flush(FICHA);
      f.detectChanges();
      http
        .match((p) => p.url.includes('/opiniones'))
        .forEach((p) =>
          p.flush({
            resumen: { totalOpiniones: 0 },
            contenido: [],
            pagina: 0,
            tamano: 5,
            totalElementos: 0,
            totalPaginas: 0,
          }),
        );
      f.detectChanges();
    }

    it('en español muestra la ficha como siempre', () => {
      crear();

      expect(texto()).toContain('Sobre la actividad');
      expect(texto()).toContain('Capacidad máxima: 1 persona');
      expect(texto()).toContain('Prestador local');
      expect(texto()).toContain('Español, Inglés');
    });

    it('cambia a inglés con la ficha abierta, con el glosario y los plurales', () => {
      crear();

      cambiarA('en');

      expect(texto()).toContain('About the activity');
      expect(texto()).toContain('Physical effort');
      expect(texto()).toContain('Moderate');
      expect(texto()).toContain('Format');
      expect(texto()).toContain('Walking');
      expect(texto()).toContain('Spanish, English');
      expect(texto()).toContain('Protected area.');
      expect(texto()).toContain('Maximum capacity: 1 person');
      expect(texto()).toContain('Local provider');
      expect(texto()).toContain('Not wheelchair accessible');
      expect(texto()).toContain('Reference price');
      expect(texto()).toContain('COP 150,000 per person');
      expect(texto()).toContain('Area: Tayrona');
      expect(texto()).toContain('Good practices');
      expect(texto()).toContain('Moderate effort');
      expect(raiz().querySelector('nav.migas')?.getAttribute('aria-label')).toBe('Breadcrumb');
    });

    it('una oferta que no existe: el aviso cambia de idioma', () => {
      const f = TestBed.createComponent(Ficha);
      fixture = f;
      f.componentRef.setInput('id', 'nada');
      f.detectChanges();
      http
        .expectOne('/api/catalogo/ofertas/nada')
        .flush(
          { status: 404, codigo: 'OFERTA_NO_ENCONTRADA' },
          { status: 404, statusText: 'Not Found' },
        );
      f.detectChanges();
      expect(texto()).toContain('Esta oferta ya no está disponible');

      cambiarA('en');

      expect(texto()).toContain('This offer is no longer available');
      expect(texto()).toContain('View the catalog');
    });
  });

  describe('opiniones', () => {
    const LECTURA = {
      resumen: { totalOpiniones: 2, promedioCalificacion: 4.5 },
      contenido: [
        { id: 'p1', calificacion: 5, comentario: 'Excelente guía.', fecha: '2026-10-28' },
        { id: 'p2', calificacion: 4, comentario: 'Muy bien.', fecha: '2026-10-20' },
      ],
      pagina: 0,
      tamano: 5,
      totalElementos: 2,
      totalPaginas: 1,
    };

    function crear(conSesion: boolean): void {
      if (conSesion) {
        localStorage.setItem(
          CLAVE_SESION,
          JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
        );
      }
      const f = TestBed.createComponent(OpinionesDeLaOferta);
      fixture = f;
      f.componentRef.setInput('ofertaId', 'o1');
      f.detectChanges();
      http.expectOne((p) => p.url === '/api/catalogo/ofertas/o1/opiniones').flush(LECTURA);
      f.detectChanges();
    }

    it('el resumen, el promedio, las fechas y el formulario cambian a inglés', () => {
      crear(true);
      expect(texto()).toContain('2 opiniones de turistas que la pagaron');
      expect(texto()).toContain('4,5');
      expect(texto()).toContain('28 oct 2026');

      cambiarA('en');

      expect(raiz().querySelector('h2')?.textContent).toContain('Reviews');
      expect(texto()).toContain('2 reviews from tourists who paid for it');
      expect(texto()).toContain('4.5');
      expect(texto()).toContain('Oct 28, 2026');
      expect(texto()).toContain('Tourist');
      expect(texto()).toContain('Leave a review as Laura');
      expect(texto()).toContain('Rating');
      expect(texto()).toContain('Not rated');
      expect(texto()).toContain('Submit review');
      expect(texto()).toContain('10 to 1000 characters (0)');
    });

    it('las estrellas se dicen en singular y plural', () => {
      crear(true);
      cambiarA('en');

      const textos = Array.from(
        raiz().querySelectorAll('.calificacion__opcion .solo-lectores'),
      ).map((e) => e.textContent);
      expect(textos).toEqual(['1 star', '2 stars', '3 stars', '4 stars', '5 stars']);
    });

    it('los avisos de campo cambian de idioma sin enviar de nuevo', () => {
      crear(true);
      raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
      fixture.detectChanges();
      expect(texto()).toContain('Elige de 1 a 5 estrellas.');
      expect(texto()).toContain('Escribe tu comentario.');

      cambiarA('en');

      expect(texto()).toContain('Choose 1 to 5 stars.');
      expect(texto()).toContain('Write your comment.');
    });

    it('sin sesión, la invitación a iniciar sesión está en inglés', () => {
      crear(false);

      cambiarA('en');

      expect(texto()).toContain('To leave a review,');
      expect(raiz().querySelector('a[href^="/iniciar-sesion"]')?.textContent).toContain('log in');
      expect(texto()).toContain('as a tourist.');
    });
  });

  describe('mis opiniones', () => {
    function crear(): void {
      localStorage.setItem(
        CLAVE_SESION,
        JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'TURISTA' }))),
      );
      const f = TestBed.createComponent(MisOpiniones);
      fixture = f;
      f.detectChanges();
      http
        .expectOne((p) => p.url.includes('/opiniones'))
        .flush({
          contenido: [
            {
              id: 'p1',
              ofertaId: 'o1',
              nombreOferta: null,
              calificacion: 3,
              comentario: 'Regular.',
              fecha: '2026-10-28',
              estado: 'RECHAZADA',
              motivoRechazo: 'Contiene datos de contacto',
            },
          ],
          pagina: 0,
          tamano: 10,
          totalElementos: 1,
          totalPaginas: 1,
        });
      f.detectChanges();
    }

    it('en español y en inglés: estado, fecha, oferta y motivo', () => {
      crear();
      expect(texto()).toContain('Rechazada');
      expect(texto()).toContain('Enviada el 28 oct 2026');
      expect(texto()).toContain('Motivo del rechazo:');

      cambiarA('en');

      expect(raiz().querySelector('h1')?.textContent).toContain('My reviews');
      expect(texto()).toContain('Rejected');
      expect(texto()).toContain('Submitted on Oct 28, 2026');
      expect(texto()).toContain('Reason for rejection:');
      // El motivo lo escribe el administrador: llega en español y se muestra tal cual.
      expect(texto()).toContain('Contiene datos de contacto');
      expect(texto()).toContain('Offer');
    });
  });
});
