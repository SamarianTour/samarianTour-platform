import { Component } from '@angular/core';
import { TPipe } from '../../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../../core/idioma/textos';
import { Icono, NombreIcono } from '../../../../shared/icono/icono';
import { ComoInicio } from '../como-inicio/como-inicio';

interface Bloque {
  readonly clase: string;
  readonly icono: NombreIcono;
  readonly titulo: ClaveTexto;
  readonly texto: ClaveTexto;
}

/**
 * Los tres bloques de "Viaja mejor y cuida el destino". Son solo informativos: no llevan enlaces ni botones. [SUPUESTO] Las
 * alertas, las zonas y las buenas prácticas tienen sus propias pantallas, pero el diseño aprobado no las enlaza desde aquí.
 */
export const BLOQUES: readonly Bloque[] = [
  {
    clase: 'alertas',
    icono: 'campana',
    titulo: 'turista.inicio.viaja.alertas',
    texto: 'turista.inicio.viaja.alertas.desc',
  },
  {
    clase: 'zonas',
    icono: 'brujula',
    titulo: 'turista.inicio.viaja.zonas',
    texto: 'turista.inicio.viaja.zonas.desc',
  },
  {
    clase: 'practicas',
    icono: 'hoja',
    titulo: 'turista.inicio.viaja.practicas',
    texto: 'turista.inicio.viaja.practicas.desc',
  },
];

/**
 * El bloque oscuro del Inicio: "Viaja mejor y cuida el destino" (tres círculos informativos), la transición con la hoja y
 * "Así funciona Samarian Tour" (`ComoInicio`, con sus cuatro pasos animados). El fondo oscuro continúa de una a otra.
 */
@Component({
  selector: 'app-destino-inicio',
  imports: [ComoInicio, Icono, TPipe],
  templateUrl: './destino-inicio.html',
  styleUrl: './destino-inicio.css',
})
export class DestinoInicio {
  protected readonly bloques = BLOQUES;
}
