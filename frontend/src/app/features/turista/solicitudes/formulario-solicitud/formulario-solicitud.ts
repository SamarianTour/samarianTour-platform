import { Component, ElementRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, map, of, startWith, switchMap, tap } from 'rxjs';
import { CatalogoService } from '../../../../core/api/catalogo.service';
import { FichaOferta } from '../../../../core/api/modelos';
import { ClaveDeIdempotencia } from '../../../../core/api/idempotencia';
import { SolicitudesService } from '../../../../core/api/solicitudes.service';
import { AvisosService } from '../../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../../core/errores/error-api';
import { aErrorApi } from '../../../../core/errores/errores.interceptor';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../../core/idioma/texto-vivo';
import type { ClaveTexto } from '../../../../core/idioma/textos';
import { traducir } from '../../../../core/idioma/traducir';
import { SesionService } from '../../../../core/sesion/sesion.service';
import { TIPO_OFERTA } from '../../../../shared/etiquetas';
import { FechaPipe } from '../../../../shared/fecha.pipe';
import { ICONO_DE_TIPO, Icono } from '../../../../shared/icono/icono';
import { PasosSolicitud } from '../../../../shared/pasos-solicitud/pasos-solicitud';
import { PrecioPipe } from '../../../../shared/precio.pipe';
import { UnidadPipe } from '../../../../shared/unidad.pipe';
import {
  CampoSolicitud,
  MotivoDeError,
  NOTA_MAXIMA,
  VALORES_VACIOS,
  ValoresSolicitud,
  aPeticion,
  camposDe,
  fechaMinima,
  nochesEntre,
  normalizarHora,
  precioDeReferencia,
  sumarDias,
  validarSolicitud,
} from '../peticion-solicitud';

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; ficha: FichaOferta }
  | { tipo: 'no-disponible' }
  | { tipo: 'error'; error: ErrorApi };

/** Los campos del formulario que el núcleo puede señalar en un DATO_INVALIDO. */
const CAMPOS_DEL_FORMULARIO: readonly CampoSolicitud[] = [
  'fechaInicio',
  'horaSalida',
  'horaReserva',
  'fechaSalida',
  'numeroUnidades',
  'personas',
  'nota',
];

const fechaLarga = new FechaPipe();

/**
 * P02.1 Formulario de solicitud de disponibilidad (RF-30 a RF-32, QAS-08). Pide solo lo que el contrato necesita: la fecha,
 * la hora o las fechas que corresponden al tipo de oferta, y cuántas personas; el nombre y el contacto salen de la cuenta.
 * Con una sola hora de salida y las personas en 1, basta elegir la fecha. No es una reserva ni un pago.
 */
@Component({
  selector: 'app-formulario-solicitud',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Icono,
    TPipe,
    PrecioPipe,
    UnidadPipe,
    FechaPipe,
    PasosSolicitud,
  ],
  templateUrl: './formulario-solicitud.html',
  styleUrl: './formulario-solicitud.css',
})
export class FormularioSolicitud {
  /** Llega de la ruta /turista/solicitar/:ofertaId. */
  readonly ofertaId = input.required<string>();

