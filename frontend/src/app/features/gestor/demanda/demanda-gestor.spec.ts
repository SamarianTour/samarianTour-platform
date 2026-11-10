import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Zona } from '../../../core/api/modelos';
import { Tablero } from '../../../core/api/modelos-nucleo';
import { erroresInterceptor } from '../../../core/errores/errores.interceptor';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { DemandaGestor } from './demanda-gestor';

const TABLERO_URL = '/api/nucleo/gestor/indicadores/tablero';
const ZONAS_URL = '/api/catalogo/zonas';
const PERIODO = '?desde=2026-10-01&hasta=2026-10-31';

function zona(id: string, nombre: string, extra: Partial<Zona> = {}): Zona {
  return { id, nombre, areaProtegida: false, altaAfluencia: false, activa: true, buenasPracticas: [], ...extra };
}

const ZONAS = [
  zona('tayrona', 'Parque Tayrona', { areaProtegida: true, altaAfluencia: true }),
  zona('minca', 'Minca'),
];

/** El total es 42 aunque las filas conocidas sumen 34: dos filas tienen 1 o 2 solicitudes y no traen su conteo. */
const TABLERO: Tablero = {
  desde: '2026-10-01',
  hasta: '2026-10-31',
  total: 42,
  filas: [
    { zonaId: 'tayrona', tipoOferta: 'ACTIVIDAD', solicitudes: 18 },
    { zonaId: 'tayrona', tipoOferta: 'ALOJAMIENTO', solicitudes: 9 },
    { zonaId: 'minca', tipoOferta: 'ACTIVIDAD', solicitudes: 7 },
    { zonaId: 'minca', tipoOferta: 'ALOJAMIENTO' },
    { zonaId: 'vieja', tipoOferta: 'RESTAURANTE' },
  ],
};

