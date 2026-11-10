import { Component, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { TPipe } from '../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../core/idioma/textos';
import { SesionService } from '../../core/sesion/sesion.service';
import { Icono } from '../icono/icono';
import { EnlacePanel, enlacePanelActivo } from './enlace-activo';

/**
 * El panel de un rol interno: barra lateral con el nombre de quien tiene la sesión, su rol y el menú, y a su lado la pantalla de
 * la ruta. En celular la barra pasa arriba y el menú se acomoda en filas. Lo usan el administrador y el gestor; cada panel solo
 * dice cuál es su rol, cómo se llama su menú y qué enlaces tiene. La ruta que lo contiene es la que exige el rol.
 */
@Component({
  selector: 'app-panel-lateral',
  imports: [RouterOutlet, RouterLink, Icono, TPipe],
  templateUrl: './panel-lateral.html',
  styleUrl: './panel-lateral.css',
})
export class PanelLateral {
  /** El nombre del rol de quien tiene la sesión, por ejemplo `etiquetas.rol.ADMINISTRADOR`. */
  readonly rol = input.required<ClaveTexto>();
  /** El título del menú, que además nombra la navegación para los lectores de pantalla. */
  readonly titulo = input.required<ClaveTexto>();
  readonly enlaces = input.required<readonly EnlacePanel[]>();

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

  protected activo(enlace: EnlacePanel): boolean {
    return enlacePanelActivo(enlace, this.url());
  }
}
