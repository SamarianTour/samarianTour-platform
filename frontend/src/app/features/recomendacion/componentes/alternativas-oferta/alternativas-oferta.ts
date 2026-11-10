import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { switchMap, tap } from 'rxjs';
import { Alternativas } from '../../../../core/api/modelos-recomendacion';
import { RecomendacionService } from '../../../../core/api/recomendacion.service';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { Icono } from '../../../../shared/icono/icono';
import { TarjetaRecomendada } from '../tarjeta-recomendada/tarjeta-recomendada';

/**
 * Lo que se muestra de la respuesta, o `null` si la sección no debe verse: el servicio no está, tardó más de 3 s, falló, la oferta
 * no existe, la zona no es de alta afluencia (`aplica: false`) o no hay ninguna alternativa que mostrar.
 */
export function alternativasVisibles(respuesta: Alternativas): Alternativas | null {
  return respuesta.aplica && respuesta.alternativas.length > 0 ? respuesta : null;
}

/**
 * P08.3 Alternativas (HU-TUR-06, RF-29): en la ficha de una oferta de una zona con alta afluencia, de 2 a 3 ofertas parecidas
 * —mismo tipo, esfuerzo igual o menor, precio parecido— en zonas con menos visitantes, cada una con la explicación de por qué se
 * propone. Son siempre sugerencias por regla, sin IA y sin patrocinio. La ficha solo la crea cuando la zona es de alta afluencia.
 *
 * La recomendación es opcional (ADR-04) y la ficha no debe esperarla: si falla, tarda más de 3 s o no hay alternativas, la sección se
 * oculta sin avisos y el resto de la ficha sigue igual. Mientras consulta no ocupa lugar, para que la página no se mueva al llegar.
 */
@Component({
  selector: 'app-alternativas-oferta',
  imports: [Icono, TPipe, TarjetaRecomendada],
  templateUrl: './alternativas-oferta.html',
  styleUrl: './alternativas-oferta.css',
})
export class AlternativasOferta {
  /** La oferta que se está viendo. */
  readonly ofertaId = input.required<string>();

  private readonly servicio = inject(RecomendacionService);
  private readonly lectura = signal<Alternativas | null>(null);

  protected readonly alternativas = computed(() => this.lectura());

  constructor() {
    toObservable(this.ofertaId)
      .pipe(
        // Al cambiar de oferta no se queda la lectura de la anterior.
        tap(() => this.lectura.set(null)),
        switchMap((ofertaId) => this.servicio.alternativas(ofertaId)),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) =>
        this.lectura.set(resultado.estado === 'ok' ? alternativasVisibles(resultado.datos) : null),
      );
  }
}
