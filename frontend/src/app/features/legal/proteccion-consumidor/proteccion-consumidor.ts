import { Component } from '@angular/core';
import { PaginaLegal, SeccionLegal } from '../pagina-legal/pagina-legal';

const SECCIONES: readonly SeccionLegal[] = [
  { titulo: 'legal.proteccion.precio.titulo', parrafos: ['legal.proteccion.precio.p1'] },
  { titulo: 'legal.proteccion.estatuto.titulo', parrafos: ['legal.proteccion.estatuto.p1'] },
  { titulo: 'legal.proteccion.autoridad.titulo', parrafos: ['legal.proteccion.autoridad.p1'] },
  { titulo: 'legal.proteccion.problema.titulo', parrafos: ['legal.proteccion.problema.p1'] },
];

/** Protección al consumidor, pública: precio claro, Estatuto del Consumidor (Ley 1480 de 2011) y la SIC como autoridad. */
@Component({
  selector: 'app-proteccion-consumidor',
  imports: [PaginaLegal],
  template: '<app-pagina-legal [titulo]="titulo" [secciones]="secciones" />',
})
export class ProteccionConsumidor {
  protected readonly titulo = 'legal.proteccion.titulo' as const;
  protected readonly secciones = SECCIONES;
}
