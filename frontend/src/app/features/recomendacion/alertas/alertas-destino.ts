import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Resultado } from '../../../core/api/degradacion';
import { ResumenAlertas } from '../../../core/api/modelos-recomendacion';
import { RecomendacionService } from '../../../core/api/recomendacion.service';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { TarjetaAlerta } from '../componentes/tarjeta-alerta/tarjeta-alerta';
import { totalDeAlertas, zonasPorGravedad } from './alertas';

type Vista = 'cargando' | 'alertas' | 'sinAlertas' | 'noDisponible' | 'error';

const CLAVE_DE_NO_DISPONIBLE = {
  tiempo: 'recomendacion.alertas.noDisponible.tiempo',
  conexion: 'recomendacion.alertas.noDisponible.conexion',
  servicio: 'recomendacion.alertas.noDisponible.servicio',
} as const satisfies Record<string, ClaveTexto>;

/**
 * P08.2 Alertas del destino (HU-TUR-05, RF-28). Las condiciones activas de cada zona —las del IDEAM y, en la demo, las simuladas—
 * con su fuente y la hora de la última actualización. Es pública y no está en el menú: se llega desde Recomendaciones y desde la
 * sección de alertas de la ficha de una oferta.
 *
 * La fuente de alertas es opcional (ADR-04): si no responde, tarda más de 8 s o el servicio no está (503), la página lo avisa y
 * deja volver a intentar; nunca queda en blanco ni rota.
 */
@Component({
  selector: 'app-alertas-destino',
  imports: [RouterLink, Icono, TPipe, FechaPipe, TarjetaAlerta],
  templateUrl: './alertas-destino.html',
  styleUrl: './alertas-destino.css',
})
export class AlertasDestino {
  private readonly servicio = inject(RecomendacionService);
  private readonly destruccion = inject(DestroyRef);

  /** `null` mientras se consulta. */
  private readonly resultado = signal<Resultado<ResumenAlertas> | null>(null);

  protected readonly resumen = computed(() => {
    const resultado = this.resultado();
    return resultado?.estado === 'ok' ? resultado.datos : null;
  });

  protected readonly vista = computed<Vista>(() => {
    const resultado = this.resultado();
    if (!resultado) {
      return 'cargando';
    }
    switch (resultado.estado) {
      case 'ok':
        return resultado.datos.zonas.length > 0 ? 'alertas' : 'sinAlertas';
      case 'no-disponible':
        return 'noDisponible';
      case 'error':
        return 'error';
    }
  });

  protected readonly zonas = computed(() => {
    const resumen = this.resumen();
    return resumen ? zonasPorGravedad(resumen) : [];
  });
  protected readonly total = computed(() => {
    const resumen = this.resumen();
    return resumen ? totalDeAlertas(resumen) : 0;
  });

  protected readonly claveDeNoDisponible = computed<ClaveTexto>(() => {
    const resultado = this.resultado();
    return CLAVE_DE_NO_DISPONIBLE[resultado?.estado === 'no-disponible' ? resultado.motivo : 'servicio'];
  });
  protected readonly mensajeDeError = computed(() => {
    const resultado = this.resultado();
    return resultado?.estado === 'error' ? resultado.error.mensaje : '';
  });

  constructor() {
    this.cargar();
  }

  /** Consulta las alertas; también es lo que hace "Intentar de nuevo". */
  protected cargar(): void {
    this.resultado.set(null);
    this.servicio
      .alertas()
      .pipe(takeUntilDestroyed(this.destruccion))
      .subscribe((resultado) => this.resultado.set(resultado));
  }
}
