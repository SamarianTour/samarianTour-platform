import { Component, ElementRef, afterNextRender, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IntentoPago, SolicitudTurista } from '../../../../core/api/modelos-nucleo';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { FechaPipe } from '../../../../shared/fecha.pipe';
import { Icono } from '../../../../shared/icono/icono';
import { PrecioPipe } from '../../../../shared/precio.pipe';
import { intentoAprobado } from '../pagos';

/**
 * "Pago confirmado" (P04.1). Solo se muestra con la solicitud ya `PAGADA`, es decir, cuando el servidor recibió el evento de la
 * pasarela (RF-50): nunca por haber vuelto del checkout. Muestra lo pagado y la referencia de la transacción (RF-52). Si el
 * turista estaba esperando la confirmación, lleva el foco al título para que el cambio se anuncie.
 */
@Component({
  selector: 'app-pago-confirmado',
  imports: [RouterLink, TPipe, FechaPipe, PrecioPipe, Icono],
  templateUrl: './pago-confirmado.html',
  styleUrl: './pago-confirmado.css',
})
export class PagoConfirmado {
  readonly solicitud = input.required<SolicitudTurista>();
  /** Los intentos de pago de la solicitud; de ahí sale la referencia de la transacción aprobada. */
  readonly intentos = input<readonly IntentoPago[]>([]);
  /** El turista estaba esperando la confirmación: el foco va al título para anunciar el cambio. */
  readonly enfocar = input(false);

  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly aprobado = computed(() => intentoAprobado(this.intentos()));
  protected readonly total = computed(() => this.solicitud().condiciones?.precioBase ?? null);

  constructor() {
    afterNextRender(() => {
      if (this.enfocar()) {
        this.anfitrion.nativeElement.querySelector<HTMLElement>('h2')?.focus();
      }
    });
  }
}
