import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap } from 'rxjs';
import { EstadoOpinion, PaginaMisOpiniones } from '../../../core/api/modelos';
import { OpinionesService } from '../../../core/api/opiniones.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { SesionService } from '../../../core/sesion/sesion.service';
import { ESTADO_OPINION } from '../../../shared/etiquetas';
import { Estrellas } from '../../../shared/estrellas/estrellas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { Paginacion } from '../../../shared/paginacion/paginacion';

const TAMANO_PAGINA = 10;

/** Color de la etiqueta de cada estado: una clase de las etiquetas globales. */
const CLASE_DE_ESTADO: Record<EstadoOpinion, string> = {
  EN_MODERACION: 'etiqueta--aviso',
  APROBADA: 'etiqueta--exito',
  RECHAZADA: 'etiqueta--peligro',
};

type Resultado = { pagina: PaginaMisOpiniones } | { error: ErrorApi };

/**
 * "Mis opiniones": las del turista de la sesión en cualquier estado, con el motivo si fue rechazada (DC-42). La ruta
 * solo la abre un turista. Se recarga si entra otra cuenta.
 */
@Component({
  selector: 'app-mis-opiniones',
  imports: [RouterLink, FechaPipe, Estrellas, Icono, Paginacion, TPipe],
  templateUrl: './mis-opiniones.html',
  styleUrl: './mis-opiniones.css',
})
export class MisOpiniones {
  private readonly servicio = inject(OpinionesService);
  private readonly sesion = inject(SesionService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la lista. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);

  protected readonly etiquetaEstado = ESTADO_OPINION;
  protected readonly claseDeEstado = CLASE_DE_ESTADO;

  private readonly pagina = new BehaviorSubject(0);
  protected readonly cargando = signal(true);
  private readonly resultado = signal<Resultado | null>(null);
  protected readonly opiniones = computed(() => {
    const resultado = this.resultado();
    return resultado && 'pagina' in resultado ? resultado.pagina : null;
  });
  protected readonly error = computed(() => {
    const resultado = this.resultado();
    return resultado && 'error' in resultado ? resultado.error.mensaje : null;
  });

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.pagina])
      .pipe(
        switchMap(([, pagina]) => {
          this.cargando.set(true);
          return this.servicio.mias(pagina, TAMANO_PAGINA).pipe(
            map((respuesta): Resultado => ({ pagina: respuesta })),
            catchError((fallo: unknown) => of<Resultado>({ error: aErrorApi(fallo) })),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        this.resultado.set(resultado);
      });
  }

  protected irAPagina(pagina: number): void {
    this.pagina.next(pagina);
  }
}
