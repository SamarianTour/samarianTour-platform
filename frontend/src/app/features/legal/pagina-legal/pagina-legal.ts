import { Component, input } from '@angular/core';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { Icono } from '../../../shared/icono/icono';
import { CORREO_DE_CONTACTO } from '../../../shared/pie/pie';

/** Un bloque de una página legal: un subtítulo con párrafos y, si hace falta, una lista. */
export interface SeccionLegal {
  readonly titulo: ClaveTexto;
  readonly parrafos?: readonly ClaveTexto[];
  readonly lista?: readonly ClaveTexto[];
}

/**
 * Estructura común de las páginas de texto del pie: título, el aviso de proyecto académico (siempre visible), las secciones
 * y el correo de contacto. Las páginas solo aportan sus textos.
 */
@Component({
  selector: 'app-pagina-legal',
  imports: [Icono, TPipe],
  templateUrl: './pagina-legal.html',
  styleUrl: '../legal.css',
})
export class PaginaLegal {
  readonly titulo = input.required<ClaveTexto>();
  readonly secciones = input.required<readonly SeccionLegal[]>();

  protected readonly correo = CORREO_DE_CONTACTO;
}
