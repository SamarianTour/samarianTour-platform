import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../core/idioma/textos';
import { Icono } from '../icono/icono';
import { SelectorIdioma } from '../selector-idioma/selector-idioma';

interface EnlaceDelPie {
  readonly texto: ClaveTexto;
  readonly ruta: string;
  readonly fragmento?: string;
}

export const CORREO_DE_CONTACTO = 'contacto@samariantour.com';
export const INSTAGRAM = 'https://www.instagram.com/samariantour/';

/** Las dos anclas viven en el Inicio (`#sobre-nosotros` y `#como-funciona`); desde otra pantalla el enlace vuelve a `/`. */
export const ENLACES_DE_INFORMACION: readonly EnlaceDelPie[] = [
  { texto: 'app.pie.info.nosotros', ruta: '/', fragmento: 'sobre-nosotros' },
  { texto: 'app.pie.info.como', ruta: '/', fragmento: 'como-funciona' },
];

export const ENLACES_LEGALES: readonly EnlaceDelPie[] = [
  { texto: 'app.pie.legal.terminos', ruta: '/terminos' },
  { texto: 'app.pie.legal.proteccion', ruta: '/proteccion-consumidor' },
  { texto: 'app.pie.legal.datos', ruta: '/politica-datos' },
  { texto: 'app.pie.legal.pqrsf', ruta: '/pqrsf' },
  { texto: 'app.pie.legal.eliminar', ruta: '/mis-datos' },
];

/**
 * Pie de página de todas las vistas: la marca, la información, lo legal, las redes y el selector ES | EN (el mismo servicio de
 * idioma de la cabecera). Entra con un degradado desde el color de la página y el contenido sube suave al aparecer.
 */
@Component({
  selector: 'app-pie',
  imports: [RouterLink, Icono, SelectorIdioma, TPipe],
  templateUrl: './pie.html',
  styleUrl: './pie.css',
})
export class Pie {
  protected readonly correo = CORREO_DE_CONTACTO;
  protected readonly instagram = INSTAGRAM;
  protected readonly informacion = ENLACES_DE_INFORMACION;
  protected readonly legales = ENLACES_LEGALES;
}
