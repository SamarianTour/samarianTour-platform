import { Component } from '@angular/core';
import { PaginaLegal, SeccionLegal } from '../pagina-legal/pagina-legal';

const SECCIONES: readonly SeccionLegal[] = [
  {
    titulo: 'legal.pqrsf.letras.titulo',
    lista: ['legal.pqrsf.p', 'legal.pqrsf.q', 'legal.pqrsf.r', 'legal.pqrsf.s', 'legal.pqrsf.f'],
  },
  { titulo: 'legal.pqrsf.como.titulo', parrafos: ['legal.pqrsf.como.p1'] },
  {
    titulo: 'legal.pqrsf.datos.titulo',
    lista: [
      'legal.pqrsf.datos.nombre',
      'legal.pqrsf.datos.tipo',
      'legal.pqrsf.datos.que',
      'legal.pqrsf.datos.oferta',
      'legal.pqrsf.datos.fecha',
    ],
  },
];

/** PQRSF, pública: qué es cada letra, cómo escribir y qué datos incluir. [SUPUESTO pendiente del equipo] No fija plazo de respuesta: el equipo debe definirlo antes de publicarlo. */
@Component({
  selector: 'app-pqrsf',
  imports: [PaginaLegal],
  template: '<app-pagina-legal [titulo]="titulo" [secciones]="secciones" />',
})
export class Pqrsf {
  protected readonly titulo = 'legal.pqrsf.titulo' as const;
  protected readonly secciones = SECCIONES;
}
