import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { CatalogoService } from '../../../core/api/catalogo.service';
import { FichaOferta } from '../../../core/api/modelos';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { DuracionPipe } from '../../../shared/duracion.pipe';
import { MODALIDAD, NIVEL_ESFUERZO, TIPO_ACTIVIDAD, TIPO_OFERTA } from '../../../shared/etiquetas';
import { ICONO_DE_TIPO, Icono } from '../../../shared/icono/icono';
import { Mapa } from '../../../shared/mapa/mapa';
import { OpinionesDeLaOferta } from './opiniones/opiniones-oferta';
import { PrecioPipe } from '../../../shared/precio.pipe';

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; ficha: FichaOferta }
  | { tipo: 'no-disponible' }
  | { tipo: 'error'; mensaje: string };

/**
 * Ficha pública de una oferta, con su zona, el perfil del prestador, el mapa (RF-20, RF-24, RF-25) y sus opiniones
 * (RF-59), que se piden a su propia ruta porque el catálogo no depende del módulo de opiniones (DC-40).
 */
@Component({
  selector: 'app-ficha',
  imports: [RouterLink, PrecioPipe, DuracionPipe, Mapa, Icono, OpinionesDeLaOferta],
  templateUrl: './ficha.html',
  styleUrl: './ficha.css',
})
export class Ficha {
  /** Llega de la ruta /ofertas/:id. */
  readonly id = input.required<string>();

  private readonly catalogo = inject(CatalogoService);

  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly etiquetaActividad = TIPO_ACTIVIDAD;
  protected readonly etiquetaNivel = NIVEL_ESFUERZO;
  protected readonly etiquetaModalidad = MODALIDAD;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;

  protected readonly estado = signal<Estado>({ tipo: 'cargando' });

  protected readonly ficha = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'lista' ? estado.ficha : null;
  });

  /** El esfuerzo físico de la actividad o el calculado del paquete; los demás tipos no lo tienen. */
  protected readonly nivelEsfuerzo = computed(() => {
    const ficha = this.ficha();
    return ficha?.actividad?.nivelEsfuerzo ?? ficha?.paquete?.nivelEsfuerzo ?? null;
  });

  protected readonly mensajeDeError = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'error' ? estado.mensaje : '';
  });

  /** Enlace de Google Maps con la ruta hasta la oferta; solo existe si la oferta tiene ubicación. */
  protected readonly comoLlegar = computed(() => {
    const ubicacion = this.ficha()?.ubicacion;
    return ubicacion
      ? `https://www.google.com/maps/dir/?api=1&destination=${ubicacion.latitud},${ubicacion.longitud}`
      : null;
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        tap(() => this.estado.set({ tipo: 'cargando' })),
        switchMap((id) =>
          this.catalogo.ficha(id).pipe(
            map((ficha): Estado => ({ tipo: 'lista', ficha })),
            catchError((fallo: unknown) => {
              const error = aErrorApi(fallo);
              return of<Estado>(
                error.noEncontrado ? { tipo: 'no-disponible' } : { tipo: 'error', mensaje: error.mensaje },
              );
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((estado) => this.estado.set(estado));
  }

  /** El dominio de un enlace, para mostrarlo sin la URL completa. */
  protected dominio(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
  }
}