describe('DemandaGestor (P09.1 Demanda)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ');
  const cifras = () =>
    Array.from(raiz().querySelectorAll('.cifra')).map((c) => [
      c.querySelector('.cifra__texto')?.textContent,
      c.querySelector('.cifra__valor')?.textContent,
    ]);
  const barras = () => Array.from(raiz().querySelectorAll<HTMLElement>('.barra'));
  const filasDeTabla = () =>
    Array.from(raiz().querySelectorAll('tbody tr')).map((f) =>
      Array.from(f.children).map((c) => c.textContent?.trim()),
    );

  const pedidoTablero = () => http.expectOne((p) => p.url === TABLERO_URL);

  /** Abre la pestaña y responde al tablero y a la lista de zonas. */
  async function abrir(url = `/gestor${PERIODO}`, tablero: Tablero = TABLERO, zonas: Zona[] = ZONAS): Promise<void> {
    await harness.navigateByUrl(url, DemandaGestor);
    harness.detectChanges();
    pedidoTablero().flush(tablero);
    http.expectOne(ZONAS_URL).flush(zonas);
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
        provideRouter([{ path: 'gestor', component: DemandaGestor }]),
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
    it('el tablero del periodo de la dirección y la lista de zonas', async () => {
      await harness.navigateByUrl(`/gestor${PERIODO}`, DemandaGestor);
      harness.detectChanges();

      const peticion = pedidoTablero();
      expect(peticion.request.params.get('desde')).toBe('2026-10-01');
      expect(peticion.request.params.get('hasta')).toBe('2026-10-31');
      http.expectOne(ZONAS_URL);
      peticion.flush(TABLERO);
      http.match(ZONAS_URL).forEach((p) => p.flush(ZONAS));
    });

    it('sin periodo en la dirección, los últimos 30 días en hora de Colombia', async () => {
      await harness.navigateByUrl('/gestor', DemandaGestor);
      harness.detectChanges();

      const peticion = pedidoTablero();
      expect(peticion.request.params.get('desde')).toBe('2026-10-12');
      expect(peticion.request.params.get('hasta')).toBe('2026-11-10');
      peticion.flush(TABLERO);
      http.expectOne(ZONAS_URL).flush(ZONAS);
    });

    it('al cambiar el periodo de la dirección vuelve a pedir y no deja ver las cifras del periodo anterior', async () => {
      await abrir();
      expect(cifras()[0][1]).toBe('42');

      await TestBed.inject(Router).navigateByUrl('/gestor?desde=2026-09-01&hasta=2026-09-30');
      harness.detectChanges();

      expect(raiz().querySelector('.cifras')).toBeNull();
      expect(texto()).toContain('Cargando los indicadores…');
      const peticion = pedidoTablero();
      expect(peticion.request.params.get('desde')).toBe('2026-09-01');
      peticion.flush({ desde: '2026-09-01', hasta: '2026-09-30', total: 5, filas: [] });
      http.expectOne(ZONAS_URL).flush(ZONAS);
      harness.detectChanges();
      expect(cifras()[0][1]).toBe('5');
    });

    it('al volver a la pestaña del navegador pregunta de nuevo', async () => {
      await abrir();

      document.dispatchEvent(new Event('visibilitychange'));
      harness.detectChanges();

      pedidoTablero().flush(TABLERO);
      http.expectOne(ZONAS_URL).flush(ZONAS);
    });
  });

  describe('las cifras', () => {
    it('el total es el del núcleo, no la suma de las filas con conteo', async () => {
      await abrir();

      expect(cifras()).toEqual([
        ['Solicitudes del periodo', '42'],
        ['Zonas con solicitudes', '3'],
        ['Tipos de oferta con solicitudes', '3'],
      ]);
    });

    it('un total de 1 o 2 solicitudes (que el núcleo no entrega) dice "menos de 3"', async () => {
      await abrir(`/gestor${PERIODO}`, {
        desde: '2026-10-01',
        hasta: '2026-10-31',
        filas: [{ zonaId: 'minca', tipoOferta: 'ACTIVIDAD' }],
      });

      expect(cifras()[0]).toEqual(['Solicitudes del periodo', 'menos de 3']);
      expect(cifras()[1][1]).toBe('1');
    });

    it('sin solicitudes el total es 0 y no hay gráfico', async () => {
      await abrir(`/gestor${PERIODO}`, { desde: '2026-10-01', hasta: '2026-10-31', total: 0, filas: [] });

      expect(cifras()[0][1]).toBe('0');
      expect(texto()).toContain('No hubo solicitudes en este periodo.');
      expect(raiz().querySelector('app-grafico-barras')).toBeNull();
      expect(raiz().querySelector('app-tabla-datos')).toBeNull();
    });
  });

  describe('el gráfico', () => {
    it('tiene una barra por zona y tipo de oferta, de mayor a menor, con el nombre de la zona', async () => {
      await abrir();

      expect(barras().map((b) => [b.querySelector('.barra__etiqueta')?.textContent, b.querySelector('.barra__detalle')?.textContent, b.querySelector('.barra__valor')?.textContent])).toEqual([
        ['Parque Tayrona', 'Actividad', '18'],
        ['Parque Tayrona', 'Alojamiento', '9'],
        ['Minca', 'Actividad', '7'],
        ['Minca', 'Alojamiento', 'menos de 3'],
        ['Zona sin nombre (vieja)', 'Restaurante', 'menos de 3'],
      ]);
    });

    it('marca las zonas sensibles', async () => {
      await abrir();

      const marcas = Array.from(barras()[0].querySelectorAll('.etiqueta')).map((m) => m.textContent);
      expect(marcas).toEqual(['Área protegida', 'Alta afluencia']);
      expect(barras()[2].querySelector('.etiqueta')).toBeNull();
    });

    it('el largo es proporcional a la mayor cantidad conocida', async () => {
      await abrir();

      const ancho = (i: number) => barras()[i].querySelector<HTMLElement>('.barra__relleno')!.style.width;
      expect(ancho(0)).toBe('100%');
      expect(ancho(1)).toBe('50%');
      expect(ancho(2)).toBe('39%');
    });

    it('un conteo omitido va rayado, no como una cantidad', async () => {
      await abrir();

      expect(barras()[3].classList).toContain('barra--omitida');
      expect(barras()[4].classList).toContain('barra--omitida');
      expect(barras()[0].classList).not.toContain('barra--omitida');
    });

    it('es una imagen para los lectores de pantalla, resumida por la mayor barra', async () => {
      await abrir();

      const grafico = raiz().querySelector('.barras')!;
      expect(grafico.getAttribute('role')).toBe('img');
      expect(grafico.getAttribute('aria-label')).toContain('La mayor es Parque Tayrona, Actividad: 18 solicitudes.');
      expect(grafico.getAttribute('aria-label')).toContain('Los datos completos están en la tabla que sigue.');
    });

    it('si falla la lista de zonas, igual se ve el tablero con el id de cada zona', async () => {
      await harness.navigateByUrl(`/gestor${PERIODO}`, DemandaGestor);
      harness.detectChanges();
      pedidoTablero().flush(TABLERO);
      http.expectOne(ZONAS_URL).error(new ProgressEvent('error'));
      harness.detectChanges();

      expect(raiz().querySelector('.alerta--error')).toBeNull();
      expect(barras()[0].querySelector('.barra__etiqueta')?.textContent).toBe('Zona sin nombre (tayrona)');
      expect(barras()[0].querySelector('.etiqueta')).toBeNull();
    });
  });

  describe('la tabla de datos', () => {
    it('trae lo mismo que el gráfico, con "menos de 3" donde el núcleo omitió el conteo', async () => {
      await abrir();

      expect(Array.from(raiz().querySelectorAll('thead th')).map((h) => h.textContent?.trim())).toEqual([
        'Zona',
        'Tipo de oferta',
        'Solicitudes',
      ]);
      expect(filasDeTabla()).toEqual([
        ['Parque Tayrona (área protegida, alta afluencia)', 'Actividad', '18'],
        ['Parque Tayrona (área protegida, alta afluencia)', 'Alojamiento', '9'],
        ['Minca', 'Actividad', '7'],
        ['Minca', 'Alojamiento', 'menos de 3'],
        ['Zona sin nombre (vieja)', 'Restaurante', 'menos de 3'],
      ]);
    });

    it('lleva el periodo en su título y se abre con un desplegable', async () => {
      await abrir();

      expect(raiz().querySelector('caption')?.textContent).toBe(
        'Solicitudes por zona y tipo de oferta, del 1 oct 2026 al 31 oct 2026',
      );
      expect(raiz().querySelector('summary')?.textContent?.trim()).toBe('Ver los datos en una tabla');
    });
  });

  it('si el núcleo rechaza el periodo lo avisa y no deja cifras viejas', async () => {
    await harness.navigateByUrl(`/gestor${PERIODO}`, DemandaGestor);
    harness.detectChanges();
    pedidoTablero().flush(
      { status: 400, codigo: 'DATO_INVALIDO', title: 'x', detail: 'hasta: no puede ser anterior a desde' },
      { status: 400, statusText: 'Bad Request', headers: { 'Content-Type': 'application/problem+json' } },
    );
    // La lista de zonas se cancela sola: sin tablero no hace falta. Se descarta para que no cuente como petición abierta.
    expect(http.match(ZONAS_URL)).toHaveLength(1);
    harness.detectChanges();

    expect(raiz().querySelector('.alerta--error')).not.toBeNull();
    expect(raiz().querySelector('.cifras')).toBeNull();
  });

  it('en inglés cambian las cifras, el gráfico, la tabla y "fewer than 3"', async () => {
    establecerIdioma('en', { recordar: false });
    await abrir();

    expect(cifras().map((c) => c[0])).toEqual(['Requests in the period', 'Areas with requests', 'Offer types with requests']);
    expect(texto()).toContain('Requests by area and offer type');
    expect(barras()[0].querySelector('.barra__detalle')?.textContent).toBe('Activity');
    expect(Array.from(barras()[0].querySelectorAll('.etiqueta')).map((m) => m.textContent)).toEqual([
      'Protected area',
      'High visitor traffic',
    ]);
    expect(barras()[3].querySelector('.barra__valor')?.textContent).toBe('fewer than 3');
    expect(raiz().querySelector('.barras')?.getAttribute('aria-label')).toContain('The highest is Parque Tayrona, Activity: 18 requests.');
    expect(raiz().querySelector('caption')?.textContent).toBe('Requests by area and offer type, from Oct 1, 2026 to Oct 31, 2026');
    expect(filasDeTabla()[0][0]).toBe('Parque Tayrona (protected area, high visitor traffic)');
  });
});
