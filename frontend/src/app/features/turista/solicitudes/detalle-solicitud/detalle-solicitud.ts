import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, catchError, map, merge, of, switchMap, tap } from 'rxjs';
import { CatalogoService } from '../../../../core/api/catalogo.service';
import { Zona } from '../../../../core/api/modelos';
import { SolicitudTurista } from '../../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../../core/api/reconsulta';
import { SolicitudesService } from '../../../../core/api/solicitudes.service';
import { ErrorApi } from '../../../../core/errores/error-api';
import { aErrorApi } from '../../../../core/errores/errores.interceptor';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { ahoraConReloj } from '../../../../core/tiempo/reloj';
import { TIPO_OFERTA } from '../../../../shared/etiquetas';
import { FechaPipe } from '../../../../shared/fecha.pipe';
import { ICONO_DE_TIPO, Icono } from '../../../../shared/icono/icono';
import { PasosSolicitud } from '../../../../shared/pasos-solicitud/pasos-solicitud';
import { PrecioPipe } from '../../../../shared/precio.pipe';
import {
  AccionSolicitud,
  CLASE_DE_ESTADO_SOLICITUD,
  DESCRIPCION_ESTADO,
  ETIQUETA_ACTOR,
  ETIQUETA_ESTADO,
  accionPrincipal,
  estaActiva,
  ordenarHistorial,
  pasoDe,
} from '../estado-solicitud';
import { normalizarHora } from '../peticion-solicitud';
import { plazoRestante } from '../plazo';
import { avisoDePlazo } from '../texto-plazo';

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; solicitud: SolicitudTurista }
  | { tipo: 'no-encontrada' }
  | { tipo: 'error'; error: ErrorApi };

/**
 * P02.2 Detalle de una solicitud (RF-33, RF-42): su estado real, los datos que pidió, las condiciones del operador cuando las
 * hay, el plazo vigente y el historial. Es la dirección `/turista/solicitudes/:id` a la que enlazan el regreso de la
 * pasarela de pago y el aviso de WhatsApp. Vuelve a consultar al regresar a la pestaña, al reconectarse y cuando el plazo
 * termina, en lugar de suponer el estado (ADR-04). Confirmar o declinar las condiciones va en su propio bloque.
 */
@Component({
  selector: 'app-detalle-solicitud',
  imports: [RouterLink, Icono, TPipe, FechaPipe, PrecioPipe, PasosSolicitud],
  templateUrl: './detalle-solicitud.html',
  styleUrl: './detalle-solicitud.css',
})
export class DetalleSolicitud {
  /** Llega de la ruta /turista/solicitudes/:id. */
  readonly id = input.required<string>();

  private readonly servicio = inject(SolicitudesService);
  private readonly catalogo = inject(CatalogoService);
  private readonly recarga = new Subject<void>();
  /** Las veces que el plazo ya terminó y se volvió a consultar, por versión de la solicitud: una sola consulta cada vez. */
  private readonly reconsultadas = new Set<string>();
  private readonly ahora = ahoraConReloj();

  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly descripcionEstado = DESCRIPCION_ESTADO;
  protected readonly claseDeEstado = CLASE_DE_ESTADO_SOLICITUD;
  protected readonly etiquetaActor = ETIQUETA_ACTOR;
  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;

  protected readonly estado = signal<Estado>({ tipo: 'cargando' });
  /** Una reconsulta falló pero se conserva lo que ya se veía. */
  protected readonly errorDeActualizacion = signal<ErrorApi | null>(null);
  private readonly zonas = signal<readonly Zona[]>([]);

  protected readonly solicitud = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'lista' ? estado.solicitud : null;
  });
  protected readonly mensajeDeError = computed(() => {
    const estado = this.estado();
    // Se guarda el error y no su texto: así el mensaje cambia de idioma con la interfaz.
    return estado.tipo === 'error' ? estado.error.mensaje : '';
  });
  protected readonly nombreDeZona = computed(
    () => this.zonas().find((zona) => zona.id === this.solicitud()?.oferta.zonaId)?.nombre ?? null,
  );
  protected readonly paso = computed(() => {
    const solicitud = this.solicitud();
    return solicitud ? pasoDe(solicitud.estado) : null;
  });
  protected readonly accion = computed<AccionSolicitud | null>(() => {
    const solicitud = this.solicitud();
    return solicitud ? accionPrincipal(solicitud.estado) : null;
  });
  protected readonly plazo = computed(() => plazoRestante(this.solicitud()?.venceEn, this.ahora()));
  protected readonly avisoDePlazo = computed(() => {
    const solicitud = this.solicitud();
    return solicitud ? avisoDePlazo(solicitud.estado, this.plazo()) : null;
  });
  protected readonly historial = computed(() =>
    ordenarHistorial(this.solicitud()?.historial ?? []),
  );
  /** La hora de salida o de la reserva, sin segundos. */
  protected readonly hora = computed(() => {
    const solicitud = this.solicitud();
    const hora = solicitud?.horaSalida ?? solicitud?.horaReserva;
    return hora ? normalizarHora(hora) : null;
  });
  protected readonly codigo = computed(() => this.solicitud()?.id.slice(0, 8).toUpperCase() ?? '');

  constructor() {
    this.catalogo
      .zonas()
      .pipe(
        catchError(() => of<Zona[]>([])),
        takeUntilDestroyed(),
      )
      .subscribe((zonas) => this.zonas.set(zonas));

    // Al cambiar de solicitud se muestra "cargando"; al reconsultar se conserva lo que ya se veía.
    merge(toObservable(this.id).pipe(map(() => true)), this.recarga.pipe(map(() => false)))
      .pipe(
        tap((esNueva) => {
          if (esNueva) {
            this.estado.set({ tipo: 'cargando' });
            this.errorDeActualizacion.set(null);
            this.reconsultadas.clear();
          }
        }),
        switchMap(() =>
          this.servicio.consultar(this.id()).pipe(
            map((solicitud): Estado => ({ tipo: 'lista', solicitud })),
            catchError((fallo: unknown) => {
              const error = aErrorApi(fallo);
              return of<Estado>(
                error.noEncontrado ? { tipo: 'no-encontrada' } : { tipo: 'error', error },
              );
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((estado) => {
        if (estado.tipo === 'error' && this.solicitud()) {
          this.errorDeActualizacion.set(estado.error);
          return;
        }
        this.errorDeActualizacion.set(null);
        this.estado.set(estado);
      });

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());

    // Cuando el plazo termina, el núcleo cierra la solicitud: se consulta una vez para ver su estado real.
    effect(() => {
      const solicitud = this.solicitud();
      if (!solicitud || !estaActiva(solicitud.estado) || this.plazo().nivel !== 'vencido') {
        return;
      }
      const version = `${solicitud.id}:${solicitud.actualizadaEn}`;
      if (!this.reconsultadas.has(version)) {
        this.reconsultadas.add(version);
        this.recarga.next();
      }
    });
  }
}
