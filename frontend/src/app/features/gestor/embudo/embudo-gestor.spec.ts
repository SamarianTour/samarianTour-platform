import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Embudo, EtapaEmbudo } from '../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { EmbudoGestor } from './embudo-gestor';

const EMBUDO_URL = '/api/nucleo/gestor/indicadores/embudo';
const PERIODO = '?desde=2026-10-01&hasta=2026-10-31';

const TOTAL: EtapaEmbudo[] = [
  { etapa: 'ENVIADAS', solicitudes: 42, porcentaje: 100 },
  { etapa: 'ATENDIDAS', solicitudes: 30, porcentaje: 71.4 },
  { etapa: 'CONFIRMADAS', solicitudes: 21, porcentaje: 50 },
  { etapa: 'PAGADAS', solicitudes: 17, porcentaje: 40.5 },
];

const EMBUDO: Embudo = {
  desde: '2026-10-01',
  hasta: '2026-10-31',
  etapas: TOTAL,
  porTipo: [
    { tipoOferta: 'ACTIVIDAD', etapas: TOTAL },
    {
      tipoOferta: 'RESTAURANTE',
      etapas: [
        { etapa: 'ENVIADAS', solicitudes: 4, porcentaje: 100 },
        { etapa: 'ATENDIDAS', solicitudes: 3, porcentaje: 75 },
        { etapa: 'CONFIRMADAS' },
        { etapa: 'PAGADAS', solicitudes: 0, porcentaje: 0 },
      ],
    },
    {
      tipoOferta: 'ALOJAMIENTO',
      etapas: ['ENVIADAS', 'ATENDIDAS', 'CONFIRMADAS', 'PAGADAS'].map((etapa) => ({
        etapa: etapa as EtapaEmbudo['etapa'],
        solicitudes: 0,
      })),
    },
  ],
};

