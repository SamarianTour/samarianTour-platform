import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { CatalogoService } from '../../../core/api/catalogo.service';
import { Zona } from '../../../core/api/modelos';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { Icono } from '../../../shared/icono/icono';
import { AlertasZona } from '../../recomendacion/componentes/alertas-zona/alertas-zona';

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; zona: Zona }
  | { tipo: 'no-existe' }
  | { tipo: 'error'; error: ErrorApi };

/**
 * Ficha pública de una zona (RF-25, HU-TUR-16): descripción, marcas (área protegida, alta afluencia), buenas prácticas,
 * condiciones de visita y alertas del destino. También responde por una zona inactiva (DC-49): sus ofertas siguen visibles.
 */
@Component({
  selector: 'app-ficha-zona',
  imports: [RouterLink, Icono, TPipe, AlertasZona],
  templateUrl: './ficha-zona.html',
  styleUrl: './ficha-zona.css',
})
export class FichaZona {
  /** Llega de la ruta /zonas/:id. */
  readonly id = input.required<string>();

  private readonly catalogo = inject(CatalogoService);

  protected readonly estado = signal<Estado>({ tipo: 'cargando' });

  protected readonly zona = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'lista' ? estado.zona : null;
  });

  protected readonly mensajeDeError = computed(() => {
    const estado = this.estado();
    // Se guarda el error y no su texto: así el mensaje cambia de idioma con la interfaz.
    return estado.tipo === 'error' ? estado.error.mensaje : '';
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        tap(() => this.estado.set({ tipo: 'cargando' })),
        switchMap((id) =>
          this.catalogo.zona(id).pipe(
            map((zona): Estado => ({ tipo: 'lista', zona })),
            catchError((fallo: unknown) => {
              const error = aErrorApi(fallo);
              return of<Estado>(error.noEncontrado ? { tipo: 'no-existe' } : { tipo: 'error', error });
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((estado) => this.estado.set(estado));
  }
}
