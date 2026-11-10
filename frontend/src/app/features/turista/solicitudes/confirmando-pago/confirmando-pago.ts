import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, inject, output } from '@angular/core';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { Icono } from '../../../../shared/icono/icono';
import { esperaDeReconsulta } from '../pagos';

/**
 * "Confirmando tu pago" (RF-50, RF-51). Se muestra mientras hay un intento de pago en curso: el turista pudo haber vuelto de la
 * pasarela, pero la redirección del navegador no prueba nada, y el estado del pago lo decide solo el servidor con el evento de
 * la pasarela. Por eso este bloque no afirma que se pagó: pide a la pantalla que vuelva a consultar, cada 3 segundos los primeros
 * 2 minutos y luego cada 15, hasta que el estado cambie (entonces la pantalla lo quita). Con la pestaña oculta no consulta; al
 * volver a verla la pantalla ya consulta por su cuenta (ADR-04).
 */
@Component({
  selector: 'app-confirmando-pago',
  imports: [TPipe, Icono],
  templateUrl: './confirmando-pago.html',
  styleUrl: './confirmando-pago.css',
})
export class ConfirmandoPago {
  /** Es hora de preguntar de nuevo el estado real de la solicitud y de sus intentos. */
  readonly consultar = output<void>();

  private readonly documento = inject(DOCUMENT);

  constructor() {
    const inicio = Date.now();
    let temporizador: ReturnType<typeof setTimeout> | undefined;

    const programar = (): void => {
      temporizador = setTimeout(() => {
        if (this.documento.visibilityState === 'visible') {
          this.consultar.emit();
        }
        programar();
      }, esperaDeReconsulta(Date.now() - inicio));
    };
    programar();

    inject(DestroyRef).onDestroy(() => clearTimeout(temporizador));
  }
}
