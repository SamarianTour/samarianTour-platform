import { Component, input } from '@angular/core';
import { Alerta } from '../../../../core/api/modelos-recomendacion';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { FechaPipe } from '../../../../shared/fecha.pipe';
import { Icono } from '../../../../shared/icono/icono';
import { CLAVE_DE_NIVEL, CLAVE_DE_TIPO } from '../../alertas/alertas';

/**
 * Una alerta del destino (RF-28): su nivel (con color, icono y nombre: el color solo no basta), su tipo, el título y la descripción
 * en el idioma pedido, y quién la emite. La fuente se muestra tal cual la da el servicio: si termina en "(simulado)", se ve así;
 * un dato simulado nunca se presenta como oficial (DR-17). La usan la página de alertas (P08.2) y la sección de la ficha.
 */
@Component({
  selector: 'app-tarjeta-alerta',
  imports: [Icono, TPipe, FechaPipe],
  templateUrl: './tarjeta-alerta.html',
  styleUrl: './tarjeta-alerta.css',
})
export class TarjetaAlerta {
  readonly alerta = input.required<Alerta>();

  protected readonly claveDeNivel = CLAVE_DE_NIVEL;
  protected readonly claveDeTipo = CLAVE_DE_TIPO;
}
