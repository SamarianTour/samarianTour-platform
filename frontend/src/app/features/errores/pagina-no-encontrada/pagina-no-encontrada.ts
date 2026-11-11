import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '../../../core/idioma/t.pipe';
import { Icono } from '../../../shared/icono/icono';

/**
 * "Página no encontrada": la pantalla de cualquier dirección que ninguna ruta reconoce (`**`). Antes esas direcciones
 * volvían al Inicio en silencio, y quien escribía mal una dirección no se enteraba de que se había equivocado.
 */
@Component({
  selector: 'app-pagina-no-encontrada',
  imports: [RouterLink, Icono, TPipe],
  templateUrl: './pagina-no-encontrada.html',
  styleUrl: './pagina-no-encontrada.css',
})
export class PaginaNoEncontrada {}
