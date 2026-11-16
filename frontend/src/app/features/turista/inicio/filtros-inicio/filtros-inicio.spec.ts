import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { FiltrosInicio, consultaDeEleccion } from './filtros-inicio';

const SIN_ELEGIR = { tipo: null, zonaId: '', rango: 'cualquiera', sillaRuedas: false, bajoEsfuerzo: false } as const;

const ZONAS = [
  { id: 'z1', nombre: 'Taganga', areaProtegida: false, altaAfluencia: true, activa: true, buenasPracticas: [] },
  { id: 'z2', nombre: 'Tayrona', areaProtegida: true, altaAfluencia: false, activa: true, buenasPracticas: [] },
];

describe('consultaDeEleccion', () => {
  it('sin elegir nada no manda ningún parámetro', () => {
    expect(consultaDeEleccion(SIN_ELEGIR)).toEqual({});
  });

  it.each([
    ['hasta', { precioMax: 100000 }],
    ['entre', { precioMin: 100000, precioMax: 300000 }],
    ['mas', { precioMin: 300000 }],
  ] as const)('el rango %s manda solo sus límites', (rango, esperado) => {
    expect(consultaDeEleccion({ ...SIN_ELEGIR, rango })).toEqual(esperado);
  });

  it('arma tipo, zona y accesibilidad con los nombres que entiende el catálogo', () => {
    expect(
      consultaDeEleccion({
        tipo: 'ACTIVIDAD',
        zonaId: 'z1',
        rango: 'entre',
        sillaRuedas: true,
        bajoEsfuerzo: true,
      }),
    ).toEqual({
      tipo: 'ACTIVIDAD',
      zonaId: 'z1',
      precioMin: 100000,
      precioMax: 300000,
      accesibleSillaRuedas: true,
      nivelEsfuerzoMaximo: 'BAJO',
    });
  });

  it('manda solo lo que se eligió', () => {
    expect(consultaDeEleccion({ ...SIN_ELEGIR, bajoEsfuerzo: true })).toEqual({ nivelEsfuerzoMaximo: 'BAJO' });
    expect(consultaDeEleccion({ ...SIN_ELEGIR, tipo: 'PAQUETE' })).toEqual({ tipo: 'PAQUETE' });
  });
});

describe('FiltrosInicio', () => {
  let http: HttpTestingController;

  function crear(zonas: unknown = ZONAS) {
    const fixture = TestBed.createComponent(FiltrosInicio);
    fixture.detectChanges();
    const peticion = http.expectOne('/api/catalogo/zonas');
    if (zonas === 'error') {
      peticion.error(new ProgressEvent('error'));
    } else {
      peticion.flush(zonas as object);
    }
    fixture.detectChanges();
    return fixture;
  }

  const raiz = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const botones = (f: { nativeElement: unknown }, selector: string) =>
    Array.from(raiz(f).querySelectorAll<HTMLButtonElement>(selector));
  const verOfertas = (f: { nativeElement: unknown }) => raiz(f).querySelector<HTMLAnchorElement>('.filtros__ver')!;

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('sin elegir nada, "Ver ofertas" lleva a /catalogo sin parámetros', () => {
    const fixture = crear();

    expect(verOfertas(fixture).getAttribute('href')).toBe('/catalogo');
  });

  it('ofrece los cinco tipos, con "Todos" marcado al inicio', () => {
    const fixture = crear();
    const tipos = botones(fixture, '.seg');

    expect(tipos.map((b) => b.textContent?.trim())).toEqual([
      'Todos',
      'Actividades',
      'Alojamientos',
      'Restaurantes',
      'Paquetes',
    ]);
    expect(tipos.map((b) => b.getAttribute('aria-pressed'))).toEqual(['true', 'false', 'false', 'false', 'false']);
  });

  it('elegir un tipo lo marca y lo manda; volver a "Todos" lo quita', () => {
    const fixture = crear();

    botones(fixture, '.seg')[1].click();
    fixture.detectChanges();
    expect(botones(fixture, '.seg')[1].getAttribute('aria-pressed')).toBe('true');
    expect(botones(fixture, '.seg')[0].getAttribute('aria-pressed')).toBe('false');
    expect(verOfertas(fixture).getAttribute('href')).toBe('/catalogo?tipo=ACTIVIDAD');

    botones(fixture, '.seg')[0].click();
    fixture.detectChanges();
    expect(verOfertas(fixture).getAttribute('href')).toBe('/catalogo');
  });

  it('el selector de zona trae las zonas de GET /zonas y manda la elegida', () => {
    const fixture = crear();
    const selector = raiz(fixture).querySelector<HTMLSelectElement>('#filtro-zona')!;

    expect(Array.from(selector.options).map((o) => o.textContent?.trim())).toEqual([
      'Todas las zonas',
      'Taganga',
      'Tayrona',
    ]);
    selector.value = 'z2';
    selector.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(verOfertas(fixture).getAttribute('href')).toBe('/catalogo?zonaId=z2');
  });

  it('si fallan las zonas, el selector ofrece solo "Todas las zonas"', () => {
    const fixture = crear('error');

    const selector = raiz(fixture).querySelector<HTMLSelectElement>('#filtro-zona')!;
    expect(selector.options).toHaveLength(1);
  });

  it('el rango de precio se escribe con formato en español y en inglés, y manda sus límites', () => {
    const fixture = crear();
    const selector = raiz(fixture).querySelector<HTMLSelectElement>('#filtro-precio')!;
    const textos = () => Array.from(selector.options).map((o) => o.textContent?.replace(/\s+/g, ' ').trim());

    expect(textos()).toEqual(['Cualquier precio', 'Hasta $ 100.000', '$ 100.000 a $ 300.000', 'Más de $ 300.000']);

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();
    expect(textos()).toEqual(['Any price', 'Up to COP 100,000', 'COP 100,000 to COP 300,000', 'Over COP 300,000']);

    selector.value = 'entre';
    selector.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(verOfertas(fixture).getAttribute('href')).toBe('/catalogo?precioMin=100000&precioMax=300000');
  });

  it('los botones de accesibilidad usan aria-pressed y mandan sus parámetros', () => {
    const fixture = crear();
    const [silla, esfuerzo] = botones(fixture, '.chip');

    expect([silla.getAttribute('aria-pressed'), esfuerzo.getAttribute('aria-pressed')]).toEqual(['false', 'false']);
    silla.click();
    esfuerzo.click();
    fixture.detectChanges();

    expect([silla.getAttribute('aria-pressed'), esfuerzo.getAttribute('aria-pressed')]).toEqual(['true', 'true']);
    expect(verOfertas(fixture).getAttribute('href')).toBe(
      '/catalogo?accesibleSillaRuedas=true&nivelEsfuerzoMaximo=BAJO',
    );

    silla.click();
    fixture.detectChanges();
    expect(silla.getAttribute('aria-pressed')).toBe('false');
    expect(verOfertas(fixture).getAttribute('href')).toBe('/catalogo?nivelEsfuerzoMaximo=BAJO');
  });
});
