import { Component, input } from '@angular/core';
import { TPipe } from '../../../../core/idioma/t.pipe';

/**
 * Los datos de un gráfico en una tabla, dentro de un desplegable ("Ver los datos en una tabla"). Es la versión accesible del gráfico
 * para los lectores de pantalla y también para quien prefiere leer las cifras; por eso es visible para todos y se abre con teclado.
 * La primera columna es el encabezado de cada fila. Los textos llegan ya traducidos y formateados.
 */
@Component({
  selector: 'app-tabla-datos',
  imports: [TPipe],
  templateUrl: './tabla-datos.html',
  styleUrl: './tabla-datos.css',
})
export class TablaDatos {
  /** Qué contiene la tabla: es su título para los lectores de pantalla. */
  readonly titulo = input.required<string>();
  readonly columnas = input.required<readonly string[]>();
  readonly filas = input.required<readonly (readonly string[])[]>();
}
