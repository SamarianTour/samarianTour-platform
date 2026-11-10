import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { CLAVE_SESION, aSesionGuardada } from '../../../core/sesion/almacen-sesion';
import { respuestaDeSesion } from '../../../core/sesion/testing';
import { RUTAS_GESTOR } from '../gestor.routes';
import { PanelGestor } from '../panel/panel-gestor';

/** 10 de noviembre de 2026, 10:00 en Colombia. */
const AHORA = new Date('2026-11-10T15:00:00Z');

describe('PanelIndicadores (P09.1)', () => {
  let harness: RouterTestingHarness;
  let router: Router;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ');
  const desde = () => raiz().querySelector<HTMLInputElement>('#periodo-desde')!;
  const hasta = () => raiz().querySelector<HTMLInputElement>('#periodo-hasta')!;
  const atajos = () => Array.from(raiz().querySelectorAll<HTMLButtonElement>('.atajo'));
  const atajoMarcado = () => atajos().filter((a) => a.getAttribute('aria-pressed') === 'true').map((a) => a.textContent?.trim());
  const pestanas = () => Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.pestanas a'));

  function escribir(campo: HTMLInputElement, valor: string): void {
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    harness.detectChanges();
  }

  async function ir(url: string): Promise<void> {
    await harness.navigateByUrl(url, PanelGestor);
    harness.detectChanges();
  }

  beforeEach(async () => {
    // Solo se simula la fecha: los temporizadores reales siguen funcionando.
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(AHORA);
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    localStorage.setItem(CLAVE_SESION, JSON.stringify(aSesionGuardada(respuestaDeSesion({ rol: 'GESTOR_DESTINO' }))));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'gestor', children: RUTAS_GESTOR }], withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    router = TestBed.inject(Router);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    vi.useRealTimers();
    establecerIdioma('es', { recordar: false });
  });

  describe('el periodo', () => {
    it('sin periodo en la dirección muestra los últimos 30 días, en hora de Colombia', async () => {
      await ir('/gestor');

      expect(desde().value).toBe('2026-10-12');
      expect(hasta().value).toBe('2026-11-10');
      expect(texto()).toContain('Periodo consultado: del 12 oct 2026 al 10 nov 2026 (30 días).');
      expect(atajoMarcado()).toEqual(['Últimos 30 días']);
    });

    it('usa el periodo de la dirección', async () => {
      await ir('/gestor?desde=2026-09-01&hasta=2026-10-01');

      expect(desde().value).toBe('2026-09-01');
      expect(hasta().value).toBe('2026-10-01');
      expect(texto()).toContain('(31 días)');
      expect(atajoMarcado()).toEqual([]);
    });

    it('una dirección con un periodo inválido no rompe la pantalla: usa el de por defecto', async () => {
      await ir('/gestor?desde=2026-10-31&hasta=2026-10-01');

      expect(desde().value).toBe('2026-10-12');
      expect(hasta().value).toBe('2026-11-10');
    });

    it('un día solo se cuenta en singular', async () => {
      await ir('/gestor?desde=2026-11-10&hasta=2026-11-10');

      expect(texto()).toContain('(1 día)');
    });

    it('ofrece los cinco atajos', async () => {
      await ir('/gestor');

      expect(atajos().map((a) => a.textContent?.trim())).toEqual([
        'Últimos 7 días',
        'Últimos 30 días',
        'Últimos 90 días',
        'Este mes',
        'Mes anterior',
      ]);
    });

    it.each([
      ['Últimos 7 días', '2026-11-04', '2026-11-10'],
      ['Últimos 90 días', '2026-08-13', '2026-11-10'],
      ['Este mes', '2026-11-01', '2026-11-10'],
      ['Mes anterior', '2026-10-01', '2026-10-31'],
    ])('el atajo "%s" pone el periodo en la dirección y lo marca', async (nombre, inicio, fin) => {
      await ir('/gestor');

      atajos().find((a) => a.textContent?.trim() === nombre)!.click();
      await harness.fixture.whenStable();
      harness.detectChanges();

      expect(router.url).toBe(`/gestor?desde=${inicio}&hasta=${fin}`);
      expect(desde().value).toBe(inicio);
      expect(hasta().value).toBe(fin);
      expect(atajoMarcado()).toEqual([nombre]);
    });

    it('aplicar fechas escritas a mano las pone en la dirección y no marca ningún atajo', async () => {
      await ir('/gestor');
      escribir(desde(), '2026-09-01');
      escribir(hasta(), '2026-09-15');

      raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
      await harness.fixture.whenStable();
      harness.detectChanges();

      expect(router.url).toBe('/gestor?desde=2026-09-01&hasta=2026-09-15');
      expect(atajoMarcado()).toEqual([]);
      expect(texto()).toContain('(15 días)');
    });

    it.each([
      ['', '2026-11-10', 'Escribe la fecha de inicio.', 'periodo-desde'],
      ['2026-11-01', '', 'Escribe la fecha final.', 'periodo-hasta'],
      ['2026-11-05', '2026-11-01', 'La fecha de inicio no puede ser posterior a la final.', 'periodo-desde'],
      ['2026-11-01', '2026-11-11', 'La fecha final no puede ser posterior a hoy.', 'periodo-hasta'],
      ['2025-01-01', '2026-11-10', 'El periodo puede tener como máximo 366 días.', 'periodo-desde'],
    ])('un periodo inválido (%s a %s) no se envía y lo explica', async (inicio, fin, mensaje, campoConFoco) => {
      await ir('/gestor');
      escribir(desde(), inicio);
      escribir(hasta(), fin);

      raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
      await harness.fixture.whenStable();
      harness.detectChanges();

      expect(router.url).toBe('/gestor');
      expect(raiz().querySelector('#periodo-error')?.textContent).toContain(mensaje);
      expect(desde().getAttribute('aria-invalid')).toBe('true');
      expect(document.activeElement?.id).toBe(campoConFoco);
    });

    it('el error desaparece al elegir un atajo', async () => {
      await ir('/gestor');
      escribir(desde(), '');
      raiz().querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
      harness.detectChanges();
      expect(raiz().querySelector('#periodo-error')).not.toBeNull();

      atajos()[0].click();
      await harness.fixture.whenStable();
      harness.detectChanges();

      expect(raiz().querySelector('#periodo-error')).toBeNull();
    });

    it('las fechas no pasan de hoy', async () => {
      await ir('/gestor');

      expect(desde().max).toBe('2026-11-10');
      expect(hasta().max).toBe('2026-11-10');
    });
  });

  describe('las pestañas', () => {
    it('se llaman Demanda, Embudo de conversión y Distribución por tipo de prestador', async () => {
      await ir('/gestor');

      expect(pestanas().map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
        ['Demanda', '/gestor'],
        ['Embudo de conversión', '/gestor/embudo'],
        ['Distribución por tipo de prestador', '/gestor/distribucion'],
      ]);
    });

    it('conservan el periodo de la dirección en cada enlace', async () => {
      await ir('/gestor?desde=2026-10-01&hasta=2026-10-31');

      expect(pestanas().map((a) => a.getAttribute('href'))).toEqual([
        '/gestor?desde=2026-10-01&hasta=2026-10-31',
        '/gestor/embudo?desde=2026-10-01&hasta=2026-10-31',
        '/gestor/distribucion?desde=2026-10-01&hasta=2026-10-31',
      ]);
    });

    it.each([
      ['/gestor', 'Demanda'],
      ['/gestor/embudo', 'Embudo de conversión'],
      ['/gestor/distribucion', 'Distribución por tipo de prestador'],
    ])('en %s solo está marcada "%s"', async (url, nombre) => {
      await ir(url);
      await router.navigateByUrl(url);
      harness.detectChanges();

      const marcadas = pestanas().filter((a) => a.getAttribute('aria-current') === 'page');
      expect(marcadas.map((a) => a.textContent?.trim())).toEqual([nombre]);
    });

    it('cambiar de periodo no saca de la pestaña en que se está', async () => {
      await ir('/gestor/embudo');

      atajos()[0].click();
      await harness.fixture.whenStable();

      expect(router.url).toBe('/gestor/embudo?desde=2026-11-04&hasta=2026-11-10');
    });
  });

  describe('la nota de privacidad', () => {
    it('explica por qué aparece "menos de 3"', async () => {
      await ir('/gestor');

      expect(texto()).toContain('Para proteger la privacidad (RET-03)');
      expect(texto()).toContain('una cifra de 1 o 2 solicitudes no se muestra y aparece como «menos de 3»');
    });

    it('está en todas las pestañas', async () => {
      for (const url of ['/gestor/embudo', '/gestor/distribucion']) {
        await ir(url);
        expect(texto()).toContain('RET-03');
      }
    });
  });

  it('en inglés cambian el periodo, las pestañas y la nota', async () => {
    establecerIdioma('en', { recordar: false });
    await ir('/gestor');

    expect(raiz().querySelector('h1')?.textContent).toBe('Indicators dashboard');
    expect(atajos().map((a) => a.textContent?.trim())).toEqual([
      'Last 7 days',
      'Last 30 days',
      'Last 90 days',
      'This month',
      'Last month',
    ]);
    expect(raiz().querySelector('label[for="periodo-desde"]')?.textContent).toBe('From');
    expect(texto()).toContain('Period shown: from Oct 12, 2026 to Nov 10, 2026 (30 days).');
    expect(pestanas().map((a) => a.textContent?.trim())).toEqual([
      'Demand',
      'Conversion funnel',
      'Distribution by provider type',
    ]);
    expect(texto()).toContain('appears as “fewer than 3”');
  });
});
