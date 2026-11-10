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
  ContenidoVersion,
  Coordenada,
  Foto,
  ModalidadRecorrido,
  NivelEsfuerzo,
  OfertaPrestador,
  OfertaPropia,
  TipoActividad,
  TipoOferta,
  Zona,
} from '../../../core/api/modelos';
import { OfertasPrestadorService } from '../../../core/api/ofertas-prestador.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { MENSAJES } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo, textoVivo } from '../../../core/idioma/texto-vivo';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
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
import { MINIMO_FOTOS } from './fotos-oferta';
import { ValoresOferta, aLista, aPeticion, valoresDeContenido } from './peticion-oferta';
import { SelectorFotos } from './selector-fotos/selector-fotos';

type Campo = keyof ValoresOferta;

type Estado = { tipo: 'cargando' } | { tipo: 'lista' } | { tipo: 'bloqueada'; mensaje: TextoVivo };

/** Lo que se edita de una oferta propia: el contenido de una de sus versiones (DC-50). */
interface Edicion {
  readonly tipo: TipoOferta;
  readonly contenido: ContenidoVersion;
  /** El aviso que se muestra arriba del formulario según de qué versión parte. */
  readonly aviso: AvisoDeEdicion | null;
  readonly hayPublicada: boolean;
}

/** Qué se le dice al prestador al editar: de qué versión parte el formulario y qué pasa al guardar. */
interface AvisoDeEdicion {
  readonly tono: 'aviso' | 'error';
  readonly texto: TextoVivo;
}

/** Lo que hace falta para mostrar el formulario; `bloqueo` explica por qué no se puede editar. */
interface Carga {
  readonly zonas: Zona[];
  readonly propias: OfertaPrestador[];
  readonly edicion?: Edicion;
  /** Por qué no se puede mostrar el formulario; se arma al mostrarlo para que siga el idioma. */
  readonly bloqueo?: TextoVivo;
}

/** Una actividad propia que se puede incluir en un paquete. */
interface ActividadPropia {
  readonly id: string;
  readonly nombre: string;
  readonly sinAprobar: boolean;
}

const MINIMO_ACTIVIDADES = 2;

/** Sin tipo de prestador no se sabe qué ofertas puede publicar (DC-10): primero debe registrar su RNT. */
const BLOQUEO_SIN_TIPO = textoVivo('prestador.form.sinTipo');
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
  imports: [ReactiveFormsModule, RouterLink, Mapa, Icono, SelectorFotos, TPipe],
  templateUrl: './formulario-oferta.html',
  styleUrl: './formulario-oferta.css',
})
export class FormularioOferta {
  /** Llega de la ruta /prestador/ofertas/:id/editar; no existe al publicar una oferta nueva. */
  readonly id = input<string>();

