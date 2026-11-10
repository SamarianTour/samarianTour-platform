import { ComponentFixture, TestBed } from '@angular/core/testing';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { TablaDatos } from './tabla-datos';

describe('TablaDatos', () => {
  let fixture: ComponentFixture<TablaDatos>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const celdas = (fila: number) =>
    Array.from(raiz().querySelectorAll('tbody tr')[fila].children).map((c) => c.textContent?.trim());

  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    fixture = TestBed.createComponent(TablaDatos);
    fixture.componentRef.setInput('titulo', 'Solicitudes por zona');
    fixture.componentRef.setInput('columnas', ['Zona', 'Tipo', 'Solicitudes']);
    fixture.componentRef.setInput('filas', [
      ['Tayrona', 'Actividad', '18'],
      ['Minca', 'Alojamiento', 'menos de 3'],
    ]);
    fixture.detectChanges();
  });

  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('está dentro de un desplegable cerrado que cualquiera puede abrir', () => {
    const detalles = raiz().querySelector('details')!;

    expect(detalles.open).toBe(false);
    expect(raiz().querySelector('summary')?.textContent?.trim()).toBe('Ver los datos en una tabla');
  });

  it('la tabla tiene título para los lectores de pantalla', () => {
    expect(raiz().querySelector('caption')?.textContent).toBe('Solicitudes por zona');
  });

  it('las columnas son encabezados de columna', () => {
    const encabezados = Array.from(raiz().querySelectorAll('thead th'));

    expect(encabezados.map((h) => h.textContent?.trim())).toEqual(['Zona', 'Tipo', 'Solicitudes']);
    expect(encabezados.every((h) => h.getAttribute('scope') === 'col')).toBe(true);
  });

  it('la primera celda de cada fila es su encabezado de fila', () => {
    const primera = raiz().querySelector('tbody tr')!.children[0];

    expect(primera.tagName).toBe('TH');
    expect(primera.getAttribute('scope')).toBe('row');
    expect(celdas(0)).toEqual(['Tayrona', 'Actividad', '18']);
    expect(celdas(1)).toEqual(['Minca', 'Alojamiento', 'menos de 3']);
  });

  it('el texto del desplegable sigue el idioma', () => {
    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(raiz().querySelector('summary')?.textContent?.trim()).toBe('View the data in a table');
  });
});
