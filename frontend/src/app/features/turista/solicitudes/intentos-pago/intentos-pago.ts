import { Component, input } from '@angular/core';
import { IntentoPago } from '../../../../core/api/modelos-nucleo';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { FechaPipe } from '../../../../shared/fecha.pipe';
import { PrecioPipe } from '../../../../shared/precio.pipe';
import { CLASE_DE_INTENTO, ETIQUETA_INTENTO, MOTIVO_INTENTO, centavosAPesos } from '../pagos';

/**
 * P04.2 Intentos de pago: cada intento de la solicitud, del primero al último, con su estado y por qué terminó así (rechazado,
 * con error, abandonado). El motivo es una explicación fija de cada estado: el contrato no entrega el texto de la pasarela.
 * Solo muestra lo que informa el servidor; no decide nada del pago (RF-50, RF-51).
 */
@Component({
  selector: 'app-intentos-pago',
  imports: [TPipe, FechaPipe, PrecioPipe],
  templateUrl: './intentos-pago.html',
  styleUrl: './intentos-pago.css',
})
export class IntentosPago {
  readonly intentos = input.required<readonly IntentoPago[]>();

  protected readonly etiqueta = ETIQUETA_INTENTO;
  protected readonly motivo = MOTIVO_INTENTO;
  protected readonly clase = CLASE_DE_INTENTO;
  protected readonly pesos = centavosAPesos;
}
