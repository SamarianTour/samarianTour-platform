import { Component } from '@angular/core';
import { PaginaLegal, SeccionLegal } from '../pagina-legal/pagina-legal';

const SECCIONES: readonly SeccionLegal[] = [
  { titulo: 'legal.terminos.que.titulo', parrafos: ['legal.terminos.que.p1', 'legal.terminos.que.p2'] },
  {
    titulo: 'legal.terminos.solicitud.titulo',
    parrafos: ['legal.terminos.solicitud.p1', 'legal.terminos.solicitud.p2'],
  },
  { titulo: 'legal.terminos.pago.titulo', parrafos: ['legal.terminos.pago.p1'] },
  { titulo: 'legal.terminos.comision.titulo', parrafos: ['legal.terminos.comision.p1'] },
];

/** Términos y condiciones, pública. [SUPUESTO] Texto informativo de un proyecto académico, sin plazos ni compromisos legales. */
@Component({
  selector: 'app-terminos',
  imports: [PaginaLegal],
  template: '<app-pagina-legal [titulo]="titulo" [secciones]="secciones" />',
})
export class Terminos {
  protected readonly titulo = 'legal.terminos.titulo' as const;
  protected readonly secciones = SECCIONES;
}
