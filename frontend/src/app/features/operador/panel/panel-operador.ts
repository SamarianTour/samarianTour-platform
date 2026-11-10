import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { TPipe } from '../../../core/idioma/t.pipe';
import { SesionService } from '../../../core/sesion/sesion.service';
import { Icono } from '../../../shared/icono/icono';
import { seccionDelOperador } from '../seccion';

/**
 * Panel del operador de disponibilidad: barra lateral con su nombre y la navegación (Cola, Historial y Avisos), y a su
 * lado la pantalla de la ruta. Solo lo abre quien tiene la sesión de un operador (la ruta lo exige).
 */
@Component({
  selector: 'app-panel-operador',
  imports: [RouterOutlet, RouterLink, Icono, TPipe],
  templateUrl: './panel-operador.html',
  styleUrl: './panel-operador.css',
})
export class PanelOperador {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionService);

  protected readonly cuenta = this.sesion.cuenta;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  protected readonly seccion = computed(() => seccionDelOperador(this.url()));
}
