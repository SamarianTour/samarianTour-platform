import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { PaginaAvisos } from '../../../core/api/modelos-nucleo';
import { OperadorService } from '../../../core/api/operador.service';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { SesionService } from '../../../core/sesion/sesion.service';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { claveDeTipoDeAviso, resumenDeAviso } from '../avisos';
import { codigoCorto } from '../cola';

const TAMANO_PAGINA = 10;

type Resultado = { pagina: PaginaAvisos } | { error: ErrorApi };

/**
 * Avisos del operador (RF-34): cada solicitud nueva que llega a su cola deja uno, del más reciente al más antiguo. Cada
 * aviso lleva a esa solicitud, que muestra su estado real (el aviso es solo el momento en que llegó). Vuelve a consultar al
 * regresar a la pestaña y al reconectarse, para ver los avisos nuevos.
 */
@Component({
  selector: 'app-avisos-operador',
  imports: [RouterLink, Icono, TPipe, FechaPipe, Paginacion],
  templateUrl: './avisos-operador.html',
  styleUrl: './avisos-operador.css',
})
export class AvisosOperador {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly servicio = inject(OperadorService);
  private readonly sesion = inject(SesionService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir los avisos. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly cargando = signal(true);
  protected readonly pagina = signal<PaginaAvisos | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);

  protected readonly resumen = resumenDeAviso;
  protected readonly claveDeTipo = claveDeTipoDeAviso;
  protected readonly codigo = codigoCorto;

  constructor() {
    const numeroDePagina = this.ruta.queryParamMap.pipe(
      map((params) => aPagina(params.get('page'))),
    );

    combineLatest([numeroDePagina, toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(([pagina]) =>
          this.servicio.avisos(pagina, TAMANO_PAGINA).pipe(
            map((resultado): Resultado => ({ pagina: resultado })),
            catchError((fallo: unknown) => of<Resultado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('pagina' in resultado) {
          this.pagina.set(resultado.pagina);
        }
        this.error.set('error' in resultado ? resultado.error : null);
      });

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: pagina > 0 ? { page: pagina } : {},
    });
  }
}

function aPagina(valor: string | null): number {
  const pagina = Number(valor);
  return Number.isInteger(pagina) && pagina > 0 ? pagina : 0;
}
