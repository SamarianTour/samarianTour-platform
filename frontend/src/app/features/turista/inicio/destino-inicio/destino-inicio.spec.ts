import { TestBed } from '@angular/core/testing';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { DestinoInicio } from './destino-inicio';

describe('DestinoInicio', () => {
  function crear() {
    const fixture = TestBed.createComponent(DestinoInicio);
    fixture.detectChanges();
    return fixture;
  }

  const raiz = (f: { nativeElement: unknown }) => f.nativeElement as HTMLElement;

  beforeEach(() => establecerIdioma('es', { recordar: false }));
  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('muestra los tres bloques informativos, sin enlaces ni botones', () => {
    const fixture = crear();
    const bloques = raiz(fixture).querySelectorAll('.circulo-item');

    expect(Array.from(bloques).map((b) => b.querySelector('h3')?.textContent?.trim())).toEqual([
      'Alertas del destino',
      'Zonas muy visitadas',
      'Buenas prácticas',
    ]);
    expect(raiz(fixture).querySelector('.viaja a, .viaja button')).toBeNull();
    expect(raiz(fixture).querySelector('#viaja-titulo')?.textContent).toContain('Viaja mejor y cuida el destino');
  });

  it('los círculos, las curvas y la hoja son decorativos (aria-hidden)', () => {
    const fixture = crear();

    expect(raiz(fixture).querySelector('.curvas')?.getAttribute('aria-hidden')).toBe('true');
    expect(Array.from(raiz(fixture).querySelectorAll('.circulo')).every((c) => c.getAttribute('aria-hidden') === 'true')).toBe(
      true,
    );
    expect(raiz(fixture).querySelector('.union')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('"Así funciona" lleva sus anclas: #sobre-nosotros en el encabezado y #como-funciona en los pasos', () => {
    const fixture = crear();

    const encabezado = raiz(fixture).querySelector('#sobre-nosotros')!;
    expect(encabezado.querySelector('h2')?.textContent).toContain('Así funciona Samarian Tour');
    const pasos = raiz(fixture).querySelector('#como-funciona')!;
    expect(pasos.querySelectorAll('ol > li')).toHaveLength(4);
  });

  it('los pasos tienen su número decorativo y "Paso n:" para los lectores de pantalla', () => {
    const fixture = crear();
    const pasos = Array.from(raiz(fixture).querySelectorAll('.paso'));

    expect(pasos.map((p) => p.querySelector('.paso__num')?.textContent?.trim())).toEqual(['1', '2', '3', '4']);
    expect(pasos[1].querySelector('.paso__num')?.getAttribute('aria-hidden')).toBe('true');
    expect(pasos[1].querySelector('h3')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('Paso 2: Envía tu solicitud');
  });

  it('cambia de idioma con la pantalla abierta', () => {
    const fixture = crear();

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(raiz(fixture).querySelector('#viaja-titulo')?.textContent).toContain('Travel better');
    expect(raiz(fixture).querySelectorAll('.paso h3')[0].textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Step 1: Explore and choose',
    );
  });
});
