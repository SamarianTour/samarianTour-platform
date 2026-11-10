import { Component, ElementRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, map, of, startWith, switchMap, tap } from 'rxjs';
import { CatalogoService } from '../../../core/api/catalogo.service';
import { FichaOferta } from '../../../core/api/modelos';
import { SolicitudOperador, TransicionSolicitud } from '../../../core/api/modelos-nucleo';
import { OperadorService } from '../../../core/api/operador.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { tablaDeTextos } from '../../../core/idioma/tabla-de-textos';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { ahoraConReloj } from '../../../core/tiempo/reloj';
import { TIPO_OFERTA } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { ICONO_DE_TIPO, Icono } from '../../../shared/icono/icono';
import { PrecioPipe } from '../../../shared/precio.pipe';
import {
  CLASE_DE_ESTADO_SOLICITUD,
  ETIQUETA_ESTADO,
  ordenarHistorial,
} from '../../turista/solicitudes/estado-solicitud';
import {
  nochesEntre,
  normalizarHora,
  precioDeReferencia,
} from '../../turista/solicitudes/peticion-solicitud';
import { plazoRestante } from '../../turista/solicitudes/plazo';
import { duracionTexto } from '../../turista/solicitudes/texto-plazo';
import {
  CampoAtencion,
  MotivoAtencion,
  ResultadoAtencion,
  VALORES_ATENCION_VACIOS,
  ValoresAtencion,
  construirAtencion,
  enlaceWhatsapp,
  notaMaxima,
  validarAtencion,
} from '../atencion';
import { codigoCorto } from '../cola';

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; solicitud: SolicitudOperador }
  | { tipo: 'no-encontrada' }
  | { tipo: 'error'; error: ErrorApi };

/** El precio de referencia de la oferta en el catálogo: solo orienta, así que si falla no impide atender. */
type Referencia = { tipo: 'cargando' } | { tipo: 'lista'; ficha: FichaOferta } | { tipo: 'error' };

/** Los campos del formulario que el núcleo puede señalar en un DATO_INVALIDO (`precioBase` es nuestro `precioTotal`). */
const CAMPO_DE_API: Readonly<Record<string, CampoAtencion>> = {
  precioBase: 'precioTotal',
  politicaCancelacion: 'politicaCancelacion',
  aparteAcordado: 'aparteAcordado',
  nota: 'nota',
};

const ETIQUETA_ACTOR = tablaDeTextos<TransicionSolicitud['actor']>({
  TURISTA: 'operador.atender.actor.TURISTA',
  OPERADOR: 'operador.atender.actor.OPERADOR',
  SISTEMA: 'operador.atender.actor.SISTEMA',
  PAGOS: 'operador.atender.actor.PAGOS',
});

/**
 * P02.5 Atender una solicitud (RF-37 a RF-40): el operador verifica el cupo con el prestador (le escribe por WhatsApp,
 * fuera de la plataforma) y registra el resultado. "Hay cupo" exige el precio TOTAL, la política de cancelación y que
 * confirmó con el prestador que el cupo queda apartado durante el plazo (RF-38, RF-39); "no hay cupo" lleva un motivo
 * opcional (RF-40). Junto al precio se muestran, solo como referencia, la cantidad pedida y el precio de la oferta en el
 * catálogo: el total lo escribe el operador. No se muestran datos personales del turista: el operador gestiona con el
 * prestador (minimización de datos, Ley 1581).
 */
@Component({
  selector: 'app-atender-solicitud',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe, FechaPipe, PrecioPipe],
  templateUrl: './atender-solicitud.html',
  styleUrl: './atender-solicitud.css',
})
export class AtenderSolicitud {
  /** Llega de la ruta /operador/solicitudes/:id. */
  readonly id = input.required<string>();

