import { Component, input } from '@angular/core';
import { TipoOferta } from '../../core/api/modelos';

export type NombreIcono =
  | 'actividad'
  | 'paquete'
  | 'alojamiento'
  | 'restaurante'
  | 'ubicacion'
  | 'reloj'
  | 'personas'
  | 'silla'
  | 'esfuerzo'
  | 'hoja'
  | 'alerta'
  | 'info'
  | 'flecha'
  | 'chevron'
  | 'mas'
  | 'escudo'
  | 'externo'
  | 'estrella';

/** Icono con el que se representa cada tipo de oferta. */
export const ICONO_DE_TIPO: Record<TipoOferta, NombreIcono> = {
  ACTIVIDAD: 'actividad',
  PAQUETE: 'paquete',
  ALOJAMIENTO: 'alojamiento',
  RESTAURANTE: 'restaurante',
};

/**
 * Icono de trazo, dibujado en línea para no depender de una fuente ni de imágenes. Es decorativo:
 * los lectores de pantalla lo ignoran, así que el texto que lo acompaña debe bastar por sí solo.
 * Toma el color y el tamaño del texto que lo rodea.
 */
@Component({
  selector: 'app-icono',
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      @switch (nombre()) {
        @case ('actividad') {
          <path d="m8 3 4 8 5-5 5 15H2L8 3z" />
        }
        @case ('paquete') {
          <path d="m7.5 4.27 9 5.15" />
          <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
          <path d="m3.3 7 8.7 5 8.7-5" />
          <path d="M12 22V12" />
        }
        @case ('alojamiento') {
          <path d="M2 4v16" />
          <path d="M2 8h18a2 2 0 0 1 2 2v10" />
          <path d="M2 17h20" />
          <path d="M6 8v9" />
        }
        @case ('restaurante') {
          <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2" />
          <path d="M7 2v20" />
          <path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7" />
        }
        @case ('ubicacion') {
          <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
          <circle cx="12" cy="10" r="3" />
        }
        @case ('reloj') {
          <circle cx="12" cy="12" r="10" />
          <path d="M12 6v6l4 2" />
        }
        @case ('personas') {
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        }
        @case ('silla') {
          <circle cx="16" cy="4" r="1" />
          <path d="m18 19 1-7-6 1" />
          <path d="m5 8 3-3 5.5 3-2.36 3.5" />
          <path d="M4.24 14.5a5 5 0 0 0 6.88 6" />
          <path d="M13.76 17.5a5 5 0 0 0-6.88-6" />
        }
        @case ('esfuerzo') {
          <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
        }
        @case ('hoja') {
          <path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z" />
          <path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12" />
        }
        @case ('alerta') {
          <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
          <path d="M12 9v4" />
          <path d="M12 17h.01" />
        }
        @case ('info') {
          <circle cx="12" cy="12" r="10" />
          <path d="M12 16v-4" />
          <path d="M12 8h.01" />
        }
        @case ('flecha') {
          <path d="M5 12h14" />
          <path d="m12 5 7 7-7 7" />
        }
        @case ('chevron') {
          <path d="m9 18 6-6-6-6" />
        }
        @case ('mas') {
          <path d="M5 12h14" />
          <path d="M12 5v14" />
        }
        @case ('escudo') {
          <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z" />
          <path d="m9 12 2 2 4-4" />
        }
        @case ('estrella') {
          <path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
        }
        @case ('externo') {
          <path d="M15 3h6v6" />
          <path d="M10 14 21 3" />
          <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
      width: 1.25em;
      height: 1.25em;
    }
    svg {
      width: 100%;
      height: 100%;
    }
  `,
})
export class Icono {
  readonly nombre = input.required<NombreIcono>();
}
