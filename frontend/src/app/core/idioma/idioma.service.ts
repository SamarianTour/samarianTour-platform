import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { IDIOMAS, Idioma, LOCALE, establecerIdioma, idiomaActual } from './idioma';
import { traducir } from './traducir';

/**
 * El idioma de la interfaz para los componentes: cuál es, cambiarlo y el formato regional que le corresponde. Al
 * crearse y cada vez que cambia deja el documento en el mismo idioma: el atributo `lang` de `<html>` y la descripción de
 * la página. El estado vive en `core/idioma/idioma.ts`.
 */
@Injectable({ providedIn: 'root' })
export class IdiomaService {
  private readonly documento = inject(DOCUMENT);
  private readonly meta = inject(Meta);

  readonly idioma = idiomaActual;
  readonly idiomas = IDIOMAS;
  /** El formato regional del idioma vigente, por ejemplo `es-CO`, para `Intl` y los pipes de fecha y precio. */
  readonly locale = computed(() => LOCALE[idiomaActual()]);

  constructor() {
    this.aplicarAlDocumento();
  }

  /** Cambia el idioma al instante, sin recargar, y lo recuerda para la próxima visita. */
  establecer(idioma: Idioma): void {
    establecerIdioma(idioma);
    this.aplicarAlDocumento();
  }

  private aplicarAlDocumento(): void {
    this.documento.documentElement.lang = idiomaActual();
    this.meta.updateTag({ name: 'description', content: traducir('comun.descripcion') });
  }
}
