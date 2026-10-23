import { Component, computed, input } from '@angular/core';
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
  protected readonly descripcion = computed(
    () => `${this.valor().toLocaleString('es-CO', { maximumFractionDigits: 1 })} de 5 estrellas`,
  );
}
