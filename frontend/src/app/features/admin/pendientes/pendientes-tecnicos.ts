import { Component } from '@angular/core';
import { TPipe } from '../../../core/idioma/t.pipe';
import { ListaAvisosRechazados } from './lista-avisos-rechazados';
import { ListaEventosFallidos } from './lista-eventos-fallidos';

/**
 * Pendientes técnicos (DN-06, DN-21, DN-44): lo que el sistema no pudo resolver solo y alguien debe revisar. Dos listas independientes,
 * los avisos al catálogo que este rechazó (con reenvío) y los eventos de pago que fallaron cinco veces (con reintento). Cada una se
 * carga por su lado: si una falla, la otra se sigue viendo.
 */
@Component({
  selector: 'app-pendientes-tecnicos',
  imports: [TPipe, ListaAvisosRechazados, ListaEventosFallidos],
  template: `
    <section class="pagina">
      <header class="encabezado">
        <p class="rotulo">{{ 'admin.panel.control' | t }}</p>
        <h1>{{ 'app.titulo.pendientesTecnicos' | t }}</h1>
        <p class="texto-suave">{{ 'admin.pendientes.intro' | t }}</p>
      </header>

      <div class="partes">
        <app-lista-avisos-rechazados />
        <app-lista-eventos-fallidos />
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
export class PendientesTecnicos {}
