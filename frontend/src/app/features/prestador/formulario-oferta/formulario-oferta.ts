import { Component, ElementRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of, startWith, switchMap, tap } from 'rxjs';
import { modalidadRecorridoValues, nivelEsfuerzoValues, tipoActividadValues } from '../../../core/api/catalogo-api';
import { CatalogoService } from '../../../core/api/catalogo.service';
import {
  Coordenada,
  FichaOferta,
  ModalidadRecorrido,
  NivelEsfuerzo,
  OfertaPrestador,
  TipoActividad,
  TipoOferta,
  Zona,
} from '../../../core/api/modelos';
import { OfertasPrestadorService } from '../../../core/api/ofertas-prestador.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { MENSAJES } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { IdentidadService } from '../../../core/identidad/identidad.service';
import {
  MODALIDAD,
  NIVEL_ESFUERZO,
  TIPOS_QUE_PUBLICA,
  TIPO_ACTIVIDAD,
  TIPO_OFERTA,
  UNIDAD_PRECIO,
} from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';
import { Mapa } from '../../../shared/mapa/mapa';
import { versionVigente } from '../estado-oferta';
import { ValoresOferta, aLista, aPeticion, valoresDeFicha } from './peticion-oferta';

type Campo = keyof ValoresOferta;

type Estado = { tipo: 'cargando' } | { tipo: 'lista' } | { tipo: 'bloqueada'; mensaje: string };

/** Lo que hace falta para mostrar el formulario; `bloqueo` explica por qué no se puede editar. */
interface Carga {
  readonly zonas: Zona[];
  readonly propias: OfertaPrestador[];
  readonly ficha?: FichaOferta;
  readonly conCambiosEnRevision?: boolean;
  readonly bloqueo?: string;
}

/** Una actividad propia que se puede incluir en un paquete. */
interface ActividadPropia {
  readonly id: string;
  readonly nombre: string;
  readonly sinAprobar: boolean;
}

const MINIMO_ACTIVIDADES = 2;
const HORA = /^([01]\d|2[0-3]):[0-5]\d$/;
const IDIOMA = /^[a-z]{2}$/;

/** Un texto con solo espacios no cuenta: el dominio también lo rechaza. */
const obligatorio: ValidatorFn = (control) =>
  typeof control.value === 'string' && control.value.trim() !== '' ? null : { required: true };

const entero: ValidatorFn = (control) =>
  control.value === null || Number.isInteger(control.value) ? null : { entero: true };

const horas: ValidatorFn = (control) =>
  aLista(String(control.value ?? '')).every((hora) => HORA.test(hora)) ? null : { horas: true };

const idiomas: ValidatorFn = (control) =>
  aLista(String(control.value ?? '')).every((idioma) => IDIOMA.test(idioma)) ? null : { idiomas: true };

const minimoDeActividades: ValidatorFn = (control) =>
  Array.isArray(control.value) && control.value.length >= MINIMO_ACTIVIDADES ? null : { actividades: true };

/**
 * Formulario para publicar una oferta (POST) o editarla (PUT). Los campos cambian con el tipo de oferta y solo se
 * ofrecen los tipos que el prestador puede publicar (DC-10). Las validaciones repiten las del dominio para avisar
 * antes de enviar; la API las vuelve a aplicar y sus errores de campo se muestran junto al campo.
 */
@Component({
  selector: 'app-formulario-oferta',
  imports: [ReactiveFormsModule, RouterLink, Mapa, Icono],
  templateUrl: './formulario-oferta.html',
  styleUrl: './formulario-oferta.css',
})
export class FormularioOferta {
  /** Llega de la ruta /prestador/ofertas/:id/editar; no existe al publicar una oferta nueva. */
  readonly id = input<string>();

