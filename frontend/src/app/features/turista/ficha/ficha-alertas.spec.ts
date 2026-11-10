import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { Ficha } from './ficha';

const FICHA = {
  id: 'o1',
  tipo: 'ACTIVIDAD',
  nombre: 'Caminata a Marinka',
  descripcion: 'Un recorrido por la sierra.',
  zonaId: 'z2',
  precioReferencia: 150000,
  unidadPrecio: 'por persona',
  portada: null,
  fotos: [],
  capacidadMaxima: 1,
  accesibleSillaRuedas: false,
  zona: {
    id: 'z2',
    nombre: 'Tayrona',
    altaAfluencia: false,
    areaProtegida: true,
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
    idiomasAtencion: ['es'],
  },
  perfilPrestador: { nombreComercial: null, descripcion: 'Guías locales.', redesSociales: [] },
};

const ALERTAS = {
  zonaId: 'z2',
  actualizadoEn: '2026-10-24T23:42:10.512Z',
  alertas: [
    {
      id: 's1',
      tipo: 'CIERRE',
      nivel: 'NARANJA',
      titulo: 'Cierre temporal del parque',
      descripcion: 'Consulta la fecha de reapertura antes de viajar.',
      fuente: 'Parques Nacionales (simulado)',
    },
  ],
};

const URL_ALERTAS = '/api/recomendacion/zonas/z2/alertas';

describe('Ficha de la oferta: alertas de su zona (P08.2)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<Ficha>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const seccionDeAlertas = () => raiz().querySelector('app-alertas-zona section');

  /** Abre la ficha; las opiniones se responden vacías para que no estorben. */
  function abrir(ficha: object = FICHA): void {
    fixture = TestBed.createComponent(Ficha);
    fixture.componentRef.setInput('id', 'o1');
    fixture.detectChanges();
    http.expectOne('/api/catalogo/ofertas/o1').flush(ficha);
    fixture.detectChanges();
    http
      .match((p) => p.url.includes('/opiniones'))
      .forEach((p) =>
        p.flush({ resumen: { totalOpiniones: 0 }, contenido: [], pagina: 0, tamano: 5, totalElementos: 0, totalPaginas: 0 }),
      );
    fixture.detectChanges();
  }

  beforeEach(() => {
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
  });

  afterEach(() => {
    vi.useRealTimers();
    establecerIdioma('es', { recordar: false });
    http.match(() => true).forEach((p) => p.error(new ProgressEvent('error')));
  });

  it('pide las alertas de la zona de la oferta y las muestra después de la información de la zona', () => {
    abrir();

    http.expectOne((p) => p.url === URL_ALERTAS).flush(ALERTAS);
    fixture.detectChanges();

    expect(seccionDeAlertas()).not.toBeNull();
    expect(seccionDeAlertas()?.textContent).toContain('Cierre temporal del parque');
    expect(seccionDeAlertas()?.textContent).toContain('Parques Nacionales (simulado)');
    const titulos = Array.from(raiz().querySelectorAll('.ficha__principal h2')).map((h) => h.textContent?.trim());
    expect(titulos.indexOf('Alertas del destino')).toBe(titulos.indexOf('Zona: Tayrona') + 1);
  });

  it('una zona sin alertas dice "Sin alertas activas" con la hora', () => {
    abrir();

    http.expectOne((p) => p.url === URL_ALERTAS).flush({ ...ALERTAS, alertas: [] });
    fixture.detectChanges();

    expect(texto()).toContain('Sin alertas activas');
    expect(seccionDeAlertas()?.querySelector('time')).not.toBeNull();
  });

  it('si el servicio de alertas no está (503), la sección se oculta y el resto de la ficha sigue igual', () => {
    abrir();

    http.expectOne((p) => p.url === URL_ALERTAS).flush(
      { status: 503, codigo: 'ALERTAS_NO_DISPONIBLES', title: 'x', detail: 'x' },
      { status: 503, statusText: 'x', headers: { 'Content-Type': 'application/problem+json' } },
    );
    fixture.detectChanges();

    expect(seccionDeAlertas()).toBeNull();
    expect(raiz().querySelector('[role="alert"]')).toBeNull();
    expect(texto()).toContain('Caminata a Marinka');
    expect(texto()).toContain('Sobre la actividad');
    expect(texto()).toContain('Zona: Tayrona');
    expect(texto()).toContain('No dejes basura');
    expect(raiz().querySelector('app-opiniones-oferta')).not.toBeNull();
    expect(raiz().querySelector('a[href="/turista/solicitar/o1"]')).not.toBeNull();
  });

  it('si las alertas tardan más de 3 s, se ocultan y la ficha no las espera', () => {
    vi.useFakeTimers();
    abrir();
    const peticion = http.expectOne((p) => p.url === URL_ALERTAS);

    vi.advanceTimersByTime(3_000);
    fixture.detectChanges();

    expect(peticion.cancelled).toBe(true);
    expect(seccionDeAlertas()).toBeNull();
    expect(texto()).toContain('Sobre la actividad');
  });

  it('las alertas no retrasan la ficha: esta se ve aunque la respuesta de alertas aún no llegue', () => {
    abrir();

    expect(texto()).toContain('Sobre la actividad');
    expect(seccionDeAlertas()).toBeNull();
    http.expectOne((p) => p.url === URL_ALERTAS);
  });

  it('una oferta sin zona no pide alertas', () => {
    abrir({ ...FICHA, zona: undefined });

    expect(http.match((p) => p.url.includes('/alertas'))).toHaveLength(0);
    expect(texto()).toContain('Sobre la actividad');
  });

  it('la sección lleva a la página de alertas del destino', () => {
    abrir();

    http.expectOne((p) => p.url === URL_ALERTAS).flush(ALERTAS);
    fixture.detectChanges();

    expect(seccionDeAlertas()?.querySelector('a[href="/alertas"]')).not.toBeNull();
  });
});
