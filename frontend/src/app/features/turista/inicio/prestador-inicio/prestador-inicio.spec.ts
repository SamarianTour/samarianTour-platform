import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { establecerIdioma } from '../../../../core/idioma/idioma';
import { PrestadorInicio } from './prestador-inicio';

describe('PrestadorInicio', () => {
  beforeEach(() => {
    establecerIdioma('es', { recordar: false });
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('invita a los prestadores y lleva al registro', () => {
    const fixture = TestBed.createComponent(PrestadorInicio);
    fixture.detectChanges();
    const raiz = fixture.nativeElement as HTMLElement;

    expect(raiz.querySelector('h2')?.textContent).toContain('¿Eres prestador turístico en Santa Marta?');
    const boton = raiz.querySelector<HTMLAnchorElement>('a')!;
    expect(boton.textContent).toContain('Registrarme y validar mi RNT');
    expect(boton.getAttribute('href')).toBe('/registro');
  });

  it('cambia de idioma con la pantalla abierta', () => {
    const fixture = TestBed.createComponent(PrestadorInicio);
    fixture.detectChanges();

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('a')?.textContent).toContain('Sign up and validate my RNT');
  });
});
