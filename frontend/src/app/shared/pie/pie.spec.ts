import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Component } from '@angular/core';
import { establecerIdioma, idiomaActual } from '../../core/idioma/idioma';
import { Pie } from './pie';

@Component({ template: '<app-pie />', imports: [Pie] })
class Anfitrion {}

describe('Pie', () => {
  let harness: RouterTestingHarness;

  const raiz = () => harness.routeNativeElement as HTMLElement;
  const enlace = (texto: string) =>
    Array.from(raiz().querySelectorAll<HTMLAnchorElement>('a')).find((a) => a.textContent?.includes(texto))!;

  beforeEach(async () => {
    localStorage.clear();
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: '', component: Anfitrion }])],
    });
    harness = await RouterTestingHarness.create('/');
  });

  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('muestra la marca, el lema, el correo y la ciudad', () => {
    expect(raiz().querySelector('.pie__marca')?.textContent).toContain('Samarian Tour');
    expect(raiz().textContent).toContain('Turismo sostenible en Santa Marta');
    expect(raiz().textContent).toContain('Santa Marta, Magdalena');
  });

  it('el correo es un enlace mailto', () => {
    expect(enlace('contacto@samariantour.com').getAttribute('href')).toBe('mailto:contacto@samariantour.com');
  });

  it('tiene los enlaces legales a sus rutas', () => {
    const legales = Array.from(raiz().querySelectorAll<HTMLAnchorElement>('nav[aria-labelledby="pie-legal"] a'));

    expect(legales.map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Términos y condiciones', '/terminos'],
      ['Protección al consumidor', '/proteccion-consumidor'],
      ['Política de tratamiento de datos (Ley 1581 de 2012)', '/politica-datos'],
      ['PQRSF', '/pqrsf'],
      ['Eliminar cuenta', '/mis-datos'],
    ]);
  });

  it('"Sobre nosotros" y "Cómo funciona" llevan al Inicio con su ancla', () => {
    expect(enlace('Sobre nosotros').getAttribute('href')).toBe('/#sobre-nosotros');
    expect(enlace('Cómo funciona').getAttribute('href')).toBe('/#como-funciona');
  });

  it('el enlace de Instagram se abre en otra pestaña con rel="noopener" y avisa en su nombre', () => {
    const instagram = raiz().querySelector<HTMLAnchorElement>('.pie__red')!;

    expect(instagram.getAttribute('href')).toBe('https://www.instagram.com/samariantour/');
    expect(instagram.getAttribute('target')).toBe('_blank');
    expect(instagram.getAttribute('rel')).toBe('noopener');
    expect(instagram.getAttribute('aria-label')).toBe('Instagram de Samarian Tour (abre en otra pestaña)');
    // Es la única red.
    expect(raiz().querySelectorAll('.pie__red')).toHaveLength(1);
  });

  it('la franja de abajo lleva los derechos y el selector ES | EN con el mismo servicio de idioma', () => {
    expect(raiz().querySelector('.pie__abajo p')?.textContent).toBe(
      '© 2026 Samarian Tour · Todos los derechos reservados · Proyecto académico · Universidad del Magdalena',
    );
    const en = Array.from(raiz().querySelectorAll<HTMLButtonElement>('.pie__abajo app-selector-idioma button')).find(
      (b) => b.textContent?.trim() === 'EN',
    )!;

    en.click();
    harness.detectChanges();

    expect(idiomaActual()).toBe('en');
    expect(raiz().querySelector('.pie__abajo p')?.textContent).toContain('All rights reserved · Academic project');
    expect(enlace('Terms and conditions').getAttribute('href')).toBe('/terminos');
    expect(raiz().querySelector('.pie__red')?.getAttribute('aria-label')).toBe(
      'Samarian Tour on Instagram (opens in a new tab)',
    );
  });

  it('agrupa los enlaces en dos navegaciones con nombre', () => {
    expect(Array.from(raiz().querySelectorAll('footer nav')).map((n) => n.getAttribute('aria-labelledby'))).toEqual([
      'pie-info',
      'pie-legal',
    ]);
  });
});
