import { Component } from '@angular/core';
import { TPipe } from '../../../core/idioma/t.pipe';
import { CambiarComision } from './cambiar-comision';
import { ListaPagosTardios } from './lista-pagos-tardios';

/**
 * P05.4 Comisión y pagos (RF-82, RF-83, RF-49, RF-50): la comisión de la plataforma, con su cambio y su historial, y los pagos
 * tardíos que hay que devolver a mano. Cada parte se carga por su lado: si una falla, la otra se sigue viendo.
 */
@Component({
  selector: 'app-comision-pagos',
  imports: [TPipe, CambiarComision, ListaPagosTardios],
  template: `
    <section class="pagina">
      <header class="encabezado">
        <p class="rotulo">{{ 'admin.panel.control' | t }}</p>
        <h1>{{ 'app.titulo.comisionPagos' | t }}</h1>
        <p class="texto-suave">{{ 'admin.comision.intro' | t }}</p>
      </header>

      <div class="partes">
        <app-cambiar-comision />
        <app-lista-pagos-tardios />
      </div>
    </section>
  `,
  styles: `
    .partes {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }
  `,
})
export class ComisionPagos {}
