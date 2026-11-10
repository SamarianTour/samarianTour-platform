import { Component, computed, input } from '@angular/core';
import { TPipe } from '../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../core/idioma/textos';

export type PasoSolicitud = 'solicitar' | 'confirmar' | 'pagar' | 'listo';

const PASOS: readonly { readonly paso: PasoSolicitud; readonly clave: ClaveTexto }[] = [
  { paso: 'solicitar', clave: 'solicitudes.pasos.solicitar' },
  { paso: 'confirmar', clave: 'solicitudes.pasos.confirmar' },
  { paso: 'pagar', clave: 'solicitudes.pasos.pagar' },
  { paso: 'listo', clave: 'solicitudes.pasos.listo' },
];

/**
 * Los cuatro pasos de una solicitud (solicitar → confirmar → pagar → listo), con el actual marcado y los anteriores como
 * hechos. El estado no depende solo del color: el paso actual y los completados llevan también texto para lectores de
 * pantalla (`aria-current="step"`).
 */
@Component({
  selector: 'app-pasos-solicitud',
  imports: [TPipe],
  templateUrl: './pasos-solicitud.html',
  styleUrl: './pasos-solicitud.css',
})
export class PasosSolicitud {
  readonly paso = input.required<PasoSolicitud>();

  protected readonly pasos = computed(() => {
    const actual = PASOS.findIndex((p) => p.paso === this.paso());
    return PASOS.map((p, indice) => ({
      ...p,
      numero: indice + 1,
      estado: indice < actual ? ('hecho' as const) : indice === actual ? ('actual' as const) : null,
    }));
  });
}
