import { Component, signal } from '@angular/core';
import { Params, RouterLink } from '@angular/router';
import { TipoActividad } from '../../../../core/api/modelos';
import { TPipe } from '../../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../../core/idioma/textos';
import { Icono, NombreIcono } from '../../../../shared/icono/icono';
import { ClaveExperiencia, FOTO_DE_EXPERIENCIA } from '../medios';

interface Experiencia {
  readonly clave: ClaveExperiencia;
  readonly nombre: ClaveTexto;
  readonly descripcion: ClaveTexto;
  readonly icono: NombreIcono;
  /** Los parámetros de `/catalogo` que lleva su "Ver ofertas". */
  readonly consulta: Params;
}

function actividad(tipoActividad: TipoActividad): Params {
  return { tipo: 'ACTIVIDAD', tipoActividad };
}

/** El orden es el de la pantalla; la primera columna empieza abierta. */
export const EXPERIENCIAS: readonly Experiencia[] = [
  {
    clave: 'buceo',
    nombre: 'turista.inicio.exp.buceo',
    descripcion: 'turista.inicio.exp.buceo.desc',
    icono: 'olas',
    consulta: actividad('BUCEO_SNORKEL'),
  },
  {
    clave: 'trekking',
    nombre: 'turista.inicio.exp.trekking',
    descripcion: 'turista.inicio.exp.trekking.desc',
    icono: 'actividad',
    consulta: actividad('SENDERISMO'),
  },
  {
    clave: 'aves',
    nombre: 'turista.inicio.exp.aves',
    descripcion: 'turista.inicio.exp.aves.desc',
    icono: 'ave',
    consulta: actividad('AVISTAMIENTO_FAUNA'),
  },
  {
    clave: 'playas',
    nombre: 'turista.inicio.exp.playas',
    descripcion: 'turista.inicio.exp.playas.desc',
    icono: 'sol',
    consulta: actividad('PLAYA_RELAX'),
  },
  {
    // No hay un tipo de actividad para el café: se busca por texto, igual en los dos idiomas.
    clave: 'cafe',
    nombre: 'turista.inicio.exp.cafe',
    descripcion: 'turista.inicio.exp.cafe.desc',
    icono: 'taza',
    consulta: { texto: 'café' },
  },
];

/**
 * Experiencias para vivir en Santa Marta: un acordeón de cinco columnas con una sola abierta. La columna se abre al pasar el
 * mouse, con el foco (Tab) o al tocarla, y cada una tiene un botón con `aria-expanded`. En celular las columnas se apilan.
 */
@Component({
  selector: 'app-experiencias-inicio',
  imports: [RouterLink, Icono, TPipe],
  templateUrl: './experiencias-inicio.html',
  styleUrl: './experiencias-inicio.css',
})
export class ExperienciasInicio {
  protected readonly experiencias = EXPERIENCIAS;
  /** Posición de la columna abierta. */
  protected readonly abierta = signal(0);

  protected foto(clave: ClaveExperiencia): string | null {
    return FOTO_DE_EXPERIENCIA[clave];
  }

  protected abrir(posicion: number): void {
    this.abierta.set(posicion);
  }

  /** Solo el mouse abre al pasar por encima: un toque ya abre con el clic. */
  protected alPasarPor(evento: PointerEvent, posicion: number): void {
    if (evento.pointerType === 'mouse') {
      this.abrir(posicion);
    }
  }
}
