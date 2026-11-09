import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { EnlaceNavegacion, enlaceActivo, enlacesDelRol } from './core/sesion/enlaces-por-rol';
import { SesionService } from './core/sesion/sesion.service';
import { Avisos } from './shared/avisos/avisos';
import { Icono } from './shared/icono/icono';
import { MenuUsuario } from './shared/menu-usuario/menu-usuario';

/**
 * Armazón de la aplicación: cabecera con la navegación del rol y la cuenta (o, sin sesión, "Iniciar sesión" y
 * "Registrarse"), el contenido de la ruta y los avisos.
 */
@Component({
  imports: [RouterOutlet, RouterLink, Avisos, Icono, MenuUsuario],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionService);

  protected readonly autenticada = this.sesion.autenticada;
  /** Las secciones que ofrece la cabecera según el rol de la sesión. */
  protected readonly enlaces = computed(() => enlacesDelRol(this.sesion.rol()));

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /** La sección de la cabecera que se marca: por ejemplo, la ficha de una oferta pertenece al catálogo. */
  protected esActivo(enlace: EnlaceNavegacion): boolean {
    return enlaceActivo(enlace, this.url());
  }
}