  private readonly catalogo = inject(CatalogoService);
  private readonly solicitudes = inject(SolicitudesService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  /** La misma clave en cada reintento de la misma solicitud; otra si el turista cambia algún dato. */
  private readonly clave = new ClaveDeIdempotencia();

  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;
  protected readonly notaMaxima = NOTA_MAXIMA;

  protected readonly formulario = new FormGroup({
    fechaInicio: new FormControl('', { nonNullable: true }),
    horaSalida: new FormControl('', { nonNullable: true }),
    horaReserva: new FormControl('', { nonNullable: true }),
    fechaSalida: new FormControl('', { nonNullable: true }),
    numeroUnidades: new FormControl<number | null>(null),
    personas: new FormControl<number | null>(1),
    nota: new FormControl('', { nonNullable: true }),
  });

  protected readonly estado = signal<Estado>({ tipo: 'cargando' });
  protected readonly ficha = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'lista' ? estado.ficha : null;
  });
  protected readonly mensajeDeError = computed(() => {
    const estado = this.estado();
    // Se guarda el error y no su texto: así el mensaje cambia de idioma con la interfaz.
    return estado.tipo === 'error' ? estado.error.mensaje : '';
  });

  /** El momento que se usa para contar los 2 días de anticipación; se renueva al enviar. */
  private readonly ahora = signal(Date.now());
  protected readonly minimaFecha = computed(() => fechaMinima(this.ahora()));

  protected readonly valores = toSignal(
    this.formulario.valueChanges.pipe(
      startWith(null),
      map((): ValoresSolicitud => ({ ...VALORES_VACIOS, ...this.formulario.getRawValue() })),
    ),
    { initialValue: { ...VALORES_VACIOS, ...this.formulario.getRawValue() } },
  );

  protected readonly campos = computed(() => {
    const ficha = this.ficha();
    return ficha ? camposDe(ficha.tipo) : [];
  });
  /** Las horas de salida de la oferta, sin segundos. */
  protected readonly horasDeSalida = computed(() => {
    const ficha = this.ficha();
    return (ficha?.actividad?.horariosSalida ?? ficha?.paquete?.horariosSalida ?? []).map(
      normalizarHora,
    );
  });
  private readonly errores = computed(() => {
    const ficha = this.ficha();
    return ficha
      ? validarSolicitud(this.valores(), {
          tipo: ficha.tipo,
          capacidadMaxima: ficha.capacidadMaxima,
          horariosSalida: this.horasDeSalida(),
          ahora: this.ahora(),
        })
      : {};
  });
  protected readonly menorFechaDeSalida = computed(() =>
    sumarDias(this.valores().fechaInicio || this.minimaFecha(), 1),
  );

  /** El precio de referencia de lo pedido, o null mientras falten datos [SUPUESTO]. */
  protected readonly precioReferencia = computed(() => {
    const ficha = this.ficha();
    return ficha ? precioDeReferencia(ficha.tipo, ficha.precioReferencia, this.valores()) : null;
  });
  protected readonly noches = computed(() =>
    nochesEntre(this.valores().fechaInicio, this.valores().fechaSalida),
  );
  protected readonly largoNota = computed(() => this.valores().nota.trim().length);

  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  /** El núcleo dijo que ya hay una solicitud abierta igual: se ofrece ir a verla. */
  protected readonly esDuplicada = signal(false);
  /** Errores de campo que devolvió la API, por campo. */
  private readonly erroresDeApi = signal<Partial<Record<CampoSolicitud, TextoVivo>>>({});
  private readonly whatsappRechazado = signal(false);
  private readonly intentado = signal(false);
  /** Cambia con cada edición, para que los mensajes de error se vuelvan a calcular. */
  private readonly cambios = signal(0);

  /** Sin el WhatsApp verificado no se puede enviar (RF-30): lo dice la cuenta o lo rechaza el núcleo. */
  protected readonly sinWhatsapp = computed(
    () => this.sesion.cuenta()?.whatsappVerificado === false || this.whatsappRechazado(),
  );

  constructor() {
    toObservable(this.ofertaId)
      .pipe(
        tap(() => this.estado.set({ tipo: 'cargando' })),
        switchMap((id) =>
          this.catalogo.ficha(id).pipe(
            map((ficha): Estado => ({ tipo: 'lista', ficha })),
            catchError((fallo: unknown) => {
              const error = aErrorApi(fallo);
              return of<Estado>(
                error.noEncontrado ? { tipo: 'no-disponible' } : { tipo: 'error', error },
              );
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((estado) => this.mostrar(estado));

    // Al corregir un dato desaparece el error que la API había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.erroresDeApi.set({});
      this.cambios.update((cambios) => cambios + 1);
    });
  }

  private mostrar(estado: Estado): void {
    this.ahora.set(Date.now());
    this.intentado.set(false);
    this.errorGeneral.set(null);
    this.esDuplicada.set(false);
    this.whatsappRechazado.set(false);
    this.clave.reiniciar();
    this.formulario.reset({ ...VALORES_VACIOS, personas: 1 });
    this.estado.set(estado);
    if (estado.tipo === 'lista') {
      const horas = (
        estado.ficha.actividad?.horariosSalida ??
        estado.ficha.paquete?.horariosSalida ??
        []
      ).map(normalizarHora);
      // Con una sola hora de salida no hay nada que elegir.
      if (horas.length === 1) {
        this.formulario.controls.horaSalida.setValue(horas[0]);
      }
    }
  }

  /** Mensaje de error de un campo: primero el de la API; si no, el de la validación local una vez tocado o al enviar. */
  protected errorDe(campo: CampoSolicitud): string | null {
    this.cambios();
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi();
    }
    const motivo = this.errores()[campo];
    if (!motivo || !(this.intentado() || this.formulario.controls[campo].touched)) {
      return null;
    }
    return traducir(claveDeError(campo, motivo), {
      fecha: fechaLarga.transform(this.minimaFecha(), 'larga'),
      maximo: campo === 'nota' ? NOTA_MAXIMA : (this.ficha()?.capacidadMaxima ?? 0),
    });
  }

  protected enviar(): void {
    const ficha = this.ficha();
    if (this.enviando() || !ficha) {
      return;
    }
    this.errorGeneral.set(null);
    this.esDuplicada.set(false);
    this.erroresDeApi.set({});
    this.ahora.set(Date.now());
    this.intentado.set(true);
    this.cambios.update((cambios) => cambios + 1);

    const primerError = this.campos().find((campo) => this.errores()[campo]);
    if (primerError) {
      this.enfocar(primerError);
      return;
    }
    if (this.sinWhatsapp()) {
      return;
    }

    const peticion = aPeticion(this.valores(), ficha.id, ficha.tipo);
    this.enviando.set(true);
    this.solicitudes.enviar(peticion, this.clave.para(peticion)).subscribe({
      next: (solicitud) => {
        this.enviando.set(false);
        this.clave.reiniciar();
        this.avisos.mostrar(traducir('solicitudes.form.enviada'));
        void this.router.navigate(['/turista/solicitudes', solicitud.id]);
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        this.mostrarError(aErrorApi(fallo));
      },
    });
  }

  private mostrarError(error: ErrorApi): void {
    if (error.codigo === 'WHATSAPP_NO_VERIFICADO') {
      this.whatsappRechazado.set(true);
      return;
    }
    const campo = CAMPOS_DEL_FORMULARIO.find((nombre) => nombre === error.campo);
    if (campo) {
      this.erroresDeApi.set({
        [campo]: () => mayusculaInicial(error.detalleCampo ?? error.mensaje),
      });
      this.enfocar(campo);
      return;
    }
    this.esDuplicada.set(error.codigo === 'SOLICITUD_DUPLICADA');
    this.errorGeneral.set(() => error.mensaje);
  }

  /** Lleva el foco al campo con error; los campos tienen como id el nombre de su control. */
  private enfocar(campo: CampoSolicitud): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#${campo}`)?.focus();
  }
}

function mayusculaInicial(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** La clave del mensaje para el motivo de error de un campo. */
function claveDeError(campo: CampoSolicitud, motivo: MotivoDeError): ClaveTexto {
  const esFecha = campo === 'fechaInicio' || campo === 'fechaSalida';
  const esHora = campo === 'horaSalida' || campo === 'horaReserva';
  switch (motivo) {
    case 'requerido':
      return esFecha
        ? 'solicitudes.error.fechaRequerido'
        : esHora
          ? 'solicitudes.error.horaRequerida'
          : 'solicitudes.error.cantidadRequerida';
    case 'formato':
      return esFecha ? 'solicitudes.error.fechaFormato' : 'solicitudes.error.horaFormato';
    case 'pasada':
      return 'solicitudes.error.fechaPasada';
    case 'antesDeInicio':
      return 'solicitudes.error.salidaAntes';
    case 'noOfrecida':
      return 'solicitudes.error.horaNoOfrecida';
    case 'entero':
      return 'solicitudes.error.cantidadEntera';
    case 'minimo':
      return 'solicitudes.error.cantidadMinima';
    case 'maximo':
      return 'solicitudes.error.personasMaximo';
    case 'largo':
      return 'solicitudes.error.notaLarga';
  }
}
