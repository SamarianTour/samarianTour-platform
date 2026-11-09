import {
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AvisosService } from '../../core/avisos/avisos.service';
import { TPipe } from '../../core/idioma/t.pipe';
import { traducir } from '../../core/idioma/traducir';
import { SesionService } from '../../core/sesion/sesion.service';
import { ROL } from '../etiquetas';
import { Icono } from '../icono/icono';

/**
 * Menú de la cuenta en la cabecera: el nombre de quien tiene la sesión abierta y, al abrirlo, su rol, "Mis datos" y
 * "Cerrar sesión". Es un botón que muestra u oculta el menú (no un menú de aplicación): se recorre con Tab, se cierra
 * con Escape, con un clic afuera o al salir del menú con el teclado.
 */
@Component({
  selector: 'app-menu-usuario',
  imports: [RouterLink, Icono, TPipe],
  templateUrl: './menu-usuario.html',
  styleUrl: './menu-usuario.css',
})
export class MenuUsuario {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly boton = viewChild.required<ElementRef<HTMLButtonElement>>('boton');

  protected readonly cuenta = this.sesion.cuenta;
  protected readonly inicial = computed(() =>
    (this.cuenta()?.nombre.trim().charAt(0) || '?').toUpperCase(),
  );
  protected readonly etiquetaRol = ROL;
  protected readonly abierto = signal(false);

  protected alternar(): void {
    this.abierto.update((abierto) => !abierto);
  }

  protected cerrar(): void {
    this.abierto.set(false);
  }

  protected cerrarSesion(): void {
    this.cerrar();
    this.sesion.cerrarSesion().subscribe(() => {
      this.avisos.mostrar(traducir('app.menu.sesionCerrada'));
      void this.router.navigateByUrl('/');
    });
  }

  @HostListener('document:click', ['$event'])
  protected alHacerClicAfuera(evento: Event): void {
    if (this.abierto() && !this.anfitrion.nativeElement.contains(evento.target as Node)) {
      this.cerrar();
    }
  }

  @HostListener('keydown.escape')
  protected alPulsarEscape(): void {
    if (this.abierto()) {
      this.cerrar();
      this.boton().nativeElement.focus();
    }
  }

  /** Al salir del menú con Tab, se cierra: ya no se está usando. */
  protected alPerderElFoco(evento: FocusEvent): void {
    const destino = evento.relatedTarget as Node | null;
    if (destino && !this.anfitrion.nativeElement.contains(destino)) {
      this.cerrar();
    }
  }
}
