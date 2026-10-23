import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IdentidadService } from '../../core/identidad/identidad.service';
import { TIPO_PRESTADOR } from '../etiquetas';

/**
 * Selector "Usuario demo" de la cabecera: IDENTIDAD PROVISIONAL mientras no exista el inicio de sesión con JWT. Reúne
 * a los turistas y a los prestadores de los datos de demostración, porque cada uno usa partes distintas de la
 * aplicación: el turista opina en la ficha y ve sus opiniones; el prestador entra a su panel.
 */
@Component({
  selector: 'app-usuario-demo',
  template: `
    <label class="usuario-demo">
      <span class="usuario-demo__rotulo">Usuario demo</span>
      <select id="usuario-demo" #selector (change)="elegir(selector.value)">
        <optgroup label="Turistas">
          @for (turista of turistas; track turista.id) {
            <option [value]="turista.id" [selected]="turista.id === identidad.turista()?.id">
              {{ turista.nombre }} · Turista
            </option>
          }
        </optgroup>
        <optgroup label="Prestadores">
          @for (prestador of prestadores; track prestador.id) {
            <option [value]="prestador.id" [selected]="prestador.id === identidad.prestador()?.id">
              {{ prestador.nombre }} · {{ etiquetaTipo[prestador.tipo] }}
            </option>
          }
        </optgroup>
      </select>
    </label>
  `,
  styles: `
    .usuario-demo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .usuario-demo__rotulo {
      color: var(--texto-suave);
      font-size: 0.75rem;
      font-weight: 600;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      white-space: nowrap;
    }
    select {
      min-height: 44px;
      max-width: 15rem;
      border: 1px solid var(--borde-control);
      border-radius: 8px;
      padding: 0.375rem 0.625rem;
      background: var(--superficie);
      color: var(--texto);
      font: inherit;
      font-size: 0.875rem;
    }
  `,
})
export class UsuarioDemo {
  protected readonly identidad = inject(IdentidadService);
  private readonly router = inject(Router);

  protected readonly turistas = this.identidad.turistas;
  protected readonly prestadores = this.identidad.prestadores;
  protected readonly etiquetaTipo = TIPO_PRESTADOR;

  /**
   * Si se cambia de prestador dentro del panel, se vuelve a "Mis ofertas": lo que había en pantalla era del
   * anterior. Las demás pantallas se recargan solas con el usuario nuevo.
   */
  protected elegir(id: string): void {
    this.identidad.elegir(id);
    if (this.identidad.prestador() && this.router.url.startsWith('/prestador')) {
      void this.router.navigate(['/prestador/ofertas']);
    }
  }
}
