import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { PaginaColaOperador } from '../../../core/api/modelos-nucleo';
import { OperadorService } from '../../../core/api/operador.service';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { SesionService } from '../../../core/sesion/sesion.service';
import { ahoraConReloj } from '../../../core/tiempo/reloj';
import { Icono } from '../../../shared/icono/icono';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { ordenarPorUrgencia } from '../cola';
import { TablaSolicitudesOperador } from '../tabla-solicitudes/tabla-solicitudes';

const TAMANO_PAGINA = 10;

type Resultado = { pagina: PaginaColaOperador } | { error: ErrorApi };

/**
 * P02.4 Cola (RF-35, RF-36): las solicitudes pendientes del operador de la sesión, las urgentes (6 h o menos para
 * atenderlas) primero y resaltadas. La API ya las entrega de la más antigua a la más reciente; aquí se reordenan por
 * urgencia dentro de la página y se vuelve a ordenar cuando el reloj cambia una de nivel. Vuelve a consultar al regresar
 * a la pestaña y al reconectarse, para ver las solicitudes nuevas.
 */
@Component({
  selector: 'app-cola-operador',
  imports: [Icono, TPipe, Paginacion, TablaSolicitudesOperador],
  templateUrl: './cola-operador.html',
  styleUrl: './cola-operador.css',
})
export class ColaOperador {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly servicio = inject(OperadorService);
  private readonly sesion = inject(SesionService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la cola. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly ahora = ahoraConReloj();
  protected readonly cargando = signal(true);
  protected readonly pagina = signal<PaginaColaOperador | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);

  protected readonly solicitudes = computed(() =>
    ordenarPorUrgencia(this.pagina()?.contenido ?? [], this.ahora()),
  );

  constructor() {
    const numeroDePagina = this.ruta.queryParamMap.pipe(
      map((params) => aPagina(params.get('page'))),
    );

    combineLatest([numeroDePagina, toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(([pagina]) =>
          this.servicio.cola(undefined, pagina, TAMANO_PAGINA).pipe(
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
