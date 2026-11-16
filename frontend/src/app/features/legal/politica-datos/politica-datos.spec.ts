import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { RUTAS_LEGAL } from '../legal.routes';
import { PoliticaDatosPagina } from './politica-datos';

const POLITICA = '/api/nucleo/politica-datos/vigente';
const TEXTO = '/api/nucleo/politica-datos/v2';

describe('PoliticaDatosPagina (/politica-datos)', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/\s+/g, ' ');

  beforeEach(async () => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [provideRouter(RUTAS_LEGAL), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('/politica-datos crea su pantalla y pide la política vigente', async () => {
    const pantalla = await harness.navigateByUrl('/politica-datos', PoliticaDatosPagina);

    expect(pantalla).toBeInstanceOf(PoliticaDatosPagina);
    http.expectOne(POLITICA);
    expect(texto()).toContain('Cargando el texto');
  });

  it('muestra el texto vigente con formato: sin símbolos de Markdown, con las frases unidas y sin repetir el título', async () => {
    await harness.navigateByUrl('/politica-datos', PoliticaDatosPagina);
    http.expectOne(POLITICA).flush({ version: 'v2', fechaPublicacion: '2026-01-01', urlTexto: TEXTO });
    http
      .expectOne(TEXTO)
      .flush(
        '# Política de tratamiento de datos personales de Samarian Tour\n\n> Texto de demostración.\n\n## 1. Responsable\n\nTratamos tus datos\ncon **cuidado**.\n\n- Uno\n- Dos',
      );
    harness.detectChanges();

    // Un solo h1: el de la página. El título del texto se omite y los demás bajan de nivel.
    expect(Array.from(raiz().querySelectorAll('h1')).map((h) => h.textContent)).toEqual(['Política de tratamiento de datos']);
    expect(texto()).toContain('Ley 1581 de 2012');
    expect(texto()).toContain('Versión v2');
    const cuerpo = raiz().querySelector('.legal__politica')!;
    expect(cuerpo.querySelector('h2')).toBeNull();
    expect(cuerpo.querySelector('h3')?.textContent).toBe('1. Responsable');
    expect(cuerpo.querySelector('blockquote')?.textContent).toBe('Texto de demostración.');
    expect(cuerpo.querySelector('p')?.textContent).toBe('Tratamos tus datos con cuidado.');
    expect(cuerpo.querySelector('p strong')?.textContent).toBe('cuidado');
    expect(cuerpo.querySelectorAll('li')).toHaveLength(2);
    expect(cuerpo.textContent).not.toMatch(/[#>]|\*\*/);
  });

  it('un <script> del texto se ve como texto: no se crea ningún elemento', async () => {
    await harness.navigateByUrl('/politica-datos', PoliticaDatosPagina);
    http.expectOne(POLITICA).flush({ version: 'v2', fechaPublicacion: '2026-01-01', urlTexto: TEXTO });
    http.expectOne(TEXTO).flush('Hola <script>alert(1)</script> y <b>negrita falsa</b>.');
    harness.detectChanges();

    const cuerpo = raiz().querySelector('.legal__politica')!;
    expect(cuerpo.querySelector('script')).toBeNull();
    expect(cuerpo.querySelector('b')).toBeNull();
    expect(cuerpo.textContent).toContain('<script>alert(1)</script>');
  });

  it('si el backend falla, muestra el error amable y deja reintentar', async () => {
    await harness.navigateByUrl('/politica-datos', PoliticaDatosPagina);
    http.expectOne(POLITICA).flush(null, { status: 500, statusText: 'Server Error' });
    harness.detectChanges();

    const alerta = raiz().querySelector('.alerta--error')!;
    expect(alerta.getAttribute('role')).toBe('alert');
    expect(alerta.textContent?.trim()).not.toBe('');
    expect(raiz().querySelector('.legal__politica')).toBeNull();

    const reintentar = raiz().querySelector<HTMLButtonElement>('button')!;
    expect(reintentar.textContent).toContain('Intentar de nuevo');
    reintentar.click();
    http.expectOne(POLITICA).flush({ version: 'v2', fechaPublicacion: '2026-01-01', urlTexto: TEXTO });
    http.expectOne(TEXTO).flush('Texto');
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).toBeNull();
    expect(raiz().querySelector('.legal__politica')?.textContent?.trim()).toBe('Texto');
  });

  it('cambia a inglés con la pantalla abierta', async () => {
    await harness.navigateByUrl('/politica-datos', PoliticaDatosPagina);
    http.expectOne(POLITICA).flush({ version: 'v2', fechaPublicacion: '2026-01-01', urlTexto: TEXTO });
    http.expectOne(TEXTO).flush('Texto');

    establecerIdioma('en', { recordar: false });
    harness.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toBe('Data processing policy');
    expect(texto()).toContain('Law 1581 of 2012');
    expect(texto()).toContain('Version v2');
  });
});
