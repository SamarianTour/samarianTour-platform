import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { DistribucionCategoria } from '../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { DistribucionGestor } from './distribucion-gestor';

const CATEGORIAS_URL = '/api/nucleo/gestor/indicadores/categorias';
const PERIODO = '?desde=2026-10-01&hasta=2026-10-31';

/** El ejemplo del contrato: los guías tienen 6 solicitudes pero 1 o 2 pagadas, y el núcleo no entrega ese conteo. */
const DISTRIBUCION: DistribucionCategoria = {
  desde: '2026-10-01',
  hasta: '2026-10-31',
  total: 42,
  categorias: [
    { categoria: 'GUIA_INDEPENDIENTE', solicitudes: 6, porcentaje: 14.3 },
    { categoria: 'MICRO_PEQUENO', solicitudes: 25, porcentaje: 59.5, pagadas: 10 },
    { categoria: 'MEDIANO', solicitudes: 11, porcentaje: 26.2, pagadas: 6 },
    { categoria: 'GRANDE', solicitudes: 0, porcentaje: 0, pagadas: 0 },
  ],
};

/** Quita los espacios de no separar y los repetidos. */
const normal = (texto: string | null | undefined) => (texto ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ').trim();

describe('DistribucionGestor (P09.1 Distribución por tipo de prestador)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => normal(raiz().textContent);
  const cifras = () =>
    Array.from(raiz().querySelectorAll('.cifra')).map((c) => [
      normal(c.querySelector('.cifra__texto')?.textContent),
      normal(c.querySelector('.cifra__valor')?.textContent),
    ]);
  const graficos = () => Array.from(raiz().querySelectorAll('app-grafico-barras'));
  const tablas = () => Array.from(raiz().querySelectorAll('app-tabla-datos'));
  const barrasDe = (grafico: Element) =>
    Array.from(grafico.querySelectorAll('.barra')).map((b) => [
      normal(b.querySelector('.barra__etiqueta')?.textContent),
      normal(b.querySelector('.barra__valor')?.textContent),
    ]);
  const anchosDe = (grafico: Element) =>
    Array.from(grafico.querySelectorAll<HTMLElement>('.barra__relleno')).map((r) => r.style.width);
  const filasDe = (tabla: Element) =>
    Array.from(tabla.querySelectorAll('tbody tr')).map((f) => Array.from(f.children).map((c) => normal(c.textContent)));

  async function abrir(url = `/gestor/distribucion${PERIODO}`, distribucion = DISTRIBUCION): Promise<void> {
    await harness.navigateByUrl(url, DistribucionGestor);
    harness.detectChanges();
    http.expectOne((p) => p.url === CATEGORIAS_URL).flush(distribucion);
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
        provideRouter([{ path: 'gestor/distribucion', component: DistribucionGestor }]),
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
    it('la distribución del periodo de la dirección', async () => {
      await harness.navigateByUrl(`/gestor/distribucion${PERIODO}`, DistribucionGestor);
      harness.detectChanges();

      const peticion = http.expectOne((p) => p.url === CATEGORIAS_URL);
      expect(peticion.request.params.get('desde')).toBe('2026-10-01');
      expect(peticion.request.params.get('hasta')).toBe('2026-10-31');
      peticion.flush(DISTRIBUCION);
    });

    it('sin periodo en la dirección, los últimos 30 días en hora de Colombia', async () => {
      await harness.navigateByUrl('/gestor/distribucion', DistribucionGestor);
      harness.detectChanges();

      const peticion = http.expectOne((p) => p.url === CATEGORIAS_URL);
      expect(peticion.request.params.get('desde')).toBe('2026-10-12');
      expect(peticion.request.params.get('hasta')).toBe('2026-11-10');
      peticion.flush(DISTRIBUCION);
    });

    it('al cambiar el periodo vuelve a pedir y no deja ver las cifras del anterior', async () => {
      await abrir();
      expect(cifras()[0][1]).toBe('42');

      await TestBed.inject(Router).navigateByUrl('/gestor/distribucion?desde=2026-09-01&hasta=2026-09-30');
      harness.detectChanges();

      expect(raiz().querySelector('.cifras')).toBeNull();
      http.expectOne((p) => p.url === CATEGORIAS_URL).flush({ ...DISTRIBUCION, total: 9 });
      harness.detectChanges();
      expect(cifras()[0][1]).toBe('9');
    });

    it('al volver a la pestaña del navegador pregunta de nuevo', async () => {
      await abrir();

      document.dispatchEvent(new Event('visibilitychange'));
      harness.detectChanges();

      http.expectOne((p) => p.url === CATEGORIAS_URL).flush(DISTRIBUCION);
    });
  });

  describe('las cifras', () => {
    it('muestran el total, el tipo de prestador con más solicitudes y cuántos tipos tuvieron solicitudes', async () => {
      await abrir();

      expect(cifras()).toEqual([
        ['Solicitudes del periodo', '42'],
        ['Tipo de prestador con más solicitudes', 'Micro o pequeña empresa'],
        ['Tipos de prestador con solicitudes', '3'],
      ]);
    });

    it('con un empate nombra a los dos', async () => {
      await abrir(`/gestor/distribucion${PERIODO}`, {
        ...DISTRIBUCION,
        categorias: [
          { categoria: 'GRANDE', solicitudes: 8, porcentaje: 50, pagadas: 4 },
          { categoria: 'MEDIANO', solicitudes: 8, porcentaje: 50, pagadas: 3 },
          { categoria: 'MICRO_PEQUENO', solicitudes: 0, porcentaje: 0, pagadas: 0 },
          { categoria: 'GUIA_INDEPENDIENTE', solicitudes: 0, porcentaje: 0, pagadas: 0 },
        ],
      });

      expect(cifras()[1][1]).toBe('Gran empresa y Mediana empresa');
    });

    it('un total de 1 o 2 solicitudes (que el núcleo no entrega) dice "menos de 3" y no hay mayor', async () => {
      await abrir(`/gestor/distribucion${PERIODO}`, {
        ...DISTRIBUCION,
        total: undefined,
        categorias: [{ categoria: 'GRANDE' }, { categoria: 'MEDIANO' }, { categoria: 'MICRO_PEQUENO', solicitudes: 0, porcentaje: 0, pagadas: 0 }],
      });

      expect(cifras()).toEqual([
        ['Solicitudes del periodo', 'menos de 3'],
        ['Tipo de prestador con más solicitudes', 'sin dato'],
        ['Tipos de prestador con solicitudes', '2'],
      ]);
      // No es lo mismo que cero solicitudes: se dibujan los gráficos.
      expect(graficos()).toHaveLength(2);
    });

    it('sin solicitudes el total es 0, lo dice y no dibuja nada', async () => {
      await abrir(`/gestor/distribucion${PERIODO}`, {
        ...DISTRIBUCION,
        total: 0,
        categorias: DISTRIBUCION.categorias.map((c) => ({ categoria: c.categoria, solicitudes: 0, porcentaje: 0, pagadas: 0 })),
      });

      expect(cifras()[0]).toEqual(['Solicitudes del periodo', '0']);
      expect(cifras()[1][1]).toBe('sin dato');
      expect(texto()).toContain('No hubo solicitudes en este periodo.');
      expect(graficos()).toHaveLength(0);
    });
  });

  describe('las solicitudes por tipo de prestador', () => {
    it('van de los prestadores más grandes a los más pequeños, con su cantidad y su porcentaje del total', async () => {
      await abrir();

      expect(barrasDe(graficos()[0])).toEqual([
        ['Gran empresa', '0 (0,0%)'],
        ['Mediana empresa', '11 (26,2%)'],
        ['Micro o pequeña empresa', '25 (59,5%)'],
        ['Guía independiente', '6 (14,3%)'],
      ]);
    });

    it('el largo de cada barra es su porcentaje del total', async () => {
      await abrir();

      expect(anchosDe(graficos()[0])).toEqual(['0%', '26%', '60%', '14%']);
    });

    it('es una imagen para los lectores de pantalla que lee cada tipo', async () => {
      await abrir();

      const grafico = graficos()[0].querySelector('.barras')!;
      expect(grafico.getAttribute('role')).toBe('img');
      expect(normal(grafico.getAttribute('aria-label'))).toContain(
        'Gran empresa: 0 (0,0%). Mediana empresa: 11 (26,2%). Micro o pequeña empresa: 25 (59,5%). Guía independiente: 6 (14,3%).',
      );
    });

    it('su tabla trae los mismos datos y lleva el periodo en su título', async () => {
      await abrir();

      expect(Array.from(tablas()[0].querySelectorAll('thead th')).map((h) => normal(h.textContent))).toEqual([
        'Tipo de prestador',
        'Solicitudes',
        'Porcentaje del total',
      ]);
      expect(filasDe(tablas()[0])).toEqual([
        ['Gran empresa', '0', '0,0%'],
        ['Mediana empresa', '11', '26,2%'],
        ['Micro o pequeña empresa', '25', '59,5%'],
        ['Guía independiente', '6', '14,3%'],
      ]);
      expect(normal(tablas()[0].querySelector('caption')?.textContent)).toBe(
        'Solicitudes por tipo de prestador, del 1 oct 2026 al 31 oct 2026',
      );
    });

    it('un tipo de conteo omitido va rayado, dice "menos de 3" y su porcentaje es "sin dato"', async () => {
      await abrir(`/gestor/distribucion${PERIODO}`, {
        ...DISTRIBUCION,
        categorias: [{ categoria: 'GRANDE' }, ...DISTRIBUCION.categorias.filter((c) => c.categoria !== 'GRANDE')],
      });

      expect(barrasDe(graficos()[0])[0]).toEqual(['Gran empresa', 'menos de 3']);
      expect(graficos()[0].querySelectorAll('.barra--omitida')).toHaveLength(1);
      expect(filasDe(tablas()[0])[0]).toEqual(['Gran empresa', 'menos de 3', 'sin dato']);
    });

    it('muestra "sin categoría" al final cuando el núcleo la manda', async () => {
      await abrir(`/gestor/distribucion${PERIODO}`, {
        ...DISTRIBUCION,
        categorias: [{ categoria: 'SIN_CATEGORIA', solicitudes: 4, porcentaje: 8, pagadas: 3 }, ...DISTRIBUCION.categorias],
      });

      const nombres = barrasDe(graficos()[0]).map((b) => b[0]);
      expect(nombres[nombres.length - 1]).toBe('Sin categoría');
    });
  });

  describe('las solicitudes pagadas por tipo de prestador', () => {
    it('muestra cuántas de las solicitudes de cada tipo se pagaron, con su porcentaje de pago', async () => {
      await abrir();

      expect(barrasDe(graficos()[1])).toEqual([
        ['Gran empresa', '0 de 0'],
        ['Mediana empresa', '6 de 11 (54,5%)'],
        ['Micro o pequeña empresa', '10 de 25 (40,0%)'],
        ['Guía independiente', 'menos de 3 de 6'],
      ]);
    });

    it('la barra es el porcentaje de pago; si falta una cantidad va rayada y no se inventa', async () => {
      await abrir();

      expect(anchosDe(graficos()[1])).toEqual(['0%', '55%', '40%', '8%']);
      const rayadas = Array.from(graficos()[1].querySelectorAll('.barra')).map((b) => b.classList.contains('barra--omitida'));
      expect(rayadas).toEqual([false, false, false, true]);
    });

    it('su tabla trae las solicitudes, las pagadas y el porcentaje de pago, con "sin dato" donde no se puede calcular', async () => {
      await abrir();

      expect(Array.from(tablas()[1].querySelectorAll('thead th')).map((h) => normal(h.textContent))).toEqual([
        'Tipo de prestador',
        'Solicitudes',
        'Pagadas',
        'Porcentaje de pago',
      ]);
      expect(filasDe(tablas()[1])).toEqual([
        ['Gran empresa', '0', '0', 'sin dato'],
        ['Mediana empresa', '11', '6', '54,5%'],
        ['Micro o pequeña empresa', '25', '10', '40,0%'],
        ['Guía independiente', '6', 'menos de 3', 'sin dato'],
      ]);
      expect(normal(tablas()[1].querySelector('caption')?.textContent)).toBe(
        'Solicitudes pagadas por tipo de prestador, del 1 oct 2026 al 31 oct 2026',
      );
    });

    it('un tipo con las solicitudes omitidas no tiene nada que calcular', async () => {
      await abrir(`/gestor/distribucion${PERIODO}`, {
        ...DISTRIBUCION,
        categorias: [{ categoria: 'GRANDE' }, ...DISTRIBUCION.categorias.filter((c) => c.categoria !== 'GRANDE')],
      });

      expect(barrasDe(graficos()[1])[0]).toEqual(['Gran empresa', 'menos de 3']);
      expect(filasDe(tablas()[1])[0]).toEqual(['Gran empresa', 'menos de 3', 'menos de 3', 'sin dato']);
    });
  });

  it('si el núcleo rechaza el periodo lo avisa y no deja cifras viejas', async () => {
    await harness.navigateByUrl(`/gestor/distribucion${PERIODO}`, DistribucionGestor);
    harness.detectChanges();
    http.expectOne((p) => p.url === CATEGORIAS_URL).flush(
      { status: 400, codigo: 'DATO_INVALIDO', title: 'x', detail: 'hasta: no puede ser anterior a desde' },
      { status: 400, statusText: 'Bad Request', headers: { 'Content-Type': 'application/problem+json' } },
    );
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    expect(raiz().querySelector('.cifras')).toBeNull();
  });

  it('en inglés cambian los tipos, los porcentajes, las tablas y "fewer than 3"', async () => {
    establecerIdioma('en', { recordar: false });
    await abrir();

    expect(cifras().map((c) => c[0])).toEqual([
      'Requests in the period',
      'Provider type with the most requests',
      'Provider types with requests',
    ]);
    expect(cifras()[1][1]).toBe('Micro or small business');
    expect(barrasDe(graficos()[0])[2]).toEqual(['Micro or small business', '25 (59.5%)']);
    expect(barrasDe(graficos()[1]).map((b) => b[1])).toEqual(['0 of 0', '6 of 11 (54.5%)', '10 of 25 (40.0%)', 'fewer than 3 of 6']);
    expect(texto()).toContain('Requests by provider type');
    expect(texto()).toContain('Paid requests by provider type');
    expect(Array.from(tablas()[1].querySelectorAll('thead th')).map((h) => normal(h.textContent))).toEqual([
      'Provider type',
      'Requests',
      'Paid',
      'Payment rate',
    ]);
    expect(filasDe(tablas()[1])[3]).toEqual(['Independent guide', '6', 'fewer than 3', 'no data']);
  });
});
