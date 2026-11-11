import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { RUTAS_TURISTA } from '../turista.routes';
import { Catalogo } from './catalogo';
import { SIN_FILTROS, hayFiltros } from './filtros';

const PAGINA_VACIA = { contenido: [], pagina: 0, tamano: 12, totalElementos: 0, totalPaginas: 0 };

describe('Catálogo de paquetes (P01.3)', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement!;
  const ofertas = () => http.match((p) => p.url === '/api/catalogo/ofertas');

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter(RUTAS_TURISTA), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.match(() => true).forEach((p) => p.error(new ProgressEvent('error'))));

  it('"/paquetes" crea el catálogo y pide solo paquetes', async () => {
    const componente = await harness.navigateByUrl('/paquetes', Catalogo);

    expect(componente).toBeInstanceOf(Catalogo);
    expect(ofertas().every((p) => p.request.params.get('tipo') === 'PAQUETE')).toBe(true);
  });

  it('tiene su propio título y no ofrece elegir el tipo de oferta', async () => {
    await harness.navigateByUrl('/paquetes', Catalogo);
    ofertas().forEach((p) => p.flush(PAGINA_VACIA));
    harness.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toContain('Catálogo de paquetes');
    expect(raiz().querySelector('input[type="radio"]')).toBeNull();
    expect(TestBed.inject(Router).url).toBe('/paquetes');
  });

  it('ignora un tipo distinto en la URL: sigue mostrando paquetes', async () => {
    await harness.navigateByUrl('/paquetes?tipo=RESTAURANTE', Catalogo);

    expect(ofertas().every((p) => p.request.params.get('tipo') === 'PAQUETE')).toBe(true);
  });

  it('conserva los demás filtros de la URL y no repite el tipo en ella', async () => {
    await harness.navigateByUrl('/paquetes?zonaId=a00000000000000000000005&texto=tayrona', Catalogo);
    const peticiones = ofertas();

    expect(peticiones[0].request.params.get('zonaId')).toBe('a00000000000000000000005');
    expect(peticiones[0].request.params.get('texto')).toBe('tayrona');
    expect(TestBed.inject(Router).url).not.toContain('tipo=');
  });

  it('el catálogo normal sigue ofreciendo el tipo de oferta', async () => {
    await harness.navigateByUrl('/catalogo', Catalogo);
    ofertas().forEach((p) => p.flush(PAGINA_VACIA));
    harness.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toContain('Catálogo turístico');
    expect(raiz().querySelector('input[type="radio"]')).not.toBeNull();
  });
});

describe('hayFiltros con el tipo fijo de la ruta', () => {
  it('el tipo que fija la ruta no cuenta como filtro, pero otro filtro sí', () => {
    expect(hayFiltros({ ...SIN_FILTROS, tipo: 'PAQUETE' }, 'PAQUETE')).toBe(false);
    expect(hayFiltros({ ...SIN_FILTROS, tipo: 'PAQUETE', zonaId: 'a1' }, 'PAQUETE')).toBe(true);
    expect(hayFiltros({ ...SIN_FILTROS, tipo: 'PAQUETE' })).toBe(true);
  });
});
