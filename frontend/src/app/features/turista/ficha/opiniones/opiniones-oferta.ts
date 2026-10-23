import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap } from 'rxjs';
import { OpinionesOferta } from '../../../../core/api/modelos';
import { OpinionesService } from '../../../../core/api/opiniones.service';
import { AvisosService } from '../../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../../core/errores/error-api';
import { aErrorApi } from '../../../../core/errores/errores.interceptor';
import { IdentidadService } from '../../../../core/identidad/identidad.service';
import { CALIFICACION } from '../../../../shared/etiquetas';
import { Estrellas } from '../../../../shared/estrellas/estrellas';
import { Icono } from '../../../../shared/icono/icono';
import { Paginacion } from '../../../../shared/paginacion/paginacion';

const TAMANO_PAGINA = 5;
const COMENTARIO_MINIMO = 10;
const COMENTARIO_MAXIMO = 1000;

type Lectura = { opiniones: OpinionesOferta } | { error: ErrorApi };

/** El comentario cuenta sin los espacios de los extremos, como en el backend (DC-41). */
const largoDelComentario: ValidatorFn = (control) => {
  const largo = String(control.value ?? '').trim().length;
  if (largo === 0) {
    return { required: true };
  }
  return largo < COMENTARIO_MINIMO || largo > COMENTARIO_MAXIMO ? { largo: true } : null;
};

/**
 * Opiniones de una oferta en su ficha (RF-59): el promedio, el total y las aprobadas de la más reciente a la más
 * antigua. Con un turista demo elegido, deja opinar (RF-60): la opinión queda en moderación y la decide el
 * administrador.
 */
@Component({
  selector: 'app-opiniones-oferta',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, Estrellas, Icono, Paginacion],
  templateUrl: './opiniones-oferta.html',
  styleUrl: './opiniones-oferta.css',
})
export class OpinionesDeLaOferta {
  readonly ofertaId = input.required<string>();

  private readonly servicio = inject(OpinionesService);
  private readonly avisos = inject(AvisosService);
  protected readonly turista = inject(IdentidadService).turista;

  protected readonly calificaciones = [1, 2, 3, 4, 5];
  protected readonly textoCalificacion = CALIFICACION;
  protected readonly comentarioMaximo = COMENTARIO_MAXIMO;

  private readonly pagina = new BehaviorSubject(0);
  protected readonly lectura = signal<Lectura | null>(null);
  protected readonly opiniones = computed(() => {
    const lectura = this.lectura();
    return lectura && 'opiniones' in lectura ? lectura.opiniones : null;
  });
  protected readonly errorDeLectura = computed(() => {
    const lectura = this.lectura();
    return lectura && 'error' in lectura ? lectura.error.mensaje : null;
  });

  protected readonly formulario = new FormGroup({
    calificacion: new FormControl<number | null>(null, Validators.required),
    comentario: new FormControl('', { nonNullable: true, validators: largoDelComentario }),
  });
  protected readonly calificacionElegida = signal<number | null>(null);
  protected readonly largoComentario = signal(0);
  protected readonly enviando = signal(false);
  protected readonly enviada = signal(false);
  protected readonly errorDeEnvio = signal<string | null>(null);
  /** Error de un campo que devolvió la API (DATO_INVALIDO), por nombre de campo. */
  private readonly erroresDeApi = signal<Record<string, string>>({});
  private readonly cambios = signal(0);

  constructor() {
    combineLatest([toObservable(this.ofertaId), this.pagina])
      .pipe(
        switchMap(([ofertaId, pagina]) =>
          this.servicio.deOferta(ofertaId, pagina, TAMANO_PAGINA).pipe(
            map((opiniones): Lectura => ({ opiniones })),
            catchError((error: unknown) => of<Lectura>({ error: aErrorApi(error) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((lectura) => this.lectura.set(lectura));

    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe((valores) => {
      this.calificacionElegida.set(valores.calificacion ?? null);
      this.largoComentario.set((valores.comentario ?? '').trim().length);
      this.erroresDeApi.set({});
      this.cambios.update((cambios) => cambios + 1);
    });
  }

  protected irAPagina(pagina: number): void {
    this.pagina.next(pagina);
  }

  /** Mensaje de error de un campo: primero el de la API; si no, el de la validación local una vez tocado. */
  protected errorDe(campo: 'calificacion' | 'comentario'): string | null {
    this.cambios();
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi;
    }
    const control = this.formulario.controls[campo];
    if (!control.invalid || !control.touched) {
      return null;
    }
    if (campo === 'calificacion') {
      return 'Elige de 1 a 5 estrellas.';
    }
    return control.errors?.['required']
      ? 'Escribe tu comentario.'
      : `El comentario debe tener entre ${COMENTARIO_MINIMO} y ${COMENTARIO_MAXIMO} caracteres.`;
  }

  protected enviar(): void {
    if (this.enviando()) {
      return;
    }
    this.errorDeEnvio.set(null);
    this.enviada.set(false);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.cambios.update((cambios) => cambios + 1);
      return;
    }
    const { calificacion, comentario } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.servicio
      .publicar({ ofertaId: this.ofertaId(), calificacion: calificacion!, comentario: comentario.trim() })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.enviada.set(true);
          this.formulario.reset({ calificacion: null, comentario: '' });
          this.avisos.mostrar('Tu opinión quedó en moderación');
        },
        error: (fallo: unknown) => {
          this.enviando.set(false);
          const error = aErrorApi(fallo);
          if (error.campo === 'calificacion' || error.campo === 'comentario') {
            this.erroresDeApi.set({ [error.campo]: error.detalleCampo ?? error.mensaje });
          } else {
            this.errorDeEnvio.set(error.mensaje);
          }
        },
      });
  }
}
