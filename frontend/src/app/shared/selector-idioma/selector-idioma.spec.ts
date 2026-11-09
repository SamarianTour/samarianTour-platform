import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CLAVE_IDIOMA, idiomaActual } from '../../core/idioma/idioma';
import { SelectorIdioma } from './selector-idioma';

describe('SelectorIdioma', () => {
  let fixture: ComponentFixture<SelectorIdioma>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const botones = () => Array.from(raiz().querySelectorAll<HTMLButtonElement>('button'));
  const boton = (codigo: string) => botones().find((b) => b.textContent?.trim() === codigo)!;

  beforeEach(() => {
    localStorage.clear();
    fixture = TestBed.createComponent(SelectorIdioma);
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('ofrece ES | EN y marca el idioma vigente', () => {
    expect(botones().map((b) => b.textContent?.trim())).toEqual(['ES', 'EN']);
    expect(raiz().textContent?.replace(/\s+/g, '')).toBe('ES|EN');
    expect(boton('ES').getAttribute('aria-pressed')).toBe('true');
    expect(boton('EN').getAttribute('aria-pressed')).toBe('false');
  });

  it('es un grupo con nombre, y cada botón declara su idioma', () => {
    expect(raiz().querySelector('[role="group"]')?.getAttribute('aria-label')).toBe('Idioma');
    expect(boton('ES').getAttribute('lang')).toBe('es');
    expect(boton('EN').getAttribute('lang')).toBe('en');
  });

  it('elegir EN cambia el idioma al instante, marca el botón y lo recuerda', () => {
    boton('EN').click();
    fixture.detectChanges();

    expect(idiomaActual()).toBe('en');
    expect(boton('EN').getAttribute('aria-pressed')).toBe('true');
    expect(boton('ES').getAttribute('aria-pressed')).toBe('false');
    expect(localStorage.getItem(CLAVE_IDIOMA)).toBe('en');
  });

  it('el nombre del grupo también cambia de idioma', () => {
    boton('EN').click();
    fixture.detectChanges();

    expect(raiz().querySelector('[role="group"]')?.getAttribute('aria-label')).toBe('Language');
  });

  it('cambia el atributo lang del documento y la descripción de la página', () => {
    boton('EN').click();

    expect(document.documentElement.lang).toBe('en');
    expect(
      document.head.querySelector('meta[name="description"]')?.getAttribute('content'),
    ).toContain('Sustainable tourism catalog');

    boton('ES').click();

    expect(document.documentElement.lang).toBe('es');
  });

  it('los botones son lo bastante grandes para tocarlos (44 px)', () => {
    // El tamaño sale de la hoja de estilos del componente; se comprueba que se haya aplicado.
    for (const b of botones()) {
      expect(getComputedStyle(b).minHeight).toBe('44px');
    }
  });
});
