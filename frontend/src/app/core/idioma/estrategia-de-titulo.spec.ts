import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, TitleStrategy, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { EstrategiaDeTitulo } from './estrategia-de-titulo';
import { establecerIdioma } from './idioma';

@Component({ template: '' })
class Pantalla {}

describe('EstrategiaDeTitulo', () => {
  let harness: RouterTestingHarness;

  const titulo = () => {
    TestBed.tick();
    return document.title;
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'catalogo', title: 'app.titulo.catalogo', component: Pantalla },
          { path: 'sin-titulo', component: Pantalla },
          {
            path: 'panel',
            component: Pantalla,
            children: [{ path: 'ofertas', title: 'app.titulo.misOfertas', component: Pantalla }],
          },
        ]),
        { provide: TitleStrategy, useExisting: EstrategiaDeTitulo },
      ],
    });
    harness = await RouterTestingHarness.create();
  });

  it('traduce la clave del título de la ruta y agrega el nombre del sitio', async () => {
    await harness.navigateByUrl('/catalogo');

    expect(titulo()).toBe('Catálogo · Samarian Tour');
  });

  it('cambia el título al cambiar de idioma, sin salir de la pantalla', async () => {
    await harness.navigateByUrl('/catalogo');
    expect(titulo()).toBe('Catálogo · Samarian Tour');

    establecerIdioma('en', { recordar: false });
    expect(titulo()).toBe('Catalog · Samarian Tour');

    establecerIdioma('es', { recordar: false });
    expect(titulo()).toBe('Catálogo · Samarian Tour');
  });

  it('una pantalla nueva se titula en el idioma vigente', async () => {
    establecerIdioma('en', { recordar: false });

    await harness.navigateByUrl('/panel/ofertas');

    expect(titulo()).toBe('My offers · Samarian Tour');
  });

  it('una ruta sin título deja solo el nombre del sitio', async () => {
    await harness.navigateByUrl('/catalogo');
    await harness.navigateByUrl('/sin-titulo');

    expect(titulo()).toBe('Samarian Tour');
  });

  it('una clave que no existe deja el hueco a la vista en lugar de un título vacío', async () => {
    TestBed.inject(Router).resetConfig([
      { path: 'rota', title: 'app.titulo.noExiste', component: Pantalla },
    ]);

    await harness.navigateByUrl('/rota');

    expect(titulo()).toBe('app.titulo.noExiste · Samarian Tour');
  });
});
