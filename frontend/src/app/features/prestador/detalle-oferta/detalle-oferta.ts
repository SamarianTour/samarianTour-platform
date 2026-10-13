import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { OfertaPrestador } from '../../../core/api/modelos';
import { OfertasPrestadorService } from '../../../core/api/ofertas-prestador.service';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { ESTADO_MODERACION, TIPO_OFERTA, UNIDAD_PRECIO } from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';
import { PrecioPipe } from '../../../shared/precio.pipe';
import { CLASE_DE_ESTADO, ESTADO_OFERTA, estadoDe, sePuedeEditar } from '../estado-oferta';

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; oferta: OfertaPrestador }
  | { tipo: 'no-encontrada' }
  | { tipo: 'error'; mensaje: string };

/** Detalle de una oferta propia: su versión publicada y su versión pendiente, lado a lado (RF-66). */
@Component({
  selector: 'app-detalle-oferta',
  imports: [RouterLink, DatePipe, PrecioPipe, Icono],
  templateUrl: './detalle-oferta.html',
  styleUrl: './detalle-oferta.css',
})
export class DetalleOferta {
  /** Llega de la ruta /prestador/ofertas/:id. */
  readonly id = input.required<string>();

  private readonly servicio = inject(OfertasPrestadorService);

  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly etiquetaEstado = ESTADO_OFERTA;
  protected readonly claseDeEstado = CLASE_DE_ESTADO;
  protected readonly etiquetaModeracion = ESTADO_MODERACION;
  protected readonly unidad = UNIDAD_PRECIO;

  protected readonly estado = signal<Estado>({ tipo: 'cargando' });

  protected readonly oferta = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'lista' ? estado.oferta : null;
  });

  protected readonly mensajeDeError = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'error' ? estado.mensaje : '';
  });

  /** Las dos versiones, en el orden en que se muestran. */
  protected readonly versiones = computed(() => {
    const oferta = this.oferta();
    return oferta
      ? [
          { titulo: 'Versión publicada', vacio: 'Todavía no tiene una versión publicada.', version: oferta.publicada },
          { titulo: 'Versión pendiente', vacio: 'No tiene cambios en revisión.', version: oferta.pendiente },
        ]
      : [];
  });

  protected readonly estadoCalculado = computed(() => {
    const oferta = this.oferta();
    return oferta ? estadoDe(oferta) : null;
  });

  protected readonly editable = computed(() => {
    const oferta = this.oferta();
    return !!oferta && sePuedeEditar(oferta);
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        tap(() => this.estado.set({ tipo: 'cargando' })),
        switchMap((id) =>
          this.servicio.buscarPropia(id).pipe(
            map((oferta): Estado => (oferta ? { tipo: 'lista', oferta } : { tipo: 'no-encontrada' })),
            catchError((error: unknown) => of<Estado>({ tipo: 'error', mensaje: aErrorApi(error).mensaje })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((estado) => this.estado.set(estado));
  }
}
