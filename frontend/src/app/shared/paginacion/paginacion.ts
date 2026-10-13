import { Component, computed, input, output } from '@angular/core';

/** Paginación con páginas que empiezan en 0, como en la API (DC-14). */
@Component({
  selector: 'app-paginacion',
  template: `
    @if (totalPaginas() > 1) {
      <nav class="paginacion" aria-label="Paginación">
        <button type="button" class="boton boton--secundario" [disabled]="pagina() === 0" (click)="ir(pagina() - 1)">
          Anterior
        </button>
        <span aria-live="polite">Página {{ pagina() + 1 }} de {{ totalPaginas() }}</span>
        <button type="button" class="boton boton--secundario" [disabled]="esUltima()" (click)="ir(pagina() + 1)">
          Siguiente
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
