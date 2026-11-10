import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { TPipe } from '../../../core/idioma/t.pipe';
import { SesionService } from '../../../core/sesion/sesion.service';
import { TIPO_PRESTADOR } from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';
import { EnlacePanel, enlacePanelActivo } from '../../../shared/panel-lateral/enlace-activo';

const RUTA_OFERTAS = '/prestador/ofertas';
const RUTA_NUEVA = '/prestador/ofertas/nueva';

/**
 * Las secciones del panel del prestador, en el orden del menú. Los nombres son los mismos títulos de las pestañas
 * (`app.titulo.*`). El inicio (`/prestador`) es el dashboard y solo se marca en su dirección exacta.
 */
export const ENLACES_PRESTADOR: readonly EnlacePanel[] = [
  { clave: 'app.titulo.inicioPrestador', ruta: '/prestador', icono: 'inicio', exacto: true },
  { clave: 'app.nav.misOfertas', ruta: RUTA_OFERTAS, icono: 'paquete' },
  { clave: 'app.titulo.publicarOferta', ruta: RUTA_NUEVA, icono: 'mas' },
  { clave: 'app.titulo.rntPrestador', ruta: '/prestador/rnt', icono: 'escudo' },
  { clave: 'app.titulo.perfilPublico', ruta: '/prestador/perfil', icono: 'perfil' },
  { clave: 'app.titulo.whatsappPrestador', ruta: '/prestador/whatsapp', icono: 'mensaje' },
  { clave: 'app.titulo.cuentaCobro', ruta: '/prestador/cuenta-cobro', icono: 'tarjeta' },
];

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
  protected readonly enlaces = ENLACES_PRESTADOR;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  private readonly enPublicar = computed(() => this.url().split(/[?#]/)[0] === RUTA_NUEVA);

  /** "Publicar oferta" solo se marca en su ruta; el resto de `/prestador/ofertas` pertenece a "Mis ofertas". */
  protected activo(enlace: EnlacePanel): boolean {
    if (enlace.ruta === RUTA_OFERTAS && this.enPublicar()) {
      return false;
    }
    return enlacePanelActivo(enlace, this.url());
  }
}