  private readonly catalogo = inject(CatalogoService);
  private readonly servicio = inject(OfertasPrestadorService);
  private readonly identidad = inject(IdentidadService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  /** El panel solo muestra el formulario con un prestador elegido. */
  private readonly prestador = this.identidad.prestador()!;
  private readonly esGuia = this.prestador.tipo === 'GUIA_INDEPENDIENTE';

  protected readonly tiposPermitidos = TIPOS_QUE_PUBLICA[this.prestador.tipo];
  protected readonly tiposActividad = tipoActividadValues;
  protected readonly niveles = nivelEsfuerzoValues;
  protected readonly modalidades = modalidadRecorridoValues;
  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly etiquetaActividad = TIPO_ACTIVIDAD;
  protected readonly etiquetaNivel = NIVEL_ESFUERZO;
  protected readonly etiquetaModalidad = MODALIDAD;
  protected readonly unidad = UNIDAD_PRECIO;
  protected readonly minimoActividades = MINIMO_ACTIVIDADES;
  /** El guía independiente debe completar los datos del recorrido (RF-67). */
  protected readonly exigeRecorrido = this.esGuia;

  protected readonly formulario = new FormGroup({
    tipo: new FormControl<TipoOferta>(this.tiposPermitidos[0], { nonNullable: true }),
    nombre: new FormControl('', { nonNullable: true, validators: obligatorio }),
    descripcion: new FormControl('', { nonNullable: true, validators: obligatorio }),
    precioReferencia: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    horarios: new FormControl('', { nonNullable: true }),
    zonaId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    comoLlegar: new FormControl('', { nonNullable: true }),
    capacidadMaxima: new FormControl<number | null>(null, [Validators.required, Validators.min(1), entero]),
    accesibleSillaRuedas: new FormControl(false, { nonNullable: true }),
    tipoActividad: new FormControl<TipoActividad | ''>('', { nonNullable: true }),
    nivelEsfuerzo: new FormControl<NivelEsfuerzo | ''>('', { nonNullable: true }),
    horariosSalida: new FormControl('', { nonNullable: true }),
    puntoEncuentro: new FormControl('', { nonNullable: true }),
    duracionMinutos: new FormControl<number | null>(null),
    modalidad: new FormControl<ModalidadRecorrido | ''>('', { nonNullable: true }),
    idiomasAtencion: new FormControl('', { nonNullable: true }),
    actividadIds: new FormControl<string[]>([], { nonNullable: true }),
    tipoUnidad: new FormControl('', { nonNullable: true }),
  });

  protected readonly estado = signal<Estado>({ tipo: 'cargando' });
  protected readonly zonas = signal<readonly Zona[]>([]);
  protected readonly actividades = signal<readonly ActividadPropia[]>([]);
  protected readonly ubicacion = signal<Coordenada | null>(null);
  /**
   * [SUPUESTO] El formulario todavía no sube fotos (DC-47): al editar se conservan las de la versión publicada, y una
   * oferta nueva se envía sin fotos, así que la API la rechaza hasta que exista la pantalla de fotos.
   */
  private readonly fotos = signal<string[]>([]);
  protected readonly conCambiosEnRevision = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);
  /** Errores de campo que devolvió la API, por nombre de campo. */
  private readonly erroresDeApi = signal<Partial<Record<Campo, string>>>({});
  /** Cambia con cada edición del formulario, para que los mensajes de error se vuelvan a calcular. */
  private readonly cambios = signal(0);

