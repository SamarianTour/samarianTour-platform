import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ResumenAlertas } from '../../../core/api/modelos-recomendacion';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { AlertasDestino } from './alertas-destino';

const URL = '/api/recomendacion/alertas';

const IDEAM = 'IDEAM, alertas hidrológicas vigentes';

const RESUMEN: ResumenAlertas = {
  actualizadoEn: '2026-10-24T23:42:10.512Z',
  zonas: [
    {
      zonaId: 'a1',
      zonaNombre: 'Centro Histórico',
      alertas: [
        { id: 'h1', tipo: 'HIDROLOGICA', nivel: 'AMARILLA', titulo: 'Alerta hidrológica Río Piedras', descripcion: 'Consulta a las autoridades.', fuente: IDEAM },
      ],
    },
    {
      zonaId: 'a5',
      zonaNombre: 'Parque Tayrona',
      alertas: [
        { id: 's1', tipo: 'CIERRE', nivel: 'ROJA', titulo: 'Cierre temporal del parque', fuente: 'Parques Nacionales (simulado)' },
        { id: 's2', tipo: 'OLEAJE', nivel: 'NARANJA', titulo: 'Oleaje fuerte', fuente: 'Parques Nacionales (simulado)' },
      ],
    },
    { zonaId: 'a9', alertas: [{ id: 'h9', tipo: 'LLUVIAS', nivel: 'AMARILLA', titulo: 'Lluvias', fuente: IDEAM }] },
  ],
};

function problema(status: number, codigo: string) {
  return {
    body: { status, codigo, title: codigo, detail: codigo },
    init: { status, statusText: codigo, headers: { 'Content-Type': 'application/problem+json' } },
  };
}

