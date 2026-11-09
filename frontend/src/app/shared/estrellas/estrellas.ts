import { Component, computed, input } from '@angular/core';
import { LOCALE, idiomaActual } from '../../core/idioma/idioma';
import { traducir } from '../../core/idioma/traducir';
import { Icono } from '../icono/icono';

/**
 * Una calificación de 1 a 5 dibujada con estrellas, solo para leer. Los lectores de pantalla oyen el texto, por
 * ejemplo "4,7 de 5 estrellas", no los iconos.
 */
@Component({
  selector: 'app-estrellas',
  imports: [Icono],
  template: `
    <span class="estrellas" role="img" [attr.aria-label]="descripcion()">
      @for (posicion of posiciones; track posicion) {
        <app-icono nombre="estrella" [class.estrella--llena]="posicion <= llenas()" />
      }
    </span>
  `,
  styles: `
    .estrellas {
      display: inline-flex;
      gap: 0.125rem;
      color: var(--enlace);
      vertical-align: middle;
    }
    app-icono {
      color: var(--borde-control);
    }
    app-icono.estrella--llena {
      color: var(--enlace);
    }
  `,
})
export class Estrellas {
  /** De 1 a 5; puede tener decimales, como un promedio. */
  readonly valor = input.required<number>();

  protected readonly posiciones = [1, 2, 3, 4, 5];
  protected readonly llenas = computed(() => Math.round(this.valor()));
  /** "4,7 de 5 estrellas" / "4.7 out of 5 stars": el número también sigue el formato del idioma. */
  protected readonly descripcion = computed(() => {
    const idioma = idiomaActual();
    return traducir('compartido.estrellas.descripcion', {
      valor: this.valor().toLocaleString(LOCALE[idioma], { maximumFractionDigits: 1 }),
    });
  });
}
