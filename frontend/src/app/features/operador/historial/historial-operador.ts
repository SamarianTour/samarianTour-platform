import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { EstadoSolicitud, PaginaColaOperador } from '../../../core/api/modelos-nucleo';
import { OperadorService } from '../../../core/api/operador.service';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { SesionService } from '../../../core/sesion/sesion.service';
import { Icono } from '../../../shared/icono/icono';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { ETIQUETA_ESTADO } from '../../turista/solicitudes/estado-solicitud';
import { ESTADOS_HISTORIAL, estadoDelHistorial } from '../historial';
import { TablaSolicitudesOperador } from '../tabla-solicitudes/tabla-solicitudes';

const TAMANO_PAGINA = 10;

type Resultado = { pagina: PaginaColaOperador } | { error: ErrorApi };

/**
 * Historial del operador (RF-35): la misma consulta de la cola con el parámetro `estado`, de la solicitud más reciente a la
 * más antigua. El estado y la página viajan en la dirección (`?estado=PAGADA&page=1`), así que se puede compartir, recargar
 * y volver atrás. Sin estado conocido en la dirección se abre en las atendidas.
 */
@Component({
  selector: 'app-historial-operador',
  imports: [RouterLink, Icono, TPipe, Paginacion, TablaSolicitudesOperador],
  templateUrl: './historial-operador.html',
  styleUrl: './historial-operador.css',
})
export class HistorialOperador {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly servicio = inject(OperadorService);
  private readonly sesion = inject(SesionService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir el historial. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly estados = ESTADOS_HISTORIAL;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;

  protected readonly estado = signal<EstadoSolicitud>(estadoDelHistorial(null));
  protected readonly cargando = signal(true);
  protected readonly pagina = signal<PaginaColaOperador | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);

  constructor() {
    // Un solo flujo para el estado y la página: así un cambio de dirección hace una sola consulta.
    const consulta = this.ruta.queryParamMap.pipe(
      map((params) => ({
        estado: estadoDelHistorial(params.get('estado')),
        pagina: aPagina(params.get('page')),
      })),
    );

    combineLatest([consulta, toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(([{ estado }]) => {
          this.estado.set(estado);
          this.cargando.set(true);
        }),
        switchMap(([{ estado, pagina }]) =>
          this.servicio.cola(estado, pagina, TAMANO_PAGINA).pipe(
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
      queryParams: { estado: this.estado(), ...(pagina > 0 ? { page: pagina } : {}) },
    });
  }
}

function aPagina(valor: string | null): number {
  const pagina = Number(valor);
  return Number.isInteger(pagina) && pagina > 0 ? pagina : 0;
}
