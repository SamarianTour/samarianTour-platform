import { Component } from '@angular/core';
import { TPipe } from '../../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../../core/idioma/textos';

interface Paso {
  readonly titulo: ClaveTexto;
  readonly texto: ClaveTexto;
}

export const PASOS: readonly Paso[] = [
  { titulo: 'turista.inicio.como.paso1', texto: 'turista.inicio.como.paso1.desc' },
  { titulo: 'turista.inicio.como.paso2', texto: 'turista.inicio.como.paso2.desc' },
  { titulo: 'turista.inicio.como.paso3', texto: 'turista.inicio.como.paso3.desc' },
  { titulo: 'turista.inicio.como.paso4', texto: 'turista.inicio.como.paso4.desc' },
];

/**
 * "Así funciona Samarian Tour": el párrafo y los cuatro pasos. La línea se llena y los círculos se encienden uno a uno.
 * Va dentro del bloque oscuro de `DestinoInicio`, que le pone el fondo. El encabezado lleva `#sobre-nosotros` y los pasos,
 * `#como-funciona`: son los destinos de los enlaces del pie.
 */
@Component({
  selector: 'app-como-inicio',
  imports: [TPipe],
  templateUrl: './como-inicio.html',
  styleUrl: './como-inicio.css',
})
export class ComoInicio {
  protected readonly pasos = PASOS;
}
