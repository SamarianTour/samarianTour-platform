import { TestBed } from '@angular/core/testing';
import { CLAVE_IDIOMA, establecerIdioma } from './idioma';
import { IdiomaService } from './idioma.service';

const descripcion = () =>
  document.querySelector('meta[name="description"]')?.getAttribute('content') ?? '';

describe('IdiomaService', () => {
  afterEach(() => {
    localStorage.removeItem(CLAVE_IDIOMA);
    document.querySelector('meta[name="description"]')?.remove();
  });

  it('al crearse deja el documento en el idioma vigente', () => {
    document.documentElement.lang = 'fr';

    TestBed.inject(IdiomaService);

    expect(document.documentElement.lang).toBe('es');
    expect(descripcion()).toContain('Catálogo de turismo sostenible en Santa Marta');
  });

  it('al crearse con el idioma en inglés, el documento queda en inglés', () => {
    establecerIdioma('en', { recordar: false });

    TestBed.inject(IdiomaService);

    expect(document.documentElement.lang).toBe('en');
    expect(descripcion()).toContain('Sustainable tourism catalog for Santa Marta');
  });

  it('expone los dos idiomas', () => {
    expect(TestBed.inject(IdiomaService).idiomas).toEqual(['es', 'en']);
  });

  describe('establecer', () => {
    it('cambia el idioma al instante', () => {
      const servicio = TestBed.inject(IdiomaService);

      servicio.establecer('en');

      expect(servicio.idioma()).toBe('en');
    });

    it('cambia el atributo lang de la página y su descripción', () => {
      const servicio = TestBed.inject(IdiomaService);

      servicio.establecer('en');
      expect(document.documentElement.lang).toBe('en');
      expect(descripcion()).toContain('Sustainable tourism catalog');

      servicio.establecer('es');
      expect(document.documentElement.lang).toBe('es');
      expect(descripcion()).toContain('Catálogo de turismo sostenible');
    });

    it('lo recuerda para la próxima visita', () => {
      TestBed.inject(IdiomaService).establecer('en');

      expect(localStorage.getItem(CLAVE_IDIOMA)).toBe('en');
    });

    it('no deja descripciones repetidas en la página', () => {
      const servicio = TestBed.inject(IdiomaService);

      servicio.establecer('en');
      servicio.establecer('es');
      servicio.establecer('en');

      expect(document.querySelectorAll('meta[name="description"]')).toHaveLength(1);
    });
  });

  describe('locale', () => {
    it('es el formato regional del idioma vigente y cambia con él', () => {
      const servicio = TestBed.inject(IdiomaService);
      expect(servicio.locale()).toBe('es-CO');

      servicio.establecer('en');

      expect(servicio.locale()).toBe('en-US');
    });
  });
});
