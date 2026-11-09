import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { TPipe } from '../../../core/idioma/t.pipe';
import { SesionService } from '../../../core/sesion/sesion.service';
import { TIPO_PRESTADOR } from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';

const RUTA_NUEVA = '/prestador/ofertas/nueva';

/**
 * Panel del prestador: barra lateral con su nombre, su tipo y la navegación, y a su lado la pantalla de la ruta. Solo
 * lo abre quien tiene la sesión de un prestador (la ruta lo exige). El tipo viene del token y falta mientras el
 * prestador no registre su RNT.
 */
@Component({
  selector: 'app-panel-prestador',
  imports: [RouterOutlet, RouterLink, Icono, TPipe],
  templateUrl: './panel-prestador.html',
  styleUrl: './panel-prestador.css',
})
export class PanelPrestador {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionService);

  protected readonly cuenta = this.sesion.cuenta;
  protected readonly tipo = this.sesion.tipoPrestador;
  protected readonly etiquetaTipo = TIPO_PRESTADOR;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  /** "Publicar oferta" solo se marca en su ruta; el resto del panel pertenece a "Mis ofertas". */
  protected readonly enPublicar = computed(() => this.url().split('?')[0] === RUTA_NUEVA);
}
