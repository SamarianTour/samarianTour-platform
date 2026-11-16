import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { RUTAS_TURISTA } from '../turista.routes';
import { Catalogo } from './catalogo';

const PAGINA_VACIA = { contenido: [], pagina: 0, tamano: 12, totalElementos: 0, totalPaginas: 0 };

describe('Catálogo: filtros plegables en celular', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement!;
  const boton = () => raiz().querySelector<HTMLButtonElement>('.filtros__alternar')!;
  const panel = () => raiz().querySelector<HTMLElement>('#filtros-catalogo')!;
  const texto = () => boton().textContent?.replace(/\s+/g, ' ').trim();
  const ofertas = () => http.match((p) => p.url === '/api/catalogo/ofertas');

  async function abrir(url: string): Promise<void> {
    await harness.navigateByUrl(url, Catalogo);
    ofertas().forEach((p) => p.flush(PAGINA_VACIA));
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [provideRouter(RUTAS_TURISTA), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    http.match(() => true).forEach((p) => p.error(new ProgressEvent('error')));
    establecerIdioma('es', { recordar: false });
  });

  it('empieza cerrado: el botón controla el panel y el buscador queda fuera de él', async () => {
    await abrir('/catalogo');

    expect(boton().getAttribute('aria-controls')).toBe('filtros-catalogo');
    expect(boton().getAttribute('aria-expanded')).toBe('false');
    expect(panel().classList.contains('abierto')).toBe(false);
    expect(texto()).toBe('Filtrar ofertas');
    expect(panel().querySelector('#texto')).toBeNull();
    expect(raiz().querySelector('form #texto')).not.toBeNull();
    // Todos los demás filtros están dentro del panel.
    for (const id of ['zonaId', 'tipoActividad', 'precioMin', 'precioMax', 'nivelEsfuerzoMaximo']) {
      expect(panel().querySelector(`#${id}`)).not.toBeNull();
    }
    expect(panel().querySelector('input[type="radio"]')).not.toBeNull();
    expect(panel().querySelector('input[type="checkbox"]')).not.toBeNull();
  });

  it('al tocarlo abre y pasa a "Ocultar filtros"; al volver a tocarlo cierra, sin cambiar la URL', async () => {
    await abrir('/catalogo');
    const url = TestBed.inject(Router).url;

    boton().click();
    harness.detectChanges();
    expect(boton().getAttribute('aria-expanded')).toBe('true');
    expect(panel().classList.contains('abierto')).toBe(true);
    expect(texto()).toBe('Ocultar filtros');

    boton().click();
    harness.detectChanges();
    expect(boton().getAttribute('aria-expanded')).toBe('false');
    expect(panel().classList.contains('abierto')).toBe(false);
    expect(TestBed.inject(Router).url).toBe(url);
  });

  it('el contador cuenta los filtros de la URL y no cuenta texto ni página', async () => {
    await abrir('/catalogo?texto=tayrona&page=2&zonaId=a00000000000000000000005&precioMin=50000&precioMax=90000&accesibleSillaRuedas=true');

    expect(texto()).toBe('Filtrar ofertas (3)');
  });

  it('en inglés dice "Filter offers (N)" y "Hide filters"', async () => {
    await abrir('/catalogo?zonaId=a00000000000000000000005');
    establecerIdioma('en', { recordar: false });
    harness.detectChanges();
    expect(texto()).toBe('Filter offers (1)');

    boton().click();
    harness.detectChanges();
    expect(texto()).toBe('Hide filters');
  });

  it('"Limpiar filtros" deja el contador en 0 y los filtros se siguen aplicando al instante', async () => {
    await abrir('/catalogo?tipoActividad=SENDERISMO&nivelEsfuerzoMaximo=BAJO');
    expect(texto()).toBe('Filtrar ofertas (2)');
    boton().click();
    harness.detectChanges();

    panel().querySelector<HTMLButtonElement>('button.boton--ancho')!.click();
    await harness.fixture.whenStable();
    ofertas().forEach((p) => p.flush(PAGINA_VACIA));
    harness.detectChanges();

    expect(TestBed.inject(Router).url).toBe('/catalogo');
    boton().click();
    harness.detectChanges();
    expect(texto()).toBe('Filtrar ofertas');
  });

  it('elegir un filtro en el panel cambia la URL y sube el contador', async () => {
    await abrir('/catalogo');
    const zona = panel().querySelector<HTMLSelectElement>('#nivelEsfuerzoMaximo')!;

    zona.value = 'BAJO';
    zona.dispatchEvent(new Event('change'));
    // El formulario espera 350 ms antes de cambiar la URL.
    await new Promise((resolver) => setTimeout(resolver, 450));
    ofertas().forEach((p) => p.flush(PAGINA_VACIA));
    harness.detectChanges();

    expect(TestBed.inject(Router).url).toBe('/catalogo?nivelEsfuerzoMaximo=BAJO');
    expect(texto()).toBe('Filtrar ofertas (1)');
  });

  it('/paquetes también lo tiene, y el tipo que fija la ruta no cuenta', async () => {
    await abrir('/paquetes');
    expect(boton()).not.toBeNull();
    expect(panel()).not.toBeNull();
    expect(texto()).toBe('Filtrar ofertas');

    await abrir('/paquetes?zonaId=a00000000000000000000005');
    expect(texto()).toBe('Filtrar ofertas (1)');
  });

  it('un error en el precio muestra el panel para que se vea, aunque esté cerrado', async () => {
    await harness.navigateByUrl('/catalogo?precioMin=500&precioMax=100', Catalogo);
    ofertas().forEach((p) =>
      p.flush(
        { codigo: 'DATO_INVALIDO', detail: 'precioMin: no puede superar al máximo' },
        { status: 400, statusText: 'Bad Request' },
      ),
    );
    harness.detectChanges();

    expect(raiz().querySelector('#precioMin-error')).not.toBeNull();
    expect(panel().classList.contains('abierto')).toBe(true);
    expect(boton().getAttribute('aria-expanded')).toBe('true');
  });
});
