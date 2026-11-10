import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CriterioAfinidad, Recomendacion } from '../../../../core/api/modelos-recomendacion';
import { TPipe } from '../../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../../core/idioma/textos';
import { TIPO_OFERTA } from '../../../../shared/etiquetas';
import { ICONO_DE_TIPO, Icono } from '../../../../shared/icono/icono';
import { PrecioPipe } from '../../../../shared/precio.pipe';
import { UnidadPipe } from '../../../../shared/unidad.pipe';

const CLAVE_DE_CRITERIO: Readonly<Record<CriterioAfinidad, ClaveTexto>> = {
  TIPO_OFERTA: 'recomendacion.criterio.TIPO_OFERTA',
  TIPO_ACTIVIDAD: 'recomendacion.criterio.TIPO_ACTIVIDAD',
  ZONA: 'recomendacion.criterio.ZONA',
  ACCESIBILIDAD: 'recomendacion.criterio.ACCESIBILIDAD',
  NIVEL_ESFUERZO: 'recomendacion.criterio.NIVEL_ESFUERZO',
  PRECIO: 'recomendacion.criterio.PRECIO',
};

/**
 * Una oferta recomendada: su nombre (que lleva a su ficha), su zona y precio de referencia, la explicación del criterio con que se
 * eligió y los criterios que cumple (RF-27). La usan las recomendaciones del asistente (P08.1) y las alternativas de la ficha
 * (P08.3), que tienen la misma forma. La explicación la escribe el servicio en el idioma pedido y se muestra tal cual. No lleva
 * ninguna calificación, opinión ni dato del prestador: el servicio nunca las usa para elegir (DR-03).
 */
@Component({
  selector: 'app-tarjeta-recomendada',
  imports: [RouterLink, Icono, TPipe, PrecioPipe, UnidadPipe],
  templateUrl: './tarjeta-recomendada.html',
  styleUrl: './tarjeta-recomendada.css',
})
export class TarjetaRecomendada {
  readonly recomendacion = input.required<Recomendacion>();

  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;
  protected readonly claveDeCriterio = CLAVE_DE_CRITERIO;
}
