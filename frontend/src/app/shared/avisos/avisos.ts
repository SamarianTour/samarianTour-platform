import { Component, inject } from '@angular/core';
import { AvisosService } from '../../core/avisos/avisos.service';

/** Muestra los mensajes de confirmación en una esquina; los lectores de pantalla los anuncian. */
@Component({
  selector: 'app-avisos',
  template: `
    <div class="avisos" role="status" aria-live="polite">
      @for (aviso of avisos(); track aviso.id) {
        <div class="aviso">
          <span>{{ aviso.texto }}</span>
          <button type="button" class="aviso__cerrar" aria-label="Cerrar mensaje" (click)="cerrar(aviso.id)">
            ×
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .avisos {
      position: fixed;
      right: 1rem;
      bottom: 1rem;
      left: 1rem;
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 0.5rem;
      z-index: 1000;
      pointer-events: none;
    }
    .aviso {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      max-width: 28rem;
      padding: 0.5rem 0.5rem 0.5rem 1rem;
      border-radius: 10px;
      background: var(--texto);
      color: #fff;
      box-shadow: 0 8px 24px rgb(15 23 42 / 25%);
      pointer-events: auto;
    }
    .aviso__cerrar {
      min-width: 44px;
      min-height: 44px;
      border: 0;
      border-radius: 8px;
      background: transparent;
      color: #fff;
      font-size: 1.5rem;
      line-height: 1;
      cursor: pointer;
    }
  `,
})
export class Avisos {
  private readonly servicio = inject(AvisosService);
  protected readonly avisos = this.servicio.avisos;

  protected cerrar(id: number): void {
    this.servicio.cerrar(id);
  }
}
