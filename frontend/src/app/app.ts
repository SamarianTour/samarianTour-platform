import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { Avisos } from './shared/avisos/avisos';
import { Icono } from './shared/icono/icono';

/** Armazón de la aplicación: cabecera con la navegación, el contenido de la ruta y los avisos. */
@Component({
  imports: [RouterOutlet, RouterLink, Avisos, Icono],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  /** La aplicación tiene dos áreas: el panel del prestador y el catálogo, que incluye las fichas. */
  protected readonly enPrestador = computed(() => this.url().startsWith('/prestador'));
}
