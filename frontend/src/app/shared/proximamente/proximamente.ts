import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icono } from '../icono/icono';

/**
 * Pantalla "en construcción" (proximamente.html): ocupa el lugar de una sección que llega en una rama posterior, para
 * que su ruta, su protección por rol y su enlace del menú ya existan. Los textos llegan en los `data` de la ruta.
 */
@Component({
  selector: 'app-proximamente',
  imports: [RouterLink, Icono],
  templateUrl: './proximamente.html',
  styleUrl: './proximamente.css',
})
export class Proximamente {
  /** Código de la pantalla en el mapa del diseño, por ejemplo "P02.4". */
  readonly codigo = input('');
  readonly nombre = input('Esta pantalla');
  /** Una línea que dice qué habrá aquí. */
  readonly descripcion = input('La construimos en una de las próximas fases.');
}
