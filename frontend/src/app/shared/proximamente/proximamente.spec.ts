import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { establecerIdioma } from '../../core/idioma/idioma';
import { Proximamente } from './proximamente';

describe('Proximamente', () => {
  let fixture: ComponentFixture<Proximamente>;

  const raiz = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    fixture = TestBed.createComponent(Proximamente);
  });

  it('dice qué pantalla es y qué habrá en ella', () => {
    fixture.componentRef.setInput('codigo', 'P02.4');
    fixture.componentRef.setInput('nombre', 'app.proximamente.operador.nombre');
    fixture.componentRef.setInput('descripcion', 'app.proximamente.operador.descripcion');
    fixture.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toContain('Cola de solicitudes');
    expect(raiz().textContent).toContain('P02.4');
    expect(raiz().textContent).toContain('Aquí atenderás la cola de solicitudes de disponibilidad');
    expect(raiz().textContent).toContain('en construcción');
  });

  it('cambia de idioma al instante', () => {
    fixture.componentRef.setInput('nombre', 'app.proximamente.operador.nombre');
    fixture.detectChanges();

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toContain('Request queue');
    expect(raiz().textContent).toContain('under construction');
    expect(raiz().querySelector('a')?.textContent).toContain('Back to the catalog');
  });

  it('sin datos de la ruta muestra un texto general y no deja un código vacío', () => {
    fixture.detectChanges();

    expect(raiz().querySelector('h1')?.textContent).toContain('Esta pantalla');
    expect(raiz().querySelector('.rotulo')).toBeNull();
  });

  it('ofrece volver al catálogo', () => {
    fixture.detectChanges();

    expect(raiz().querySelector('a')?.getAttribute('href')).toBe('/');
  });
});