  protected readonly tipo = toSignal(
    this.formulario.controls.tipo.valueChanges.pipe(startWith(this.formulario.controls.tipo.value)),
    { initialValue: this.formulario.controls.tipo.value },
  );
  protected readonly editando = computed(() => !!this.id());
  protected readonly mensajeDeBloqueo = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'bloqueada' ? estado.mensaje : '';
  });

  constructor() {
    this.ajustarValidadores(this.formulario.controls.tipo.value);
    this.formulario.controls.tipo.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((tipo) => this.ajustarValidadores(tipo));

    // Al corregir un dato desaparece el error que la API había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.erroresDeApi.set({});
      this.cambios.update((cambios) => cambios + 1);
    });

    toObservable(this.id)
      .pipe(
        tap(() => this.estado.set({ tipo: 'cargando' })),
        switchMap((id) =>
          this.cargar(id).pipe(
            catchError((error: unknown) =>
              of<Carga>({ zonas: [], propias: [], bloqueo: aErrorApi(error).mensaje }),
            ),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((carga) => this.mostrar(carga));
  }

  /** Mensaje de error de un campo: primero el de la API; si no, el de la validación local una vez tocado. */
  protected errorDe(campo: Campo): string | null {
    this.cambios();
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi;
    }
    const control = this.formulario.controls[campo];
    return control.invalid && control.touched ? mensajeDe(control.errors) : null;
  }

  protected marcada(id: string): boolean {
    return this.formulario.controls.actividadIds.value.includes(id);
  }

  protected alternarActividad(id: string, incluida: boolean): void {
    const control = this.formulario.controls.actividadIds;
    const resto = control.value.filter((actual) => actual !== id);
    control.setValue(incluida ? [...resto, id] : resto);
    control.markAsTouched();
  }

  protected ponerUbicacion(ubicacion: Coordenada): void {
    this.ubicacion.set(ubicacion);
  }

  protected quitarUbicacion(): void {
    this.ubicacion.set(null);
  }

  protected guardar(): void {
    if (this.guardando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.cambios.update((cambios) => cambios + 1);
      this.enfocar(this.primerCampoInvalido());
      return;
    }

    const id = this.id();
    const peticion = aPeticion(this.formulario.getRawValue(), this.ubicacion(), this.fotos());
    this.guardando.set(true);
    (id ? this.servicio.editar(id, peticion) : this.servicio.publicar(peticion)).subscribe({
      next: (oferta) => {
        this.guardando.set(false);
        const numero = oferta.pendiente?.numero;
        this.avisos.mostrar(
          numero ? `Enviamos tu oferta a moderación (versión ${numero})` : 'Enviamos tu oferta a moderación',
        );
        void this.router.navigate(['/prestador/ofertas']);
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.mostrarError(fallo);
      },
    });
  }

  /** Zonas, ofertas propias y, al editar, la ficha publicada de la oferta. */
  private cargar(id: string | undefined): Observable<Carga> {
    return forkJoin({ zonas: this.catalogo.zonas(), propias: this.servicio.todas() }).pipe(
      switchMap((base): Observable<Carga> => {
        if (!id) {
          return of(base);
        }
        const propia = base.propias.find((oferta) => oferta.id === id);
        if (!propia) {
          return of({ ...base, bloqueo: 'No encontramos esta oferta entre las tuyas.' });
        }
        if (propia.retirada) {
          return of({ ...base, bloqueo: MENSAJES.OFERTA_NO_EDITABLE });
        }
        if (!propia.publicada) {
          return of({
            ...base,
            bloqueo:
              'Esta oferta todavía no tiene una versión publicada. Podrás editarla cuando la moderación la apruebe.',
          });
        }
        return this.catalogo.ficha(id).pipe(
          map((ficha): Carga => ({ ...base, ficha, conCambiosEnRevision: !!propia.pendiente })),
          catchError((error: unknown) => {
            const fallo = aErrorApi(error);
            return of<Carga>({
              ...base,
              bloqueo: fallo.noEncontrado
                ? 'No podemos cargar esta oferta para editarla porque ahora no está visible en el catálogo.'
                : fallo.mensaje,
            });
          }),
        );
      }),
    );
  }

  private mostrar(carga: Carga): void {
    if (carga.bloqueo) {
      this.estado.set({ tipo: 'bloqueada', mensaje: carga.bloqueo });
      return;
    }
    this.zonas.set(carga.zonas);
    // Un paquete solo admite actividades propias que no estén retiradas (DC-23).
    this.actividades.set(
      carga.propias
        .filter((oferta) => oferta.tipo === 'ACTIVIDAD' && !oferta.retirada)
        .map((oferta) => ({
          id: oferta.id,
          nombre: versionVigente(oferta)?.nombre ?? 'Actividad sin nombre',
          sinAprobar: !oferta.publicada,
        })),
    );
    if (carga.ficha) {
      const { valores, ubicacion, fotos } = valoresDeFicha(carga.ficha);
      const disponibles = new Set(this.actividades().map((actividad) => actividad.id));
      this.formulario.setValue({
        ...valores,
        actividadIds: valores.actividadIds.filter((actividadId) => disponibles.has(actividadId)),
      });
      this.ubicacion.set(ubicacion);
      this.fotos.set(fotos);
    }
    this.conCambiosEnRevision.set(carga.conCambiosEnRevision ?? false);
    this.estado.set({ tipo: 'lista' });
  }

  /** Cada tipo de oferta exige campos distintos; los de otros tipos quedan sin validar y no se envían. */
  private ajustarValidadores(tipo: TipoOferta): void {
    const controles = this.formulario.controls;
    const esActividad = tipo === 'ACTIVIDAD';
    const esPaquete = tipo === 'PAQUETE';
    const conRecorrido = esActividad && this.esGuia;

    controles.tipoActividad.setValidators(esActividad ? Validators.required : null);
    controles.nivelEsfuerzo.setValidators(esActividad ? Validators.required : null);
    controles.horariosSalida.setValidators(esActividad || esPaquete ? [obligatorio, horas] : null);
    controles.puntoEncuentro.setValidators(conRecorrido ? obligatorio : null);
    controles.duracionMinutos.setValidators(
      esActividad || esPaquete
        ? [...(esPaquete || conRecorrido ? [Validators.required] : []), Validators.min(1), entero]
        : null,
    );
    controles.modalidad.setValidators(conRecorrido ? Validators.required : null);
    controles.idiomasAtencion.setValidators(
      esActividad ? [...(conRecorrido ? [obligatorio] : []), idiomas] : null,
    );
    controles.actividadIds.setValidators(esPaquete ? minimoDeActividades : null);

    for (const control of Object.values(controles)) {
      control.updateValueAndValidity({ emitEvent: false });
    }
  }

  private mostrarError(fallo: unknown): void {
    const error = aErrorApi(fallo);
    const campo = error.campo;
    if (campo && esCampo(campo, this.formulario.controls)) {
      this.erroresDeApi.set({ [campo]: mayusculaInicial(error.detalleCampo ?? error.mensaje) });
      this.enfocar(campo);
      return;
    }
    this.errorGeneral.set(error.detalleCampo ? `${error.mensaje} Detalle: ${error.detalleCampo}.` : error.mensaje);
  }

  private primerCampoInvalido(): Campo | undefined {
    const controles = this.formulario.controls;
    return (Object.keys(controles) as Campo[]).find((campo) => controles[campo].invalid);
  }

  /** Lleva el foco al campo con error; los campos tienen como id el nombre de su control. */
  private enfocar(campo: Campo | undefined): void {
    if (campo) {
      this.anfitrion.nativeElement.querySelector<HTMLElement>(`#${campo}`)?.focus();
    }
  }
}

function esCampo(nombre: string, controles: Record<Campo, AbstractControl>): nombre is Campo {
  return nombre in controles;
}

function mayusculaInicial(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function mensajeDe(errores: ValidationErrors | null): string | null {
  if (!errores) {
    return null;
  }
  if (errores['required']) {
    return 'Este dato es obligatorio.';
  }
  if (errores['min']) {
    return `Debe ser ${errores['min'].min} o más.`;
  }
  if (errores['entero']) {
    return 'Debe ser un número entero.';
  }
  if (errores['horas']) {
    return 'Escribe cada hora con el formato 07:30 y sepáralas con comas.';
  }
  if (errores['idiomas']) {
    return 'Usa códigos de dos letras en minúscula, como es o en, separados por comas.';
  }
  if (errores['actividades']) {
    return `Elige al menos ${MINIMO_ACTIVIDADES} actividades.`;
  }
  return 'Revisa este dato.';
}
