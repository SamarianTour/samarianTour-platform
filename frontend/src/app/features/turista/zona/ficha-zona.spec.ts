import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { RUTAS_TURISTA } from '../turista.routes';
import { FichaZona } from './ficha-zona';

const ZONA = {
  id: 'z2',
  nombre: 'Parque Tayrona',
  descripcion: 'Selva y mar.',
  areaProtegida: true,
  altaAfluencia: true,
  activa: true,
  buenasPracticas: ['No dejes basura', 'Lleva tu propia agua'],
  condicionesVisita: 'Entrada con cupo diario.',
};

describe('Ficha de zona', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  const texto = () => (harness.routeNativeElement!.textContent ?? '').replace(/[  ]/g, ' ');

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(RUTAS_TURISTA, withComponentInputBinding()),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.match(() => true).forEach((p) => p.error(new ProgressEvent('error'))));

  async function abrir(zona: object = ZONA, id = 'z2'): Promise<FichaZona> {
    const componente = await harness.navigateByUrl(`/zonas/${id}`, FichaZona);
    http.expectOne(`/api/catalogo/zonas/${id}`).flush(zona);
    harness.detectChanges();
    return componente;
  }

  it('la ruta /zonas/:id crea la ficha y pide esa zona', async () => {
    expect(await abrir()).toBeInstanceOf(FichaZona);
  });

  it('muestra las buenas prácticas, las condiciones de visita y las marcas', async () => {
    await abrir();

    expect(harness.routeNativeElement!.querySelector('h1')?.textContent).toContain('Parque Tayrona');
    expect(texto()).toContain('Selva y mar.');
    expect(texto()).toContain('No dejes basura');
    expect(texto()).toContain('Lleva tu propia agua');
    expect(texto()).toContain('Entrada con cupo diario.');
    expect(texto()).toContain('Área protegida.');
    expect(texto()).toContain('Zona de alta afluencia.');
  });

  it('pide las alertas de la zona, que son opcionales', async () => {
    await abrir();

    expect(http.match((p) => p.url.includes('/zonas/z2/alertas')).length).toBeGreaterThan(0);
  });

  it('enlaza al catálogo filtrado por la zona', async () => {
    await abrir();

    const enlace = Array.from(harness.routeNativeElement!.querySelectorAll('a')).find((a) =>
      a.textContent?.includes('Ver las ofertas de esta zona'),
    );
    expect(enlace?.getAttribute('href')).toBe('/catalogo?zonaId=z2');
  });

  it('no muestra marcas ni condiciones que la zona no tiene', async () => {
    await abrir({
      ...ZONA,
      areaProtegida: false,
      altaAfluencia: false,
      condicionesVisita: undefined,
      buenasPracticas: [],
    });

    expect(texto()).not.toContain('Área protegida.');
    expect(texto()).not.toContain('Condiciones de visita');
    expect(texto()).toContain('todavía no tiene buenas prácticas');
  });

  it('una zona inactiva se muestra con su aviso', async () => {
    await abrir({ ...ZONA, activa: false });

    expect(texto()).toContain('ya no recibe ofertas nuevas');
  });

  it('una zona que no existe avisa y ofrece volver al catálogo', async () => {
    await harness.navigateByUrl('/zonas/nada', FichaZona);
    http
      .expectOne('/api/catalogo/zonas/nada')
      .flush({ status: 404, codigo: 'ZONA_NO_ENCONTRADA' }, { status: 404, statusText: 'Not Found' });
    harness.detectChanges();

    expect(texto()).toContain('Esta zona no existe');
    expect(harness.routeNativeElement!.querySelector('a[href="/catalogo"]')).not.toBeNull();
  });

  it('cambia a inglés con la ficha abierta', async () => {
    await abrir();

    establecerIdioma('en', { recordar: false });
    harness.detectChanges();

    expect(texto()).toContain('Good practices');
    expect(texto()).toContain('Visiting conditions');
    expect(texto()).toContain('Protected area.');
    expect(texto()).toContain('See the offers in this area');
    expect(texto()).not.toContain('Buenas prácticas');
  });
});
