import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { IdentidadService } from './core/identidad/identidad.service';
import { Avisos } from './shared/avisos/avisos';
import { Icono } from './shared/icono/icono';
import { UsuarioDemo } from './shared/usuario-demo/usuario-demo';

type Seccion = 'catalogo' | 'opiniones' | 'prestador';

/**
 * Armazón de la aplicación: cabecera con la navegación y el usuario demo, el contenido de la ruta y los avisos.
 */
@Component({
  imports: [RouterOutlet, RouterLink, Avisos, Icono, UsuarioDemo],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);

  protected readonly turista = inject(IdentidadService).turista;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /** La sección de la cabecera que se marca: la ficha pertenece al catálogo. */
  protected readonly seccion = computed<Seccion>(() => {
    const url = this.url();
    if (url.startsWith('/prestador')) {
      return 'prestador';
    }
    return url.startsWith('/mis-opiniones') ? 'opiniones' : 'catalogo';
  });
}