const normal = (texto: string | null | undefined) => (texto ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ').trim();

describe('AlertasDestino (P08.2)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => normal(raiz().textContent);
  const zonas = () => Array.from(raiz().querySelectorAll('.alertas__zona'));
  const tarjetas = () => Array.from(raiz().querySelectorAll('app-tarjeta-alerta'));
  const reintentar = () => Array.from(raiz().querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes('Intentar de nuevo') || b.textContent?.includes('Try again'));

  async function abrir(): Promise<TestRequest> {
    await harness.navigateByUrl('/alertas', AlertasDestino);
    harness.detectChanges();
    return http.expectOne((p) => p.url === URL);
  }

  function responder(peticion: TestRequest, resumen: ResumenAlertas = RESUMEN): void {
    peticion.flush(resumen);
    harness.detectChanges();
  }

  function fallar(peticion: TestRequest, estado: number, codigo: string): void {
    const { body, init } = problema(estado, codigo);
    peticion.flush(body, init);
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'alertas', component: AlertasDestino }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    vi.useRealTimers();
    establecerIdioma('es', { recordar: false });
    http.verify();
  });

  describe('mientras consulta', () => {
    it('pide las alertas con el idioma y mientras tanto lo dice', async () => {
      const peticion = await abrir();

      expect(peticion.request.method).toBe('GET');
      expect(peticion.request.params.get('idioma')).toBe('es');
      expect(raiz().querySelector('h1')?.textContent).toBe('Alertas del destino');
      expect(raiz().querySelector('[role="status"]')?.textContent).toContain('Consultando las alertas…');
      expect(tarjetas()).toHaveLength(0);
      responder(peticion);
    });
  });

  describe('con alertas', () => {
    it('agrupa por zona, con la cantidad total y la hora de actualización', async () => {
      responder(await abrir());

      expect(texto()).toContain('4 alertas activas');
      expect(zonas()).toHaveLength(3);
      expect(normal(raiz().querySelector('.alertas__actualizado')?.textContent)).toMatch(/^Actualizado: 24 oct 2026, \d{1,2}:42 [ap]\. ?m\./);
      expect(raiz().querySelector('.alertas__actualizado time')?.getAttribute('datetime')).toBe('2026-10-24T23:42:10.512Z');
    });

    it('pone arriba la zona con el nivel más grave', async () => {
      responder(await abrir());

      expect(zonas().map((z) => normal(z.querySelector('h2')?.textContent))).toEqual(['Parque Tayrona', 'Centro Histórico', 'Zona sin nombre']);
    });

    it('cada alerta muestra su nivel, su título y su fuente; la simulada se ve como simulada', async () => {
      responder(await abrir());

      const tayrona = zonas()[0];
      expect(tayrona.querySelectorAll('app-tarjeta-alerta')).toHaveLength(2);
      expect(normal(tayrona.querySelector('.tarjeta-alerta__nivel')?.textContent)).toBe('Nivel: Roja');
      expect(tayrona.textContent).toContain('Parques Nacionales (simulado)');
      expect(zonas()[1].textContent).toContain(IDEAM);
    });

    it('cada zona es una región con nombre para los lectores de pantalla', async () => {
      responder(await abrir());

      for (const zona of zonas()) {
        const id = zona.getAttribute('aria-labelledby')!;
        expect(zona.querySelector(`#${id}`)?.tagName).toBe('H2');
      }
    });

    it('una sola alerta dice "1 alerta activa"', async () => {
      responder(await abrir(), { ...RESUMEN, zonas: [RESUMEN.zonas[0]] });

      expect(texto()).toContain('1 alerta activa');
      expect(texto()).not.toContain('1 alertas');
    });

    it('lleva de vuelta a las recomendaciones', async () => {
      responder(await abrir());

      expect(raiz().querySelector('a[href="/recomendaciones"]')?.textContent?.trim()).toBe('Volver a recomendaciones');
    });
  });

  describe('sin alertas', () => {
    it('dice "Sin alertas activas" con la hora de actualización', async () => {
      responder(await abrir(), { actualizadoEn: RESUMEN.actualizadoEn, zonas: [] });

      expect(raiz().querySelector('.vacio h2')?.textContent).toBe('Sin alertas activas');
      expect(raiz().querySelector('.alertas__actualizado')).not.toBeNull();
      expect(tarjetas()).toHaveLength(0);
    });
  });

  describe('si las alertas no están disponibles (degradación)', () => {
    it('un 503 (la fuente aún no ha respondido) avisa y ofrece volver a intentar, sin romper la página', async () => {
      fallar(await abrir(), 503, 'ALERTAS_NO_DISPONIBLES');

      expect(raiz().querySelector('.alerta--aviso')?.textContent).toContain('no están disponibles en este momento');
      expect(raiz().querySelector('h1')?.textContent).toBe('Alertas del destino');
      expect(reintentar()).toBeDefined();
      expect(raiz().querySelector('a[href="/recomendaciones"]')).not.toBeNull();
      expect(tarjetas()).toHaveLength(0);
    });

    it('un 502 también cuenta como no disponible', async () => {
      fallar(await abrir(), 502, 'BAD_GATEWAY');

      expect(raiz().querySelector('.alerta--aviso')?.textContent).toContain('no están disponibles en este momento');
    });

    it('sin conexión avisa que no hay conexión', async () => {
      (await abrir()).error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(texto()).toContain('Parece que no hay conexión a internet.');
    });

    it('a los 8 s sin respuesta deja de esperar y cancela la consulta', async () => {
      vi.useFakeTimers();
      const peticion = await abrir();

      vi.advanceTimersByTime(7_999);
      harness.detectChanges();
      expect(raiz().querySelector('[role="status"]')).not.toBeNull();

      vi.advanceTimersByTime(1);
      harness.detectChanges();

      expect(peticion.cancelled).toBe(true);
      expect(texto()).toContain('tardando más de lo normal');
      expect(raiz().querySelector('[role="status"]')).toBeNull();
    });

    it('"Intentar de nuevo" vuelve a consultar y muestra las alertas', async () => {
      fallar(await abrir(), 503, 'ALERTAS_NO_DISPONIBLES');

      reintentar()!.click();
      harness.detectChanges();
      expect(raiz().querySelector('[role="status"]')).not.toBeNull();
      responder(http.expectOne((p) => p.url === URL));

      expect(zonas()).toHaveLength(3);
      expect(raiz().querySelector('.alerta--aviso')).toBeNull();
    });

    it('un error propio de la consulta se explica con el mensaje del servicio', async () => {
      fallar(await abrir(), 400, 'DATO_INVALIDO');

      expect(raiz().querySelector('.alerta--error')).not.toBeNull();
      expect(raiz().querySelector('.alerta--aviso')).toBeNull();
      expect(reintentar()).toBeDefined();
    });
  });

  it('en inglés pide los textos en inglés y traduce el resto de la página', async () => {
    establecerIdioma('en', { recordar: false });
    const peticion = await abrir();
    expect(peticion.request.params.get('idioma')).toBe('en');

    responder(peticion);

    expect(raiz().querySelector('h1')?.textContent).toBe('Destination alerts');
    expect(texto()).toContain('4 active alerts');
    expect(texto()).toContain('Last updated: Oct 24, 2026');
    expect(zonas().map((z) => normal(z.querySelector('h2')?.textContent))[2]).toBe('Unnamed area');
    expect(raiz().querySelector('a[href="/recomendaciones"]')?.textContent?.trim()).toBe('Back to recommendations');
  });
});
