import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap, tap } from 'rxjs';
import { AlertasZona as AlertasDeLaZona } from '../../../../core/api/modelos-recomendacion';
import { RecomendacionService } from '../../../../core/api/recomendacion.service';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { FechaPipe } from '../../../../shared/fecha.pipe';
import { Icono } from '../../../../shared/icono/icono';
import { TarjetaAlerta } from '../tarjeta-alerta/tarjeta-alerta';

/** `null` mientras consulta y cuando la sección se oculta por no estar disponible. */
type Lectura = { readonly datos: AlertasDeLaZona } | null;

/**
 * Las alertas activas de la zona de una oferta, para su ficha (P08.2, RF-28), con la fuente y la hora de actualización de cada una.
 * Sin alertas dice "Sin alertas activas" con la hora, para que se sepa que se consultó. Lleva un enlace a la página de alertas de
 * todo el destino.
 *
 * La fuente de alertas es opcional (ADR-04) y la ficha no debe esperarla: si falla, tarda más de 3 s o el servicio no está, la sección
 * se oculta sin avisos ni errores y el resto de la ficha sigue igual. Mientras consulta tampoco ocupa lugar, para que la página no se
 * mueva al llegar.
 */
@Component({
  selector: 'app-alertas-zona',
  imports: [RouterLink, Icono, TPipe, FechaPipe, TarjetaAlerta],
  templateUrl: './alertas-zona.html',
  styleUrl: './alertas-zona.css',
})
export class AlertasZona {
  /** La zona de la oferta que se está viendo. */
  readonly zonaId = input.required<string>();

  private readonly servicio = inject(RecomendacionService);
  private readonly lectura = signal<Lectura>(null);

  protected readonly datos = computed(() => this.lectura()?.datos ?? null);

  constructor() {
    toObservable(this.zonaId)
      .pipe(
        // Al cambiar de zona no se queda la lectura de la anterior.
        tap(() => this.lectura.set(null)),
        switchMap((zonaId) => this.servicio.alertasDeZona(zonaId)),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => this.lectura.set(resultado.estado === 'ok' ? { datos: resultado.datos } : null));
  }
}
