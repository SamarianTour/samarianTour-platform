import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { establecerIdioma } from './idioma';
import { TPipe } from './t.pipe';

@Component({
  imports: [TPipe],
  template: `
    <p id="simple">{{ 'comun.cargando' | t }}</p>
    <p id="parametros">{{ 'comun.pagina' | t: { pagina: pagina(), total: 5 } }}</p>
    <p id="plural">{{ 'comun.minutos' | t: { n: minutos() } }}</p>
  `,
})
class Anfitrion {
  readonly pagina = signal(2);
  readonly minutos = signal(1);
}

describe('TPipe', () => {
  let fixture: ComponentFixture<Anfitrion>;

  const texto = (id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`#${id}`)?.textContent;

  beforeEach(() => {
    fixture = TestBed.createComponent(Anfitrion);
    fixture.detectChanges();
  });

  it('traduce una clave en el idioma vigente', () => {
    expect(texto('simple')).toBe('Cargando…');
  });

  it('cambia de idioma al instante, sin volver a crear la pantalla', () => {
    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(texto('simple')).toBe('Loading…');

    establecerIdioma('es', { recordar: false });
    fixture.detectChanges();

    expect(texto('simple')).toBe('Cargando…');
  });

  it('reemplaza los parámetros y los actualiza cuando cambian', () => {
    expect(texto('parametros')).toBe('Página 2 de 5');

    fixture.componentInstance.pagina.set(3);
    fixture.detectChanges();

    expect(texto('parametros')).toBe('Página 3 de 5');
  });

  it('con parámetros también cambia de idioma', () => {
    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();

    expect(texto('parametros')).toBe('Page 2 of 5');
  });

  it('elige singular o plural según la cantidad y el idioma', () => {
    expect(texto('plural')).toBe('1 minuto');

    fixture.componentInstance.minutos.set(15);
    fixture.detectChanges();
    expect(texto('plural')).toBe('15 minutos');

    establecerIdioma('en', { recordar: false });
    fixture.detectChanges();
    expect(texto('plural')).toBe('15 minutes');

    fixture.componentInstance.minutos.set(1);
    fixture.detectChanges();
    expect(texto('plural')).toBe('1 minute');
  });

  it('puede usarse como función', () => {
    expect(new TPipe().transform('comun.cerrar')).toBe('Cerrar');

    establecerIdioma('en', { recordar: false });

    expect(new TPipe().transform('comun.cerrar')).toBe('Close');
  });
});
