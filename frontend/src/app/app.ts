import { DOCUMENT } from '@angular/common';
import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { EnlaceNavegacion, enlaceActivo, enlacesDelRol } from './core/sesion/enlaces-por-rol';
import { IdiomaService } from './core/idioma/idioma.service';
import { TPipe } from './core/idioma/t.pipe';
import { SesionService } from './core/sesion/sesion.service';
import { Avisos } from './shared/avisos/avisos';
import { Icono } from './shared/icono/icono';
import { MenuUsuario } from './shared/menu-usuario/menu-usuario';
import { Pie } from './shared/pie/pie';
import { SelectorIdioma } from './shared/selector-idioma/selector-idioma';

/**
 * Armazón de la aplicación: cabecera con la navegación del rol y la cuenta (o, sin sesión, "Iniciar sesión" y
 * "Registrarse"), el contenido de la ruta y los avisos.
 */
@Component({
  imports: [RouterOutlet, RouterLink, Avisos, Icono, MenuUsuario, Pie, SelectorIdioma, TPipe],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionService);
  private readonly documento = inject(DOCUMENT);
  /** La ruta (sin parámetros) de la navegación anterior: null en la primera carga. */
  private rutaAnterior: string | null = null;

  constructor() {
    // Deja el atributo `lang` y la descripción del documento en el idioma elegido desde el primer momento.
    inject(IdiomaService);

    // WCAG 2.4.3: al pasar a otra pantalla, el foco va al contenido. Si no, quien navega con teclado o con lector de pantalla
    // seguiría en el enlace del menú que acaba de usar. No se mueve en la primera carga (queda el enlace "Saltar al
    // contenido"), ni cuando solo cambian los parámetros (filtros, páginas), ni si la pantalla ya puso el foco en su interior.
    this.router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe((evento) => {
        // El panel del menú en celular se cierra al navegar.
        this.menuAbierto.set(false);
        const ruta = evento.urlAfterRedirects.split(/[?#]/)[0];
        const contenido = this.documento.getElementById('contenido');
        if (this.rutaAnterior !== null && ruta !== this.rutaAnterior && !contenido?.contains(this.documento.activeElement)) {
          contenido?.focus({ preventScroll: true });
        }
        this.rutaAnterior = ruta;
      });
  }

  /** El panel de la cabecera en celular (hasta 52 rem; en escritorio no existe y el botón no se ve). */
  protected readonly menuAbierto = signal(false);
  private readonly botonMenu = viewChild<ElementRef<HTMLButtonElement>>('botonMenu');

  protected alternarMenu(): void {
    this.menuAbierto.update((abierto) => !abierto);
  }

  /** Escape cierra el panel y devuelve el foco al botón que lo abrió. */
  protected cerrarMenuConEscape(): void {
    if (this.menuAbierto()) {
      this.menuAbierto.set(false);
      this.botonMenu()?.nativeElement.focus();
    }
  }

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
