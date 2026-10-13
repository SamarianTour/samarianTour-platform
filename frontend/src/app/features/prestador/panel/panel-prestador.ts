import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { IdentidadService } from '../../../core/identidad/identidad.service';
import { TIPO_PRESTADOR } from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';

const RUTA_NUEVA = '/prestador/ofertas/nueva';

/**
 * Panel del prestador: barra lateral con su identidad y la navegación, y a su lado la pantalla de la ruta.
 * El selector "Prestador demo" es IDENTIDAD PROVISIONAL mientras no exista el inicio de sesión con JWT.
 */
@Component({
  selector: 'app-panel-prestador',
  imports: [RouterOutlet, RouterLink, Icono],
  templateUrl: './panel-prestador.html',
  styleUrl: './panel-prestador.css',
})
export class PanelPrestador {
  private readonly identidad = inject(IdentidadService);
  private readonly router = inject(Router);

  protected readonly prestadores = this.identidad.prestadores;
  protected readonly prestador = this.identidad.prestador;
  protected readonly etiquetaTipo = TIPO_PRESTADOR;
  protected readonly inicial = computed(() => this.prestador().nombre.charAt(0).toUpperCase());

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  /** "Publicar oferta" solo se marca en su ruta; el resto del panel pertenece a "Mis ofertas". */
  protected readonly enPublicar = computed(() => this.url().split('?')[0] === RUTA_NUEVA);

  /** Al cambiar de prestador se vuelve a "Mis ofertas": lo que había en pantalla era del anterior. */
  protected elegir(id: string): void {
    this.identidad.elegir(id);
    void this.router.navigate(['/prestador/ofertas']);
  }
}