  private readonly operador = inject(OperadorService);
  private readonly catalogo = inject(CatalogoService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly pedirReferencia = new Subject<string>();
  private readonly ahora = ahoraConReloj();

  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly claseDeEstado = CLASE_DE_ESTADO_SOLICITUD;
  protected readonly etiquetaActor = ETIQUETA_ACTOR;
  protected readonly codigo = codigoCorto;

  protected readonly formulario = new FormGroup({
    resultado: new FormControl<ResultadoAtencion>('ATENDIDA', { nonNullable: true }),
    precioTotal: new FormControl<number | null>(null),
    politicaCancelacion: new FormControl('', { nonNullable: true }),
    aparteAcordado: new FormControl(false, { nonNullable: true }),
    nota: new FormControl('', { nonNullable: true }),
  });

  protected readonly estado = signal<Estado>({ tipo: 'cargando' });
  protected readonly solicitud = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'lista' ? estado.solicitud : null;
  });
  protected readonly mensajeDeError = computed(() => {
    const estado = this.estado();
    // Se guarda el error y no su texto: así el mensaje cambia de idioma con la interfaz.
    return estado.tipo === 'error' ? estado.error.mensaje : '';
  });
  protected readonly referencia = signal<Referencia>({ tipo: 'cargando' });
  protected readonly fichaDeReferencia = computed(() => {
    const referencia = this.referencia();
    return referencia.tipo === 'lista' ? referencia.ficha : null;
  });

  protected readonly valores = toSignal(
    this.formulario.valueChanges.pipe(
      startWith(null),
      map((): ValoresAtencion => ({ ...VALORES_ATENCION_VACIOS, ...this.formulario.getRawValue() })),
    ),
    { initialValue: { ...VALORES_ATENCION_VACIOS, ...this.formulario.getRawValue() } },
  );
  private readonly errores = computed(() => validarAtencion(this.valores()));
  protected readonly maximoNota = computed(() => notaMaxima(this.valores().resultado));
  protected readonly largoNota = computed(() => this.valores().nota.trim().length);

  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  /** El núcleo dijo que ya no alcanza el tiempo para confirmar: solo queda marcarla como no disponible (RF-39). */
  protected readonly sinTiempo = signal(false);
  private readonly erroresDeApi = signal<Partial<Record<CampoAtencion, TextoVivo>>>({});
  private readonly intentado = signal(false);
  /** Cambia con cada edición, para que los mensajes de error se vuelvan a calcular. */
  private readonly cambios = signal(0);

  /** Solo una solicitud pendiente se puede atender; el resto se muestra como consulta (una vez atendida, sus datos no cambian). */
  protected readonly sePuedeAtender = computed(() => this.solicitud()?.estado === 'PENDIENTE');

  protected readonly whatsapp = computed(() => enlaceWhatsapp(this.solicitud()?.whatsappPrestador));
  protected readonly historial = computed(() => ordenarHistorial(this.solicitud()?.historial ?? []));

  /** Cuánto queda para atenderla: solo aplica mientras está pendiente. */
  protected readonly plazo = computed(() => {
    const solicitud = this.solicitud();
    return solicitud?.estado === 'PENDIENTE'
      ? plazoRestante(solicitud.venceEn, this.ahora())
      : null;
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        tap(() => this.estado.set({ tipo: 'cargando' })),
        switchMap((id) =>
          this.operador.consultar(id).pipe(
            map((solicitud): Estado => ({ tipo: 'lista', solicitud })),
            catchError((fallo: unknown) => {
              const error = aErrorApi(fallo);
              return of<Estado>(error.noEncontrado ? { tipo: 'no-encontrada' } : { tipo: 'error', error });
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((estado) => this.mostrar(estado));

    // El precio de la oferta en el catálogo (público). Si falla, se avisa y se puede seguir.
    this.pedirReferencia
      .pipe(
        tap(() => this.referencia.set({ tipo: 'cargando' })),
        switchMap((ofertaId) =>
          this.catalogo.ficha(ofertaId).pipe(
            map((ficha): Referencia => ({ tipo: 'lista', ficha })),
            catchError(() => of<Referencia>({ tipo: 'error' })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((referencia) => this.referencia.set(referencia));

    // Al corregir un dato desaparece el error que la API había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.erroresDeApi.set({});
      this.cambios.update((cambios) => cambios + 1);
    });
  }

  private mostrar(estado: Estado): void {
    this.intentado.set(false);
    this.erroresDeApi.set({});
    this.sinTiempo.set(false);
    this.errorGeneral.set(null);
    this.formulario.reset(VALORES_ATENCION_VACIOS);
    this.estado.set(estado);
    if (estado.tipo === 'lista' && estado.solicitud.estado === 'PENDIENTE') {
      this.pedirReferencia.next(estado.solicitud.oferta.id);
    }
  }

  /** Vuelve a pedir la solicitud para ver su estado real (por ejemplo, si otro proceso la cerró), sin borrar el aviso. */
  private recargar(): void {
    this.operador.consultar(this.id()).subscribe({
      next: (solicitud) => this.estado.set({ tipo: 'lista', solicitud }),
      error: () => undefined,
    });
  }

  // --- Lo que se muestra de la solicitud ---

  /** La hora de salida o de la reserva, sin segundos. */
  protected hora(solicitud: SolicitudOperador): string | null {
    const hora = solicitud.horaSalida ?? solicitud.horaReserva;
    return hora ? normalizarHora(hora) : null;
  }

  protected noches(solicitud: SolicitudOperador): number {
    return nochesEntre(solicitud.fechaInicio, solicitud.fechaSalida ?? '');
  }

  /** Lo que pidieron, en palabras: "2 personas", o en alojamiento "3 noches · 1 unidad · 2 personas". */
  protected cantidad(solicitud: SolicitudOperador): string {
    const personas = traducir('solicitudes.lista.personas', { n: solicitud.personas });
    if (solicitud.oferta.tipo !== 'ALOJAMIENTO') {
      return personas;
    }
    const partes = [
      traducir('operador.atender.referencia.noches', { n: this.noches(solicitud) }),
      traducir('solicitudes.lista.unidades', { n: solicitud.numeroUnidades ?? 0 }),
      personas,
    ];
    return partes.join(' · ');
  }

  /** El cálculo de referencia (precio del catálogo × lo pedido), o null si faltan datos. Solo orienta [SUPUESTO]. */
  protected estimado(solicitud: SolicitudOperador, ficha: FichaOferta): number | null {
    return precioDeReferencia(solicitud.oferta.tipo, ficha.precioReferencia, {
      personas: solicitud.personas,
      fechaInicio: solicitud.fechaInicio,
      fechaSalida: solicitud.fechaSalida ?? '',
      numeroUnidades: solicitud.numeroUnidades ?? null,
    });
  }

  /** El aviso del plazo para atenderla, o null si no aplica. */
  protected avisoDePlazo(): string | null {
    const plazo = this.plazo();
    if (!plazo || plazo.nivel === 'sin-plazo') {
      return null;
    }
    return plazo.nivel === 'vencido'
      ? traducir('operador.atender.plazo.vencido')
      : traducir('operador.atender.plazo.restante', { tiempo: duracionTexto(plazo) });
  }

  // --- Formulario ---

  /** Mensaje de error de un campo: primero el de la API; si no, el de la validación local una vez tocado o al enviar. */
  protected errorDe(campo: CampoAtencion): string | null {
    this.cambios();
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi();
    }
    const motivo = this.errores()[campo];
    if (!motivo || !(this.intentado() || this.formulario.controls[campo].touched)) {
      return null;
    }
    return traducir(claveDeError(campo, motivo), { maximo: this.maximoNota() });
  }

  protected enviar(): void {
    const solicitud = this.solicitud();
    if (this.enviando() || !solicitud || !this.sePuedeAtender()) {
      return;
    }
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    this.intentado.set(true);
    this.cambios.update((cambios) => cambios + 1);

    const errores = this.errores();
    const primerError = CAMPOS_EN_ORDEN.find((campo) => errores[campo]);
    if (primerError) {
      this.enfocar(primerError);
      return;
    }

    const valores = this.valores();
    this.enviando.set(true);
    this.operador.atender(solicitud.id, construirAtencion(valores)).subscribe({
      next: () => {
        this.enviando.set(false);
        this.avisos.mostrar(
          traducir(
            valores.resultado === 'ATENDIDA'
              ? 'operador.atender.exito.atendida'
              : 'operador.atender.exito.noDisponible',
          ),
        );
        void this.router.navigate(['/operador']);
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        this.mostrarError(aErrorApi(fallo));
      },
    });
  }

  private mostrarError(error: ErrorApi): void {
    if (error.codigo === 'DATO_INVALIDO' && error.campo && CAMPO_DE_API[error.campo]) {
      const campo = CAMPO_DE_API[error.campo];
      this.erroresDeApi.set({ [campo]: () => mayusculaInicial(error.detalleCampo ?? error.mensaje) });
      this.enfocar(campo);
      return;
    }
    this.errorGeneral.set(() => error.mensaje);
    if (error.codigo === 'SIN_TIEMPO_PARA_CONFIRMAR') {
      // No alcanza el tiempo para confirmar antes del servicio: solo puede marcarla como no disponible (RF-39).
      this.sinTiempo.set(true);
      this.formulario.controls.resultado.setValue('NO_DISPONIBLE');
    } else if (error.codigo === 'TRANSICION_INVALIDA' || error.codigo === 'PLAZO_VENCIDO') {
      // La solicitud cambió mientras la atendía (por ejemplo, la cerró el vencimiento): se muestra su estado real.
      this.recargar();
    }
  }

  /** Lleva el foco al campo con error; los campos tienen como id el nombre de su control. */
  private enfocar(campo: CampoAtencion): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#${campo}`)?.focus();
  }
}

/** El orden en que están los campos en la pantalla: el foco va al primero con error. */
const CAMPOS_EN_ORDEN: readonly CampoAtencion[] = [
  'precioTotal',
  'politicaCancelacion',
  'aparteAcordado',
  'nota',
];

function mayusculaInicial(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** La clave del mensaje para el motivo de error de un campo. */
function claveDeError(campo: CampoAtencion, motivo: MotivoAtencion): ClaveTexto {
  switch (campo) {
    case 'precioTotal':
      return motivo === 'entero'
        ? 'operador.atender.error.precioEntero'
        : motivo === 'minimo'
          ? 'operador.atender.error.precioMinimo'
          : 'operador.atender.error.precioRequerido';
    case 'politicaCancelacion':
      return motivo === 'largo'
        ? 'operador.atender.error.politicaLarga'
        : 'operador.atender.error.politicaRequerida';
    case 'aparteAcordado':
      return 'operador.atender.error.apartePendiente';
    case 'nota':
      return 'operador.atender.error.notaLarga';
  }
}