/** Quita los espacios de no separar con que Intl escribe el signo de porcentaje en español. */
const normal = (texto: string | null | undefined) => (texto ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ').trim();

describe('EmbudoGestor (P09.1 Embudo de conversión)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => normal(raiz().textContent);
  const cifras = () =>
    Array.from(raiz().querySelectorAll('.cifra')).map((c) => [
      normal(c.querySelector('.cifra__texto')?.textContent),
      normal(c.querySelector('.cifra__valor')?.textContent),
    ]);
  /** Las barras de un gráfico: [nombre, detalle, valor]. */
  const barrasDe = (grafico: Element) =>
    Array.from(grafico.querySelectorAll('.barra')).map((b) => [
      normal(b.querySelector('.barra__etiqueta')?.textContent),
      normal(b.querySelector('.barra__detalle')?.textContent) || null,
      normal(b.querySelector('.barra__valor')?.textContent),
    ]);
  const graficos = () => Array.from(raiz().querySelectorAll('app-grafico-barras'));
  const tablas = () => Array.from(raiz().querySelectorAll('app-tabla-datos'));
  const filasDe = (tabla: Element) =>
    Array.from(tabla.querySelectorAll('tbody tr')).map((f) => Array.from(f.children).map((c) => normal(c.textContent)));

  async function abrir(url = `/gestor/embudo${PERIODO}`, embudo: Embudo = EMBUDO): Promise<void> {
    await harness.navigateByUrl(url, EmbudoGestor);
    harness.detectChanges();
    http.expectOne((p) => p.url === EMBUDO_URL).flush(embudo);
    harness.detectChanges();
  }

  beforeEach(async () => {
    // 10 de noviembre de 2026, 10:00 en Colombia. Solo se simula la fecha.
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-10T15:00:00Z'));
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'GESTOR_DESTINO' }))));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'gestor/embudo', component: EmbudoGestor }]),
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

  describe('lo que pide', () => {
    it('el embudo del periodo de la dirección, y nada más', async () => {
      await harness.navigateByUrl(`/gestor/embudo${PERIODO}`, EmbudoGestor);
      harness.detectChanges();

      const peticion = http.expectOne((p) => p.url === EMBUDO_URL);
      expect(peticion.request.params.get('desde')).toBe('2026-10-01');
      expect(peticion.request.params.get('hasta')).toBe('2026-10-31');
      peticion.flush(EMBUDO);
    });

    it('sin periodo en la dirección, los últimos 30 días en hora de Colombia', async () => {
      await harness.navigateByUrl('/gestor/embudo', EmbudoGestor);
      harness.detectChanges();

      const peticion = http.expectOne((p) => p.url === EMBUDO_URL);
      expect(peticion.request.params.get('desde')).toBe('2026-10-12');
      expect(peticion.request.params.get('hasta')).toBe('2026-11-10');
      peticion.flush(EMBUDO);
    });

    it('al cambiar el periodo vuelve a pedir y no deja ver las cifras del anterior', async () => {
      await abrir();
      expect(cifras()[0][1]).toBe('42');

      await TestBed.inject(Router).navigateByUrl('/gestor/embudo?desde=2026-09-01&hasta=2026-09-30');
      harness.detectChanges();

      expect(raiz().querySelector('.cifras')).toBeNull();
      http
        .expectOne((p) => p.url === EMBUDO_URL)
        .flush({ ...EMBUDO, desde: '2026-09-01', hasta: '2026-09-30', etapas: [{ etapa: 'ENVIADAS', solicitudes: 7, porcentaje: 100 }] });
      harness.detectChanges();
      expect(cifras()[0][1]).toBe('7');
    });

    it('al volver a la pestaña del navegador pregunta de nuevo', async () => {
      await abrir();

      document.dispatchEvent(new Event('visibilitychange'));
      harness.detectChanges();

      http.expectOne((p) => p.url === EMBUDO_URL).flush(EMBUDO);
    });
  });

  describe('las cifras', () => {
    it('muestran las enviadas, las pagadas y qué parte de las enviadas se pagó', async () => {
      await abrir();

      expect(cifras()).toEqual([
        ['Solicitudes enviadas', '42'],
        ['Solicitudes pagadas', '17'],
        ['De las enviadas, se pagaron', '40,5%'],
      ]);
    });

    it('con las enviadas omitidas (1 o 2) dicen "menos de 3" y "sin dato", sin inventar nada', async () => {
      await abrir(`/gestor/embudo${PERIODO}`, {
        ...EMBUDO,
        etapas: ['ENVIADAS', 'ATENDIDAS', 'CONFIRMADAS', 'PAGADAS'].map((etapa) => ({ etapa: etapa as EtapaEmbudo['etapa'] })),
        porTipo: [],
      });

      expect(cifras()).toEqual([
        ['Solicitudes enviadas', 'menos de 3'],
        ['Solicitudes pagadas', 'menos de 3'],
        ['De las enviadas, se pagaron', 'sin dato'],
      ]);
      // No es lo mismo que cero solicitudes: se dibuja el embudo, con todas las barras rayadas.
      expect(texto()).not.toContain('No hubo solicitudes');
      expect(raiz().querySelectorAll('.barra--omitida')).toHaveLength(4);
    });
  });

  describe('el embudo del periodo', () => {
    it('tiene las cuatro etapas, con su cantidad, su porcentaje sobre las enviadas y de cuántas de la anterior pasaron', async () => {
      await abrir();

      expect(barrasDe(graficos()[0])).toEqual([
        ['Enviadas', null, '42 (100,0%)'],
        ['Atendidas', '71,4% de las enviadas', '30 (71,4%)'],
        ['Confirmadas', '70,0% de las atendidas', '21 (50,0%)'],
        ['Pagadas', '81,0% de las confirmadas', '17 (40,5%)'],
      ]);
    });

    it('el largo de cada barra es su porcentaje sobre las enviadas', async () => {
      await abrir();

      const anchos = Array.from(graficos()[0].querySelectorAll<HTMLElement>('.barra__relleno')).map((r) => r.style.width);
      expect(anchos).toEqual(['100%', '71%', '50%', '41%']);
    });

    it('es una imagen para los lectores de pantalla que lee cada etapa', async () => {
      await abrir();

      const grafico = graficos()[0].querySelector('.barras')!;
      expect(grafico.getAttribute('role')).toBe('img');
      expect(normal(grafico.getAttribute('aria-label'))).toContain(
        'Enviadas: 42 (100,0%). Atendidas: 30 (71,4%). Confirmadas: 21 (50,0%). Pagadas: 17 (40,5%).',
      );
      expect(grafico.getAttribute('aria-label')).toContain('Los datos completos están en la tabla que sigue.');
    });

    it('su tabla trae los mismos datos, con "no aplica" donde la enviada no tiene etapa anterior', async () => {
      await abrir();

      expect(Array.from(tablas()[0].querySelectorAll('thead th')).map((h) => normal(h.textContent))).toEqual([
        'Etapa',
        'Solicitudes',
        'Porcentaje de las enviadas',
        'Porcentaje de la etapa anterior',
      ]);
      expect(filasDe(tablas()[0])).toEqual([
        ['Enviadas', '42', '100,0%', 'no aplica'],
        ['Atendidas', '30', '71,4%', '71,4%'],
        ['Confirmadas', '21', '50,0%', '70,0%'],
        ['Pagadas', '17', '40,5%', '81,0%'],
      ]);
      expect(normal(tablas()[0].querySelector('caption')?.textContent)).toBe(
        'Embudo de conversión, del 1 oct 2026 al 31 oct 2026',
      );
    });

    it('sin solicitudes en el periodo lo dice y no dibuja nada', async () => {
      await abrir(`/gestor/embudo${PERIODO}`, {
        ...EMBUDO,
        etapas: ['ENVIADAS', 'ATENDIDAS', 'CONFIRMADAS', 'PAGADAS'].map((etapa) => ({
          etapa: etapa as EtapaEmbudo['etapa'],
          solicitudes: 0,
        })),
        porTipo: [],
      });

      expect(texto()).toContain('No hubo solicitudes en este periodo.');
      expect(graficos()).toHaveLength(0);
      expect(cifras()[0]).toEqual(['Solicitudes enviadas', '0']);
      expect(cifras()[2]).toEqual(['De las enviadas, se pagaron', 'sin dato']);
    });
  });

  describe('por tipo de oferta', () => {
    it('cada tipo tiene su propio gráfico y su propia tabla', async () => {
      await abrir();

      // El total y los dos tipos con solicitudes; el tercero no tuvo ninguna.
      expect(graficos()).toHaveLength(3);
      expect(tablas()).toHaveLength(3);
      expect(Array.from(raiz().querySelectorAll('.tipo h3')).map((h) => h.textContent)).toEqual([
        'Actividad',
        'Restaurante',
        'Alojamiento',
      ]);
    });

    it('un conteo omitido en un tipo se muestra "menos de 3", rayado, y no se calcula a su alrededor', async () => {
      await abrir();

      expect(barrasDe(graficos()[2])).toEqual([
        ['Enviadas', null, '4 (100,0%)'],
        ['Atendidas', '75,0% de las enviadas', '3 (75,0%)'],
        ['Confirmadas', null, 'menos de 3'],
        ['Pagadas', null, '0 (0,0%)'],
      ]);
      expect(graficos()[2].querySelectorAll('.barra--omitida')).toHaveLength(1);
      expect(filasDe(tablas()[2])[2]).toEqual(['Confirmadas', 'menos de 3', 'sin dato', 'sin dato']);
    });

    it('el resumen de cada tipo nombra el tipo', async () => {
      await abrir();

      expect(graficos()[2].querySelector('.barras')?.getAttribute('aria-label')).toContain('ofertas de tipo Restaurante');
      expect(normal(tablas()[2].querySelector('caption')?.textContent)).toBe(
        'Embudo de las ofertas de tipo Restaurante, del 1 oct 2026 al 31 oct 2026',
      );
    });

    it('un tipo sin solicitudes lo dice en vez de dibujar un embudo vacío', async () => {
      await abrir();

      const bloque = raiz().querySelectorAll('.tipo')[2];
      expect(bloque.textContent).toContain('No hubo solicitudes de este tipo en el periodo.');
      expect(bloque.querySelector('app-grafico-barras')).toBeNull();
    });
  });

  it('si el núcleo rechaza el periodo lo avisa y no deja cifras viejas', async () => {
    await harness.navigateByUrl(`/gestor/embudo${PERIODO}`, EmbudoGestor);
    harness.detectChanges();
    http.expectOne((p) => p.url === EMBUDO_URL).flush(
      { status: 400, codigo: 'DATO_INVALIDO', title: 'x', detail: 'hasta: no puede ser anterior a desde' },
      { status: 400, statusText: 'Bad Request', headers: { 'Content-Type': 'application/problem+json' } },
    );
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    expect(raiz().querySelector('.cifras')).toBeNull();
  });

  it('en inglés cambian las etapas, los porcentajes, la tabla y "fewer than 3"', async () => {
    establecerIdioma('en', { recordar: false });
    await abrir();

    expect(cifras().map((c) => c[0])).toEqual(['Requests sent', 'Requests paid', 'Of those sent, were paid']);
    expect(cifras()[2][1]).toBe('40.5%');
    expect(barrasDe(graficos()[0])).toEqual([
      ['Sent', null, '42 (100.0%)'],
      ['Handled', '71.4% of those sent', '30 (71.4%)'],
      ['Confirmed', '70.0% of those handled', '21 (50.0%)'],
      ['Paid', '81.0% of those confirmed', '17 (40.5%)'],
    ]);
    expect(texto()).toContain('Conversion funnel for the period');
    expect(texto()).toContain('By offer type');
    expect(barrasDe(graficos()[2])[2][2]).toBe('fewer than 3');
    expect(filasDe(tablas()[2])[2]).toEqual(['Confirmed', 'fewer than 3', 'no data', 'no data']);
    expect(Array.from(tablas()[0].querySelectorAll('thead th')).map((h) => normal(h.textContent))[3]).toBe(
      'Percentage of the previous stage',
    );
  });
});
