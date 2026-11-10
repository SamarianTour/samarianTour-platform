import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { Ficha } from './ficha';

function ficha(altaAfluencia: boolean) {
  return {
    id: 'o1',
    tipo: 'ACTIVIDAD',
    nombre: 'Caminata a Cabo San Juan',
    descripcion: 'Un recorrido por el parque.',
    zonaId: 'z1',
    precioReferencia: 150000,
    unidadPrecio: 'por persona',
    portada: null,
    fotos: [],
    capacidadMaxima: 1,
    accesibleSillaRuedas: false,
    zona: { id: 'z1', nombre: 'Parque Tayrona', altaAfluencia, areaProtegida: true, buenasPracticas: [] },
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
}

const ALTERNATIVAS = {
  aplica: true,
  zonaOrigen: { id: 'z1', nombre: 'Parque Tayrona' },
  alternativas: [
    {
      posicion: 1,
      oferta: { id: 'b1', nombre: 'Pozo Azul', tipo: 'ACTIVIDAD', zonaNombre: 'Minca', precioReferencia: 95000, unidadPrecio: 'por persona' },
      criterios: ['TIPO_OFERTA'],
      explicacion: 'Mismo tipo de oferta en Minca, una zona con menos visitantes.',
    },
    {
      posicion: 2,
      oferta: { id: 'b2', nombre: 'Aves en Minca', tipo: 'ACTIVIDAD', zonaNombre: 'Minca', precioReferencia: 120000, unidadPrecio: 'por persona' },
      criterios: ['TIPO_OFERTA'],
      explicacion: 'Mismo tipo de oferta en Minca, una zona con menos visitantes.',
    },
  ],
};

const ALERTAS = { zonaId: 'z1', actualizadoEn: '2026-10-24T23:42:10.512Z', alertas: [] };

const URL_ALERTAS = '/api/recomendacion/zonas/z1/alertas';
const URL_ALTERNATIVAS = '/api/recomendacion/ofertas/o1/alternativas';

describe('Ficha de la oferta: alternativas en zona de alta afluencia (P08.3)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<Ficha>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const alternativas = () => raiz().querySelector('app-alternativas-oferta section');

  function abrir(altaAfluencia: boolean): void {
    fixture = TestBed.createComponent(Ficha);
    fixture.componentRef.setInput('id', 'o1');
    fixture.detectChanges();
    http.expectOne('/api/catalogo/ofertas/o1').flush(ficha(altaAfluencia));
    fixture.detectChanges();
    http
      .match((p) => p.url.includes('/opiniones'))
      .forEach((p) =>
        p.flush({ resumen: { totalOpiniones: 0 }, contenido: [], pagina: 0, tamano: 5, totalElementos: 0, totalPaginas: 0 }),
      );
    fixture.detectChanges();
  }

  const fallo503 = [
    { status: 503, codigo: 'RECOMENDACION_NO_DISPONIBLE', title: 'x', detail: 'x' },
    { status: 503, statusText: 'x', headers: { 'Content-Type': 'application/problem+json' } },
  ] as const;

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

  it('en una zona de alta afluencia pide las alternativas de la oferta y las muestra', () => {
    abrir(true);

    http.expectOne((p) => p.url === URL_ALERTAS).flush(ALERTAS);
    http.expectOne((p) => p.url === URL_ALTERNATIVAS).flush(ALTERNATIVAS);
    fixture.detectChanges();

    expect(alternativas()).not.toBeNull();
    expect(alternativas()?.querySelectorAll('app-tarjeta-recomendada')).toHaveLength(2);
    const titulos = Array.from(raiz().querySelectorAll('.ficha__principal h2')).map((h) => h.textContent?.trim());
    expect(titulos.indexOf('Alternativas con menos visitantes')).toBe(titulos.indexOf('Alertas del destino') + 1);
  });

  it('en una zona que no es de alta afluencia ni siquiera las pide', () => {
    abrir(false);

    expect(http.match((p) => p.url.includes('/alternativas'))).toHaveLength(0);
    expect(alternativas()).toBeNull();
    expect(texto()).toContain('Sobre la actividad');
    http.expectOne((p) => p.url === URL_ALERTAS);
  });

  it('si el servicio de recomendación no está (503), se ocultan las dos secciones y la ficha sigue igual', () => {
    abrir(true);

    http.expectOne((p) => p.url === URL_ALERTAS).flush(...fallo503);
    http.expectOne((p) => p.url === URL_ALTERNATIVAS).flush(...fallo503);
    fixture.detectChanges();

    expect(alternativas()).toBeNull();
    expect(raiz().querySelector('app-alertas-zona section')).toBeNull();
    expect(raiz().querySelector('[role="alert"]')).toBeNull();
    expect(texto()).toContain('Caminata a Cabo San Juan');
    expect(texto()).toContain('Sobre la actividad');
    expect(texto()).toContain('Zona: Parque Tayrona');
    expect(raiz().querySelector('app-opiniones-oferta')).not.toBeNull();
    expect(raiz().querySelector('a[href="/turista/solicitar/o1"]')).not.toBeNull();
  });

  it('las secciones son independientes: si fallan las alertas, las alternativas se ven', () => {
    abrir(true);

    http.expectOne((p) => p.url === URL_ALERTAS).flush(...fallo503);
    http.expectOne((p) => p.url === URL_ALTERNATIVAS).flush(ALTERNATIVAS);
    fixture.detectChanges();

    expect(raiz().querySelector('app-alertas-zona section')).toBeNull();
    expect(alternativas()).not.toBeNull();
  });

  it('si las alternativas tardan más de 3 s, se ocultan sin esperar', () => {
    vi.useFakeTimers();
    abrir(true);
    http.expectOne((p) => p.url === URL_ALERTAS).flush(ALERTAS);
    const peticion = http.expectOne((p) => p.url === URL_ALTERNATIVAS);

    vi.advanceTimersByTime(3_000);
    fixture.detectChanges();

    expect(peticion.cancelled).toBe(true);
    expect(alternativas()).toBeNull();
    expect(texto()).toContain('Sobre la actividad');
  });

  it('si el servicio responde que no aplica, la sección no se dibuja', () => {
    abrir(true);

    http.expectOne((p) => p.url === URL_ALERTAS).flush(ALERTAS);
    http.expectOne((p) => p.url === URL_ALTERNATIVAS).flush({ aplica: false, alternativas: [] });
    fixture.detectChanges();

    expect(alternativas()).toBeNull();
  });
});
