import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { HeroInicio } from './hero-inicio';

describe('HeroInicio', () => {
  const raiz = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;

  function crear() {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(HeroInicio);
    fixture.detectChanges();
    return fixture;
  }

  /** Simula las preferencias del navegador: menos movimiento y ahorro de datos. */
  function preferencias(opciones: { reducido?: boolean; ahorro?: boolean }): void {
    vi.stubGlobal('matchMedia', (consulta: string) => ({
      matches: opciones.reducido === true && consulta.includes('prefers-reduced-motion'),
      media: consulta,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    }));
    Object.defineProperty(navigator, 'connection', {
      value: { saveData: opciones.ahorro === true },
      configurable: true,
    });
  }

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    preferencias({});
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    delete (navigator as { connection?: unknown }).connection;
    establecerIdioma('es', { recordar: false });
  });

  it('carga el video sin sonido, con poster y sin precargarlo entero', () => {
    const fixture = crear();

    const video = raiz(fixture).querySelector('video')!;
    expect(video).not.toBeNull();
    // Se fija por propiedad: sin ella, el navegador bloquea el autoplay.
    expect(video.muted).toBe(true);
    expect(video.autoplay).toBe(true);
    expect(video.loop).toBe(true);
    expect(video.getAttribute('playsinline')).not.toBeNull();
    expect(video.getAttribute('preload')).toBe('metadata');
    expect(video.getAttribute('poster')).toBe('/inicio/hero-poster.jpg');
  });

  it('con prefers-reduced-motion no carga el video: se ve solo el poster, sin botón', () => {
    preferencias({ reducido: true });
    const fixture = crear();

    expect(raiz(fixture).querySelector('video')).toBeNull();
    expect(raiz(fixture).querySelector('.hero__medio img')?.getAttribute('src')).toBe('/inicio/hero-poster.jpg');
    expect(raiz(fixture).querySelector('.hero__control')).toBeNull();
  });

  it('con ahorro de datos tampoco carga el video', () => {
    preferencias({ ahorro: true });
    const fixture = crear();

    expect(raiz(fixture).querySelector('video')).toBeNull();
    expect(raiz(fixture).querySelector('.hero__medio img')).not.toBeNull();
  });

  it('el control es solo un ícono: pausa y reanuda el video, y cambia aria-pressed y su nombre accesible', () => {
    const fixture = crear();
    const video = raiz(fixture).querySelector('video')!;
    video.pause = vi.fn();
    video.play = vi.fn(() => Promise.resolve());
    const boton = () => raiz(fixture).querySelector<HTMLButtonElement>('.hero__control')!;

    expect(boton().getAttribute('aria-pressed')).toBe('false');
    expect(boton().getAttribute('aria-label')).toBe('Pausar video');
    // Solo ícono: sin texto visible.
    expect(boton().textContent?.trim()).toBe('');
    expect(boton().querySelector('app-icono')).not.toBeNull();

    boton().click();
    fixture.detectChanges();
    expect(boton().getAttribute('aria-pressed')).toBe('true');
    expect(boton().getAttribute('aria-label')).toBe('Reproducir video');
    expect(video.pause).toHaveBeenCalledTimes(1);

    boton().click();
    fixture.detectChanges();
    expect(boton().getAttribute('aria-pressed')).toBe('false');
    expect(boton().getAttribute('aria-label')).toBe('Pausar video');
    expect(video.play).toHaveBeenCalledTimes(1);
  });

  it('el nombre accesible del control también sale en inglés', () => {
    const fixture = crear();
    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(raiz(fixture).querySelector('.hero__control')!.getAttribute('aria-label')).toBe('Pause video');
    establecerIdioma('es', { recordar: false });
  });

  it('el video y el velo son decorativos y la flecha lleva a #encuentra', () => {
    const fixture = crear();

    expect(raiz(fixture).querySelector('.hero__medio')?.getAttribute('aria-hidden')).toBe('true');
    const flecha = raiz(fixture).querySelector<HTMLAnchorElement>('.hero__bajar')!;
    expect(flecha.getAttribute('href')).toContain('#encuentra');
    expect(flecha.getAttribute('aria-label')).toBe('Bajar a Encuentra tu plan');
  });

  it('la franja de confianza tiene sus tres promesas, y cambia de idioma', () => {
    const fixture = crear();
    const titulos = () => Array.from(raiz(fixture).querySelectorAll('.confianza h2')).map((h) => h.textContent?.trim());

    expect(titulos()).toEqual(['Prestadores verificados', 'Precio claro', 'Turismo responsable']);

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();
    expect(titulos()).toEqual(['Verified providers', 'Clear pricing', 'Responsible tourism']);
  });
});