  private readonly catalogo = inject(CatalogoService);
  private readonly servicio = inject(OfertasPrestadorService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  /** El tipo viene en el token de la sesión y solo lo tiene el prestador que ya registró su RNT. */
  private readonly tipoPrestador = this.sesion.tipoPrestador();
  private readonly esGuia = this.tipoPrestador === 'GUIA_INDEPENDIENTE';

  protected readonly tiposPermitidos: readonly TipoOferta[] = this.tipoPrestador
    ? TIPOS_QUE_PUBLICA[this.tipoPrestador]
    : [];
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
    tipo: new FormControl<TipoOferta>(this.tiposPermitidos[0] ?? 'ACTIVIDAD', { nonNullable: true }),
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
  /** Las fotos de la oferta, en orden; la primera es la portada (DC-47). Al editar parten de las publicadas. */
  protected readonly fotos = signal<Foto[]>([]);
  /** Verdadero mientras el selector sube alguna foto: no se envía la oferta hasta que termine. */
  protected readonly subiendoFotos = signal(false);
  /** Se activa al intentar enviar; desde entonces la falta de fotos se muestra como error. */
  private readonly intentoDeEnvio = signal(false);
  /** Error sobre `fotos` que devolvió la API; se quita al cambiar las fotos. */
  private readonly errorDeFotosApi = signal<TextoVivo | null>(null);
  /** Al editar, el aviso de qué versión se está corrigiendo; es null si el formulario parte de la publicada sin cambios pendientes. */
  protected readonly avisoDeEdicion = signal<AvisoDeEdicion | null>(null);
  /** Si la oferta ya tiene una versión publicada, que sigue visible mientras se revisan los cambios. */
  protected readonly hayPublicada = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  /** Errores de campo que devolvió la API, por nombre de campo. */
  private readonly erroresDeApi = signal<Partial<Record<Campo, TextoVivo>>>({});
  /** Cambia con cada edición del formulario, para que los mensajes de error se vuelvan a calcular. */
  private readonly cambios = signal(0);

  protected readonly tipo = toSignal(
    this.formulario.controls.tipo.valueChanges.pipe(startWith(this.formulario.controls.tipo.value)),
    { initialValue: this.formulario.controls.tipo.value },
  );
  protected readonly editando = computed(() => !!this.id());
  protected readonly errorDeFotos = computed(
    () =>
      this.errorDeFotosApi()?.() ??
      (this.intentoDeEnvio() && this.faltanFotos() ? traducir('prestador.form.faltaFoto') : null),
  );
  protected readonly mensajeDeBloqueo = computed(() => {
    const estado = this.estado();
    return estado.tipo === 'bloqueada' ? estado.mensaje() : '';
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
              of<Carga>({ zonas: [], propias: [], bloqueo: mensajeDeError(error) }),
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
      return deApi();
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

  protected cambiarFotos(fotos: Foto[]): void {
    this.fotos.set(fotos);
    this.errorDeFotosApi.set(null);
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
    this.errorDeFotosApi.set(null);
    this.intentoDeEnvio.set(true);
    if (this.formulario.invalid || this.faltanFotos()) {
      this.formulario.markAllAsTouched();
      this.cambios.update((cambios) => cambios + 1);
      if (this.formulario.invalid) {
        this.enfocar(this.primerCampoInvalido());
      } else {
        this.enfocarFotos();
      }
      return;
    }
    if (this.subiendoFotos()) {
      this.errorGeneral.set(textoVivo('prestador.form.esperaFotos'));
      this.enfocarFotos();
      return;
    }

    const id = this.id();
    const peticion = aPeticion(
      this.formulario.getRawValue(),
      this.ubicacion(),
      this.fotos().map((foto) => foto.id),
    );
    this.guardando.set(true);
    (id ? this.servicio.editar(id, peticion) : this.servicio.publicar(peticion)).subscribe({
      next: (oferta) => {
        this.guardando.set(false);
        const numero = oferta.pendiente?.numero;
        this.avisos.mostrar(
          numero
            ? traducir('prestador.form.enviadaConVersion', { numero })
            : traducir('prestador.form.enviada'),
        );
        void this.router.navigate(['/prestador/ofertas']);
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.mostrarError(fallo);
      },
    });
  }

  /** Zonas, ofertas propias (para las actividades de un paquete) y, al editar, el contenido de la oferta (DC-50). */
  private cargar(id: string | undefined): Observable<Carga> {
    if (!this.tipoPrestador) {
      return of({ zonas: [], propias: [], bloqueo: BLOQUEO_SIN_TIPO });
    }
    return forkJoin({ zonas: this.catalogo.zonas(), propias: this.servicio.todas() }).pipe(
      switchMap((base): Observable<Carga> => {
        if (!id) {
          return of(base);
        }
        return this.servicio.consultar(id).pipe(
          map((propia): Carga => conEdicion(base, propia)),
          catchError((error: unknown) => {
            const fallo = aErrorApi(error);
            // La oferta de otro prestador responde 403; una que no existe, 404: para quien edita es lo mismo.
            const ajena = fallo.estado === 403 || fallo.noEncontrado;
            return of<Carga>({ ...base, bloqueo: ajena ? textoVivo('prestador.form.noPropia') : () => fallo.mensaje });
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
          // Un getter: el texto de respaldo sigue el idioma aunque la lista ya esté en pantalla.
          get nombre() {
            return versionVigente(oferta)?.nombre ?? traducir('prestador.form.actividadSinNombre');
          },
          sinAprobar: !oferta.publicada,
        })),
    );
    if (carga.edicion) {
      const { valores, ubicacion, fotos } = valoresDeContenido(carga.edicion.tipo, carga.edicion.contenido);
      const disponibles = new Set(this.actividades().map((actividad) => actividad.id));
      this.formulario.setValue({
        ...valores,
        actividadIds: valores.actividadIds.filter((actividadId) => disponibles.has(actividadId)),
      });
      this.ubicacion.set(ubicacion);
      this.fotos.set(fotos);
    }
    this.avisoDeEdicion.set(carga.edicion?.aviso ?? null);
    this.hayPublicada.set(carga.edicion?.hayPublicada ?? false);
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
    if (campo === 'fotos') {
      // Por ejemplo una foto que ya se borró porque pasaron 24 horas sin usarla (DC-47).
      this.errorDeFotosApi.set(() => mayusculaInicial(error.detalleCampo ?? error.mensaje));
      this.enfocarFotos();
      return;
    }
    if (campo && esCampo(campo, this.formulario.controls)) {
      this.erroresDeApi.set({ [campo]: () => mayusculaInicial(error.detalleCampo ?? error.mensaje) });
      this.enfocar(campo);
      return;
    }
    this.errorGeneral.set(() =>
      error.detalleCampo
        ? traducir('prestador.form.conDetalle', { mensaje: error.mensaje, detalle: error.detalleCampo })
        : error.mensaje,
    );
  }

  private primerCampoInvalido(): Campo | undefined {
    const controles = this.formulario.controls;
    return (Object.keys(controles) as Campo[]).find((campo) => controles[campo].invalid);
  }

  /** Falta alguna foto, pero no cuenta mientras hay subidas en curso: esa foto todavía va a llegar. */
  private faltanFotos(): boolean {
    return this.fotos().length < MINIMO_FOTOS && !this.subiendoFotos();
  }

  private enfocarFotos(): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>('#fotos')?.focus();
  }

  /** Lleva el foco al campo con error; los campos tienen como id el nombre de su control. */
  private enfocar(campo: Campo | undefined): void {
    if (campo) {
      this.anfitrion.nativeElement.querySelector<HTMLElement>(`#${campo}`)?.focus();
    }
  }
}

/**
 * Qué versión se edita y qué se le avisa al prestador. Parte de la pendiente si existe —una en moderación o una rechazada, que es
 * la que hay que corregir— y si no, de la publicada. Guardar crea una versión nueva que reemplaza a la pendiente; la publicada sigue
 * visible hasta que se apruebe la nueva.
 */
function conEdicion(base: Carga, propia: OfertaPropia): Carga {
  if (propia.retirada) {
    return { ...base, bloqueo: () => MENSAJES.OFERTA_NO_EDITABLE };
  }
  const version = propia.pendiente ?? propia.publicada;
  if (!version) {
    return { ...base, bloqueo: textoVivo('prestador.form.noPropia') };
  }
  let aviso: AvisoDeEdicion | null = null;
  if (propia.pendiente?.estadoModeracion === 'RECHAZADA') {
    const { numero, motivoRechazo } = propia.pendiente;
    aviso = {
      tono: 'error',
      texto: () =>
        traducir('prestador.form.rechazada', {
          numero,
          motivo: motivoRechazo ?? traducir('prestador.ofertas.sinMotivo'),
        }),
    };
  } else if (propia.pendiente) {
    const numero = propia.pendiente.numero;
    aviso = {
      tono: 'aviso',
      texto: textoVivo(propia.publicada ? 'prestador.form.cambiosEnRevision' : 'prestador.form.primeraEnRevision', { numero }),
    };
  }
  return {
    ...base,
    edicion: { tipo: propia.tipo, contenido: version.contenido, aviso, hayPublicada: !!propia.publicada },
  };
}

function esCampo(nombre: string, controles: Record<Campo, AbstractControl>): nombre is Campo {
  return nombre in controles;
}

/** Un error de la API como texto que se arma al mostrarlo. */
function mensajeDeError(error: unknown): TextoVivo {
  const fallo = aErrorApi(error);
  return () => fallo.mensaje;
}

function mayusculaInicial(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function mensajeDe(errores: ValidationErrors | null): string | null {
  if (!errores) {
    return null;
  }
  if (errores['required']) {
    return traducir('prestador.form.errorObligatorio');
  }
  if (errores['min']) {
    return traducir('prestador.form.errorMinimo', { minimo: errores['min'].min });
  }
  if (errores['entero']) {
    return traducir('prestador.form.errorEntero');
  }
  if (errores['horas']) {
    return traducir('prestador.form.errorHoras');
  }
  if (errores['idiomas']) {
    return traducir('prestador.form.errorIdiomas');
  }
  if (errores['actividades']) {
    return traducir('prestador.form.errorActividades', { minimo: MINIMO_ACTIVIDADES });
  }
  return traducir('prestador.form.errorRevisa');
}
