import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../core/idioma/textos';
import { Icono } from '../icono/icono';

/**
 * Pantalla "en construcción" (proximamente.html): ocupa el lugar de una sección que llega en una rama posterior, para
 * que su ruta, su protección por rol y su enlace del menú ya existan. El nombre y la descripción llegan en los `data`
 * de la ruta como claves de texto, así que siguen el idioma.
 */
@Component({
  selector: 'app-proximamente',
  imports: [RouterLink, Icono, TPipe],
  templateUrl: './proximamente.html',
  styleUrl: './proximamente.css',
})
export class Proximamente {
  /** Código de la pantalla en el mapa del diseño, por ejemplo "P02.4". */
  readonly codigo = input('');
  readonly nombre = input<ClaveTexto>('compartido.proximamente.pantalla');
  /** Una línea que dice qué habrá aquí. */
  readonly descripcion = input<ClaveTexto>('compartido.proximamente.descripcion');
}
