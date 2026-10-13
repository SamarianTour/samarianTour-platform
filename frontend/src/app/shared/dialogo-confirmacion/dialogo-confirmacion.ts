import { Component, ElementRef, input, output, viewChild } from '@angular/core';

/**
 * Diálogo de confirmación propio, sobre el elemento <dialog>: atrapa el foco, se cierra con Escape
 * y devuelve el foco al control que lo abrió. Reemplaza al confirm() del navegador.
 */
@Component({
  selector: 'app-dialogo-confirmacion',
  template: `
    <dialog #dialogo class="dialogo" aria-labelledby="dialogo-titulo" (close)="cerrado()">
      <h2 id="dialogo-titulo">{{ titulo() }}</h2>
      <p>{{ mensaje() }}</p>
      <div class="dialogo__acciones">
        <button type="button" class="boton boton--secundario" (click)="cancelar()">Cancelar</button>
        <button type="button" class="boton boton--peligro" (click)="aceptar()">{{ accion() }}</button>
      </div>
    </dialog>
  `,
  styles: `
    .dialogo {
      border: 1px solid var(--borde);
      border-radius: 12px;
      padding: 1.5rem;
      width: min(28rem, calc(100vw - 2rem));
      color: var(--texto);
    }
    .dialogo::backdrop {
      background: rgb(15 23 42 / 55%);
    }
    .dialogo h2 {
      margin: 0 0 0.5rem;
      font-size: 1.25rem;
    }
    .dialogo__acciones {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      flex-wrap: wrap;
      margin-top: 1.5rem;
    }
  `,
})
export class DialogoConfirmacion {
  readonly titulo = input.required<string>();
  readonly mensaje = input.required<string>();
  readonly accion = input('Confirmar');
  readonly confirmado = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private acepto = false;

  abrir(): void {
    this.acepto = false;
    this.dialogo().nativeElement.showModal();
  }

  protected cancelar(): void {
    this.dialogo().nativeElement.close();
  }

  protected aceptar(): void {
    this.acepto = true;
    this.dialogo().nativeElement.close();
  }

  /** Se dispara al cerrar por cualquier vía: botón, Escape o aceptar. */
  protected cerrado(): void {
    if (this.acepto) {
      this.acepto = false;
      this.confirmado.emit();
    }
  }
}
