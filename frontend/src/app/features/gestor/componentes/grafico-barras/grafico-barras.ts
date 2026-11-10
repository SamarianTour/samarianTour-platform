import { Component, input } from '@angular/core';

/** Una barra del gráfico, con todo lo que se imprime ya listo: el gráfico no calcula ni traduce nada. */
export interface BarraGrafico {
  readonly id: string;
  /** Lo que se mide, por ejemplo el nombre de una zona. */
  readonly etiqueta: string;
  /** Una segunda línea más pequeña, por ejemplo el tipo de oferta. */
  readonly detalle?: string;
  /** Etiquetas pequeñas junto al nombre, con su clase: por ejemplo "Alta afluencia". */
  readonly marcas?: readonly { readonly texto: string; readonly clase: string }[];
  /** El largo de la barra, de 0 a 100. */
  readonly ancho: number;
  /** El valor escrito junto a la barra: "18" o "menos de 3". El valor siempre está escrito, no solo dibujado. */
  readonly valor: string;
  /** Un conteo que el núcleo omitió (RET-03): la barra va rayada, para no confundirla con una cantidad. */
  readonly omitida?: boolean;
}

/**
 * Barras horizontales hechas con HTML y CSS, sin librerías. Es accesible por diseño:
 * - Todo el gráfico es una sola imagen para los lectores de pantalla (`role="img"`) con un resumen en `etiqueta`; los datos completos
 *   van en una tabla junto a él (`TablaDatos`), así no se lee dos veces.
 * - El valor de cada barra está escrito junto a ella: el largo y el color no son lo único que informa.
 * - Una barra de conteo omitido va rayada, además de decir "menos de 3".
 */
@Component({
  selector: 'app-grafico-barras',
  templateUrl: './grafico-barras.html',
  styleUrl: './grafico-barras.css',
})
export class GraficoBarras {
  readonly barras = input.required<readonly BarraGrafico[]>();
  /** El resumen que oyen los lectores de pantalla en lugar del dibujo. */
  readonly etiqueta = input.required<string>();
}
