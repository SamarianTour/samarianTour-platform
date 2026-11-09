import { Component, inject } from '@angular/core';
import { Idioma } from '../../core/idioma/idioma';
import { IdiomaService } from '../../core/idioma/idioma.service';
import { TPipe } from '../../core/idioma/t.pipe';

/**
 * Selector ES | EN de la cabecera (RF-18, RF-19). Es un grupo de dos botones con `aria-pressed`: el idioma vigente
 * queda marcado y el cambio es inmediato, sin recargar. Cada botón lleva su propio `lang` para que los lectores de
 * pantalla lo pronuncien bien.
 */
@Component({
  selector: 'app-selector-idioma',
  imports: [TPipe],
  templateUrl: './selector-idioma.html',
  styleUrl: './selector-idioma.css',
})
export class SelectorIdioma {
  private readonly servicio = inject(IdiomaService);

  protected readonly idioma = this.servicio.idioma;
  protected readonly idiomas = this.servicio.idiomas;

  protected elegir(idioma: Idioma): void {
    this.servicio.establecer(idioma);
  }
}
