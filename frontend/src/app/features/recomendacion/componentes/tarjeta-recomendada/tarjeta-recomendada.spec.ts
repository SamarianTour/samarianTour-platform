import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Recomendacion } from '../../../../core/api/modelos-recomendacion';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { TarjetaRecomendada } from './tarjeta-recomendada';

const RECOMENDACION: Recomendacion = {
  posicion: 2,
  oferta: {
    id: 'b0000000000000000000000a',
    nombre: 'Avistamiento de aves en Minca',
    tipo: 'ACTIVIDAD',
    zonaNombre: 'Minca',
    precioReferencia: 120000,
    unidadPrecio: 'por persona',
  },
  criterios: ['TIPO_ACTIVIDAD', 'NIVEL_ESFUERZO', 'PRECIO'],
  explicacion: 'Coincide con: avistamiento de fauna, esfuerzo bajo, precio dentro de tu presupuesto.',
};

describe('TarjetaRecomendada', () => {
  let fixture: ComponentFixture<TarjetaRecomendada>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const normal = (texto: string | null | undefined) => (texto ?? '').replace(/[  ]/g, ' ').replace(/\s+/g, ' ').trim();

  function crear(recomendacion: Recomendacion = RECOMENDACION): void {
    fixture = TestBed.createComponent(TarjetaRecomendada);
    fixture.componentRef.setInput('recomendacion', recomendacion);
    fixture.detectChanges();
  }

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('muestra la posición, el tipo, el nombre, la zona, el precio y la explicación', () => {
    crear();

    expect(normal(raiz().querySelector('.tarjeta-rec__encabezado')?.textContent)).toBe('Opción 2 Actividad');
    expect(raiz().querySelector('h3')?.textContent?.trim()).toBe('Avistamiento de aves en Minca');
    expect(raiz().querySelector('.tarjeta-rec__zona')?.textContent).toBe('Minca');
    expect(normal(raiz().querySelector('.tarjeta-rec__precio')?.textContent)).toBe('$ 120.000 por persona');
    expect(raiz().querySelector('.tarjeta-rec__explicacion')?.textContent).toContain('Coincide con: avistamiento de fauna');
  });

  it('el nombre lleva a la ficha de la oferta', () => {
    crear();

    expect(raiz().querySelector('h3 a')?.getAttribute('href')).toBe('/ofertas/b0000000000000000000000a');
  });

  it('es un solo enlace: el lector de pantalla no oye la tarjeta repetida', () => {
    crear();

    expect(raiz().querySelectorAll('a')).toHaveLength(1);
  });

  it('muestra los criterios que cumple, con nombre', () => {
    crear();

    const criterios = Array.from(raiz().querySelectorAll('.tarjeta-rec__criterios li')).map((c) => c.textContent?.trim());
    expect(criterios).toEqual(['Tipo de actividad', 'Esfuerzo', 'Precio']);
    expect(raiz().querySelector('.tarjeta-rec__criterios')?.getAttribute('aria-label')).toBe('Criterios que cumple');
  });

  it('sin criterios no dibuja una lista vacía: la explicación ya lo dice', () => {
    crear({ ...RECOMENDACION, criterios: [], explicacion: 'No cumple ningún criterio pedido.' });

    expect(raiz().querySelector('.tarjeta-rec__criterios')).toBeNull();
    expect(raiz().textContent).toContain('No cumple ningún criterio pedido.');
  });

  it('si el catálogo no dio la zona, no la inventa', () => {
    crear({ ...RECOMENDACION, oferta: { ...RECOMENDACION.oferta, zonaNombre: undefined } });

    expect(raiz().querySelector('.tarjeta-rec__zona')).toBeNull();
  });

  it('no muestra calificaciones, opiniones ni datos del prestador', () => {
    crear();

    expect(raiz().querySelector('app-estrellas')).toBeNull();
    expect(normal(raiz().textContent)).not.toMatch(/prestador|calificaci|opini/i);
  });

  it('en inglés cambia el tipo, el precio, la unidad y los criterios', () => {
    establecerIdioma('en', { recordar: false });
    crear({ ...RECOMENDACION, explicacion: 'Matches: wildlife viewing.' });

    expect(normal(raiz().querySelector('.tarjeta-rec__encabezado')?.textContent)).toBe('Option 2 Activity');
    expect(normal(raiz().querySelector('.tarjeta-rec__precio')?.textContent)).toBe('COP 120,000 per person');
    expect(Array.from(raiz().querySelectorAll('.tarjeta-rec__criterios li')).map((c) => c.textContent?.trim())).toEqual([
      'Activity type',
      'Effort',
      'Price',
    ]);
    // La explicación la escribe el servicio en el idioma pedido y se muestra tal cual.
    expect(raiz().querySelector('.tarjeta-rec__explicacion')?.textContent).toBe('Matches: wildlife viewing.');
  });
});
