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

  it('muestra el texto vigente de la versión que entrega el backend, tal cual y con sus saltos de línea', async () => {
    await harness.navigateByUrl('/politica-datos', PoliticaDatosPagina);
    http.expectOne(POLITICA).flush({ version: 'v2', fechaPublicacion: '2026-01-01', urlTexto: TEXTO });
    http.expectOne(TEXTO).flush('# Política\n\nTratamos tus datos con <b>cuidado</b>.');
    harness.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toBe('Política de tratamiento de datos');
    expect(texto()).toContain('Ley 1581 de 2012');
    expect(texto()).toContain('Versión v2');
    const cuerpo = raiz().querySelector('.legal__politica')!;
    // El Markdown no se interpreta y el HTML no se inyecta: se ve como texto.
    expect(cuerpo.textContent).toBe('# Política\n\nTratamos tus datos con <b>cuidado</b>.');
    expect(cuerpo.querySelector('b')).toBeNull();
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
    expect(raiz().querySelector('.legal__politica')?.textContent).toBe('Texto');
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
