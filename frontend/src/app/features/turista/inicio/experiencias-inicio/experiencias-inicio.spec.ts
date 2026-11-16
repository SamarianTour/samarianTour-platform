import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { FOTO_DE_EXPERIENCIA } from '../medios';
import { ExperienciasInicio } from './experiencias-inicio';

describe('ExperienciasInicio', () => {
  const foto = FOTO_DE_EXPERIENCIA as Record<string, string | null>;
  const originales = { ...foto };

  function crear() {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(ExperienciasInicio);
    fixture.detectChanges();
    return fixture;
  }

  const raiz = (f: { nativeElement: unknown }) => f.nativeElement as HTMLElement;
  const botones = (f: { nativeElement: unknown }) =>
    Array.from(raiz(f).querySelectorAll<HTMLButtonElement>('.panel__boton'));
  const expandidos = (f: { nativeElement: unknown }) => botones(f).map((b) => b.getAttribute('aria-expanded'));

  beforeEach(() => establecerIdioma('es', { recordar: false }));

  afterEach(() => {
    Object.assign(foto, originales);
    establecerIdioma('es', { recordar: false });
  });

  it('tiene cinco columnas con la primera abierta', () => {
    const fixture = crear();

    expect(botones(fixture)).toHaveLength(5);
    expect(expandidos(fixture)).toEqual(['true', 'false', 'false', 'false', 'false']);
    expect(raiz(fixture).querySelectorAll('.panel.abierto')).toHaveLength(1);
  });

  it('al hacer clic en una columna, esa se abre y las demás se cierran (aria-expanded)', () => {
    const fixture = crear();

    botones(fixture)[2].click();
    fixture.detectChanges();

    expect(expandidos(fixture)).toEqual(['false', 'false', 'true', 'false', 'false']);
    expect(raiz(fixture).querySelectorAll('.panel')[2].classList.contains('abierto')).toBe(true);
  });

  it('con el foco (Tab) la columna se abre', () => {
    const fixture = crear();

    botones(fixture)[3].dispatchEvent(new FocusEvent('focus'));
    fixture.detectChanges();

    expect(expandidos(fixture)).toEqual(['false', 'false', 'false', 'true', 'false']);
  });

  it('el mouse la abre al pasar por encima, pero un toque no (ese abre con el clic)', () => {
    const fixture = crear();
    const paneles = Array.from(raiz(fixture).querySelectorAll('.panel'));
    const entrar = (panel: Element, tipo: string) => {
      const evento = new Event('pointerenter');
      Object.defineProperty(evento, 'pointerType', { value: tipo });
      panel.dispatchEvent(evento);
      fixture.detectChanges();
    };

    entrar(paneles[1], 'touch');
    expect(expandidos(fixture)[0]).toBe('true');

    entrar(paneles[1], 'mouse');
    expect(expandidos(fixture)).toEqual(['false', 'true', 'false', 'false', 'false']);
  });

  it('solo la columna abierta deja su enlace al alcance del teclado (las demás son inert)', () => {
    const fixture = crear();
    const inerte = () =>
      Array.from(raiz(fixture).querySelectorAll('.panel__detalle')).map((d) => d.hasAttribute('inert'));

    expect(inerte()).toEqual([false, true, true, true, true]);
    botones(fixture)[4].click();
    fixture.detectChanges();
    expect(inerte()).toEqual([true, true, true, true, false]);
  });

  it('cada "Ver ofertas" lleva al catálogo con su tipo de actividad; el café, con texto', () => {
    const fixture = crear();

    const destinos = Array.from(raiz(fixture).querySelectorAll<HTMLAnchorElement>('.panel__ver')).map((a) =>
      a.getAttribute('href'),
    );
    expect(destinos).toEqual([
      '/catalogo?tipo=ACTIVIDAD&tipoActividad=BUCEO_SNORKEL',
      '/catalogo?tipo=ACTIVIDAD&tipoActividad=SENDERISMO',
      '/catalogo?tipo=ACTIVIDAD&tipoActividad=AVISTAMIENTO_FAUNA',
      '/catalogo?tipo=ACTIVIDAD&tipoActividad=PLAYA_RELAX',
      '/catalogo?texto=caf%C3%A9',
    ]);
  });

  it('el café busca por el mismo texto en inglés, y los textos cambian de idioma', () => {
    const fixture = crear();
    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(raiz(fixture).querySelector('h2')?.textContent).toContain('Experiences to live in Santa Marta');
    const cafe = raiz(fixture).querySelectorAll<HTMLAnchorElement>('.panel__ver')[4];
    expect(cafe.getAttribute('href')).toBe('/catalogo?texto=caf%C3%A9');
    expect(cafe.getAttribute('aria-label')).toBe('See offers: Coffee and farms in Minca');
  });

  it('con las fotos de medios.ts, cada columna pone la suya, decorativa y con carga perezosa', () => {
    const fixture = crear();

    const imagenes = Array.from(raiz(fixture).querySelectorAll<HTMLImageElement>('.panel__foto'));
    expect(imagenes.map((i) => i.getAttribute('src'))).toEqual([
      '/inicio/exp-buceo.webp',
      '/inicio/exp-trekking.webp',
      '/inicio/exp-aves.webp',
      '/inicio/exp-playas.webp',
      '/inicio/exp-cafe.webp',
    ]);
    expect(imagenes.every((i) => i.getAttribute('alt') === '' && i.getAttribute('loading') === 'lazy')).toBe(true);
  });

  it('una experiencia sin foto (null) muestra su degradado de respaldo y no pone imagen', () => {
    foto['buceo'] = null;
    const fixture = crear();

    const paneles = raiz(fixture).querySelectorAll('.panel');
    expect(paneles[0].querySelector('.panel__foto')).toBeNull();
    expect(paneles[0].querySelector('.panel__fondo')?.classList.contains('exp-buceo')).toBe(true);
    expect(paneles[1].querySelector('.panel__foto')).not.toBeNull();
  });
});
