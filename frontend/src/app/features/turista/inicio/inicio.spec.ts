import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { establecerIdioma } from '../../../core/idioma/idioma';
import { RUTAS_TURISTA } from '../turista.routes';
import { Inicio } from './inicio';

const ZONAS = [
  {
    id: 'z1',
    nombre: 'Taganga',
    descripcion: 'Pueblo de pescadores.',
    areaProtegida: false,
    altaAfluencia: true,
    activa: true,
    buenasPracticas: [],
  },
  {
    id: 'z2',
    nombre: 'Tayrona',
    descripcion: 'Selva y mar.',
    areaProtegida: true,
    altaAfluencia: false,
    activa: true,
    buenasPracticas: [],
  },
];

describe('Inicio (P01.0)', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement!;
  const texto = () => (raiz().textContent ?? '').replace(/[  ]/g, ' ');
  const enlaces = () => Array.from(raiz().querySelectorAll<HTMLAnchorElement>('a'));

  async function abrir(zonas: unknown = ZONAS): Promise<void> {
    await harness.navigateByUrl('/', Inicio);
    const peticion = http.expectOne('/api/catalogo/zonas');
    if (zonas === 'error') {
      peticion.error(new ProgressEvent('error'));
    } else {
      peticion.flush(zonas as object);
    }
    harness.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '', component: Inicio }, ...RUTAS_TURISTA]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    establecerIdioma('es', { recordar: false });
    http.match(() => true).forEach((p) => p.error(new ProgressEvent('error')));
  });

  it('muestra el título, el buscador y las cuatro tarjetas de tipo, y solo pide las zonas', async () => {
    await abrir();

    expect(raiz().querySelector('h1')?.textContent).toContain('¿Qué quieres vivir en Santa Marta?');
    expect(raiz().querySelector('form[role="search"]')).not.toBeNull();
    expect(raiz().querySelectorAll('.tipos li')).toHaveLength(4);
    // Sin ofertas, sin destacados y sin llamar a recomendaciones.
    expect(http.match((p) => p.url === '/api/catalogo/ofertas')).toHaveLength(0);
    expect(http.match((p) => p.url.includes('/api/recomendacion'))).toHaveLength(0);
  });

  it('las tarjetas de tipo llevan al catálogo con el tipo, y la de paquetes a /paquetes', async () => {
    await abrir();

    const destinos = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.tipo')).map((a) => a.getAttribute('href'));
    expect(destinos).toEqual([
      '/catalogo?tipo=ACTIVIDAD',
      '/paquetes',
      '/catalogo?tipo=ALOJAMIENTO',
      '/catalogo?tipo=RESTAURANTE',
    ]);
  });

  describe('zonas reales', () => {
    it('muestra cada zona con su marca y la enlaza a su ficha', async () => {
      await abrir();

      const tarjetas = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('.zona'));
      expect(tarjetas.map((a) => a.getAttribute('href'))).toEqual(['/zonas/z1', '/zonas/z2']);
      expect(tarjetas[0].textContent).toContain('Taganga');
      expect(tarjetas[0].textContent).toContain('Alta afluencia');
      expect(tarjetas[1].textContent).toContain('Área protegida');
    });

    it('la fila se puede enfocar, tiene nombre y no avanza sola', async () => {
      await abrir();

      const fila = raiz().querySelector('.zonas')!;
      expect(fila.getAttribute('tabindex')).toBe('0');
      expect(fila.getAttribute('aria-label')).toBe('Zonas de Santa Marta');
      // Los botones anterior y siguiente son botones reales y tienen nombre.
      const botones = Array.from(raiz().querySelectorAll<HTMLButtonElement>('.zonas__boton'));
      expect(botones.map((b) => b.getAttribute('aria-label'))).toEqual(['Zonas anteriores', 'Zonas siguientes']);
    });

    it('los botones mueven la fila', async () => {
      await abrir();
      const fila = raiz().querySelector<HTMLElement>('.zonas')!;
      const mover = vi.fn();
      fila.scrollBy = mover;
      // En la prueba no hay diseño: se simula una fila más ancha que su ventana y ya desplazada, para que ambos botones sirvan.
      Object.defineProperty(fila, 'scrollWidth', { value: 1000, configurable: true });
      Object.defineProperty(fila, 'clientWidth', { value: 300, configurable: true });
      Object.defineProperty(fila, 'scrollLeft', { value: 100, configurable: true });
      fila.dispatchEvent(new Event('scroll'));
      harness.detectChanges();

      raiz().querySelectorAll<HTMLButtonElement>('.zonas__boton')[1].click();
      raiz().querySelectorAll<HTMLButtonElement>('.zonas__boton')[0].click();

      expect(mover).toHaveBeenCalledTimes(2);
      expect(mover.mock.calls[0][0].left).toBeGreaterThanOrEqual(0);
      expect(mover.mock.calls[1][0].left).toBeLessThanOrEqual(0);
    });

    it.each([['error'], [[]]])('si no hay zonas (%s) la sección no aparece y el resto sigue', async (respuesta) => {
      await abrir(respuesta);

      expect(raiz().querySelector('.zonas')).toBeNull();
      expect(texto()).not.toContain('Explora por zona');
      expect(raiz().querySelectorAll('.tipos li')).toHaveLength(4);
    });
  });

  describe('buscador', () => {
    const campo = () => raiz().querySelector<HTMLInputElement>('#inicio-texto')!;
    function enviar(valor: string): void {
      campo().value = valor;
      campo().dispatchEvent(new Event('input'));
      raiz().querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
      harness.detectChanges();
    }

    it('lleva al catálogo con el texto, sin los espacios de los extremos', async () => {
      await abrir();

      enviar('  buceo ');
      await harness.fixture.whenStable();

      expect(TestBed.inject(Router).url).toBe('/catalogo?texto=buceo');
    });

    it('con un solo carácter avisa y no navega', async () => {
      await abrir();

      enviar('b');

      expect(TestBed.inject(Router).url).toBe('/');
      expect(campo().getAttribute('aria-invalid')).toBe('true');
      expect(texto()).toContain('Escribe al menos 2 caracteres');
    });

    it('vacío lleva al catálogo completo', async () => {
      await abrir();

      enviar('');
      await harness.fixture.whenStable();

      expect(TestBed.inject(Router).url).toBe('/catalogo');
    });

    it('no deja escribir más de 100 caracteres', async () => {
      await abrir();

      expect(campo().getAttribute('maxlength')).toBe('100');
    });
  });

  it('ofrece recomendaciones con un enlace, sin consultar la IA', async () => {
    await abrir();

    const enlace = enlaces().find((a) => a.textContent?.includes('Pedir recomendaciones'));
    expect(enlace?.getAttribute('href')).toBe('/recomendaciones');
    expect(texto()).toContain('¿No sabes qué elegir?');
  });

  it('cambia a inglés con la pantalla abierta', async () => {
    await abrir();

    establecerIdioma('en', { recordar: false });
    harness.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toContain('What do you want to experience in Santa Marta?');
    expect(texto()).toContain('Find your plan');
    expect(texto()).toContain('Explore by area');
    expect(texto()).toContain('Not sure what to pick?');
    expect(texto()).toContain('Protected area');
    expect(texto()).not.toContain('Explora por zona');
  });
});
