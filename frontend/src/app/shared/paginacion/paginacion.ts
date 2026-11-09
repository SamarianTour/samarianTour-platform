import { Component, computed, input, output } from '@angular/core';
import { TPipe } from '../../core/idioma/t.pipe';

/** Paginación con páginas que empiezan en 0, como en la API (DC-14). */
@Component({
  selector: 'app-paginacion',
  imports: [TPipe],
  template: `
    @if (totalPaginas() > 1) {
      <nav class="paginacion" [attr.aria-label]="'compartido.paginacion.etiqueta' | t">
        <button type="button" class="boton boton--secundario" [disabled]="pagina() === 0" (click)="ir(pagina() - 1)">
          {{ 'compartido.paginacion.anterior' | t }}
        </button>
        <span aria-live="polite">{{
          'comun.pagina' | t: { pagina: pagina() + 1, total: totalPaginas() }
        }}</span>
        <button type="button" class="boton boton--secundario" [disabled]="esUltima()" (click)="ir(pagina() + 1)">
          {{ 'compartido.paginacion.siguiente' | t }}
        </button>
      </nav>
    }
  `,
  styles: `
    .paginacion {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      flex-wrap: wrap;
      margin-top: 1.5rem;
    }
  `,
})
export class Paginacion {
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly cambiar = output<number>();

  protected readonly esUltima = computed(() => this.pagina() + 1 >= this.totalPaginas());

  protected ir(pagina: number): void {
    this.cambiar.emit(pagina);
  }
}
