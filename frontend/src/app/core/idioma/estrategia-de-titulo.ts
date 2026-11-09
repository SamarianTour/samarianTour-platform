import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { traducirDe } from './traducir';
import { idiomaActual } from './idioma';
import { TEXTOS } from './textos';

export const SUFIJO_DEL_TITULO = 'Samarian Tour';

/**
 * El título de la pestaña del navegador. El `title` de cada ruta es una clave de texto (por ejemplo
 * `app.titulo.catalogo`), no el texto: aquí se traduce, y se vuelve a traducir cuando cambia el idioma sin salir de la
 * pantalla. Una ruta sin título deja solo "Samarian Tour".
 */
@Injectable({ providedIn: 'root' })
export class EstrategiaDeTitulo extends TitleStrategy {
  private readonly titulo = inject(Title);
  private readonly clave = signal<string | undefined>(undefined);

  /** El título completo en el idioma vigente. */
  readonly texto = computed(() => {
    const clave = this.clave();
    return clave === undefined
      ? SUFIJO_DEL_TITULO
      : `${traducirDe(TEXTOS, idiomaActual(), clave)} · ${SUFIJO_DEL_TITULO}`;
  });

  constructor() {
    super();
    effect(() => this.titulo.setTitle(this.texto()));
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    this.clave.set(this.buildTitle(snapshot));
  }
}
