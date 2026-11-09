import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { OfertaPrestador } from '../../../core/api/modelos';
import { OfertasPrestadorService } from '../../../core/api/ofertas-prestador.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { traducir } from '../../../core/idioma/traducir';
import { ESTADO_MODERACION, TIPO_OFERTA, UNIDAD_PRECIO } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { PrecioPipe } from '../../../shared/precio.pipe';
import { CLASE_DE_ESTADO, ESTADO_OFERTA, estadoDe, sePuedeEditar } from '../estado-oferta';

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; oferta: OfertaPrestador }
  | { tipo: 'no-encontrada' }
  | { tipo: 'error'; error: ErrorApi };

/** Detalle de una oferta propia: su versión publicada y su versión pendiente, lado a lado (RF-66). */
@Component({
  selector: 'app-detalle-oferta',
  imports: [RouterLink, FechaPipe, PrecioPipe, Icono, TPipe],
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
    // Se guarda el error y no su texto: así el mensaje cambia de idioma con la interfaz.
    return estado.tipo === 'error' ? estado.error.mensaje : '';
  });

  /** Las dos versiones, en el orden en que se muestran. */
  protected readonly versiones = computed(() => {
    const oferta = this.oferta();
    return oferta
      ? [
          {
            clave: 'publicada',
            titulo: traducir('prestador.detalle.versionPublicada'),
            vacio: traducir('prestador.detalle.vacioPublicada'),
            version: oferta.publicada,
          },
          {
            clave: 'pendiente',
            titulo: traducir('prestador.detalle.versionPendiente'),
            vacio: traducir('prestador.detalle.vacioPendiente'),
            version: oferta.pendiente,
          },
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
            catchError((error: unknown) => of<Estado>({ tipo: 'error', error: aErrorApi(error) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((estado) => this.estado.set(estado));
  }
}
