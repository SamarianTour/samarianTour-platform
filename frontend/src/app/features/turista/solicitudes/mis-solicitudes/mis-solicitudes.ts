import { Component, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { ICONO_DE_TIPO, Icono } from '../../../../shared/icono/icono';
import { PaginaSolicitudesTurista, SolicitudResumen } from '../../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../../core/api/reconsulta';
import { SolicitudesService } from '../../../../core/api/solicitudes.service';
import { ErrorApi } from '../../../../core/errores/error-api';
import { aErrorApi } from '../../../../core/errores/errores.interceptor';
import { TPipe } from '../../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../../core/idioma/textos';
import { SesionService } from '../../../../core/sesion/sesion.service';
import { ahoraConReloj } from '../../../../core/tiempo/reloj';
import { TIPO_OFERTA } from '../../../../shared/etiquetas';
import { FechaPipe } from '../../../../shared/fecha.pipe';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import {
  AccionSolicitud,
  CLASE_DE_ESTADO_SOLICITUD,
  DESCRIPCION_ESTADO,
  ETIQUETA_ESTADO,
  FILTROS_SOLICITUDES,
  FiltroSolicitudes,
  accionPrincipal,
  coincideConFiltro,
  estaActiva,
} from '../estado-solicitud';
import { normalizarHora } from '../peticion-solicitud';
import { plazoRestante } from '../plazo';
import { avisoDePlazo } from '../texto-plazo';

const TAMANO_PAGINA = 10;

type Resultado = { pagina: PaginaSolicitudesTurista } | { error: ErrorApi };

/**
 * P02.2 Mis solicitudes (RF-33): las del turista de la sesión, de la más reciente a la más antigua, con el estado de cada
 * una, su plazo y lo que puede hacer a continuación. La API no filtra por estado, así que Activas y Finalizadas se filtran
 * aquí, sobre la página cargada. Vuelve a consultar al regresar a la pestaña, al reconectarse y cuando un plazo termina.
 */
@Component({
  selector: 'app-mis-solicitudes',
  imports: [RouterLink, Icono, TPipe, FechaPipe, Paginacion],
  templateUrl: './mis-solicitudes.html',
  styleUrl: './mis-solicitudes.css',
})
export class MisSolicitudes {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly servicio = inject(SolicitudesService);
  private readonly sesion = inject(SesionService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la lista. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  /** Las solicitudes cuyo plazo ya terminó y que ya se volvieron a consultar: se hace una sola vez cada una. */
  private readonly reconsultadas = new Set<string>();
  private readonly ahora = ahoraConReloj();

  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly descripcionEstado = DESCRIPCION_ESTADO;
  protected readonly claseDeEstado = CLASE_DE_ESTADO_SOLICITUD;
  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;
  protected readonly filtros = FILTROS_SOLICITUDES;
  protected readonly claveDeFiltro: Readonly<Record<FiltroSolicitudes, ClaveTexto>> = {
    todas: 'solicitudes.lista.filtro.todas',
    activas: 'solicitudes.lista.filtro.activas',
    finalizadas: 'solicitudes.lista.filtro.finalizadas',
  };

  protected readonly filtro = signal<FiltroSolicitudes>('todas');
  protected readonly cargando = signal(true);
  protected readonly pagina = signal<PaginaSolicitudesTurista | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);

  protected readonly visibles = computed(() =>
    (this.pagina()?.contenido ?? []).filter((solicitud) =>
      coincideConFiltro(solicitud.estado, this.filtro()),
    ),
  );

  constructor() {
    const numeroDePagina = this.ruta.queryParamMap.pipe(
      map((params) => aPagina(params.get('page'))),
    );

    // Se vuelve a pedir la lista al cambiar de página o de cuenta, y cada vez que hay que reconsultar (RF-33).
    combineLatest([numeroDePagina, toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(([pagina]) =>
          this.servicio.listar(pagina, TAMANO_PAGINA).pipe(
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

    // Cuando el plazo de una solicitud termina, el núcleo la cierra: se consulta una vez para ver su estado real.
    effect(() => {
      const ahora = this.ahora();
      const vencidas = (this.pagina()?.contenido ?? []).filter(
        (solicitud) =>
          estaActiva(solicitud.estado) &&
          !!solicitud.venceEn &&
          Date.parse(solicitud.venceEn) <= ahora &&
          !this.reconsultadas.has(solicitud.id),
      );
      if (vencidas.length > 0) {
        vencidas.forEach((solicitud) => this.reconsultadas.add(solicitud.id));
        this.recarga.next();
      }
    });
  }

  protected elegirFiltro(filtro: FiltroSolicitudes): void {
    this.filtro.set(filtro);
  }

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: pagina > 0 ? { page: pagina } : {},
    });
  }

  protected accion(solicitud: SolicitudResumen): AccionSolicitud {
    return accionPrincipal(solicitud.estado);
  }

  /** El aviso de cuánto queda para actuar, o null si la solicitud ya cerró. */
  protected plazo(solicitud: SolicitudResumen): string | null {
    return avisoDePlazo(solicitud.estado, plazoRestante(solicitud.venceEn, this.ahora()));
  }

  /** La hora de salida o de la reserva, sin segundos. */
  protected hora(solicitud: SolicitudResumen): string | null {
    const hora = solicitud.horaSalida ?? solicitud.horaReserva;
    return hora ? normalizarHora(hora) : null;
  }

  /** Un código corto para citar la solicitud (las primeras letras de su identificador). */
  protected codigo(solicitud: SolicitudResumen): string {
    return solicitud.id.slice(0, 8).toUpperCase();
  }
}

function aPagina(valor: string | null): number {
  const pagina = Number(valor);
  return Number.isInteger(pagina) && pagina > 0 ? pagina : 0;
}
