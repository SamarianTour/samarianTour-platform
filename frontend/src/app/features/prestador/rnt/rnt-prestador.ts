import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap, tap } from 'rxjs';
import { ModalidadRecorrido, TipoPrestador } from '../../../core/api/modelos';
import { PrestadorPropio } from '../../../core/api/modelos-nucleo';
import { PrestadorService } from '../../../core/api/prestador.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { MODALIDAD, TIPO_PRESTADOR } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import {
  CLASE_SITUACION_RNT,
  CLAVE_SITUACION_RNT,
  avisoDeRnt,
  situacionRnt,
} from '../inicio/inicio';
import {
  CAMPOS_EN_ORDEN,
  CATEGORIA_PRESTADOR,
  CLAVE_RESULTADO_RNT,
  CampoRnt,
  MODALIDADES_DEL_GUIA,
  MotivoRnt,
  TIPOS_DE_PRESTADOR,
  ValoresRegistro,
  construirRegistro,
  construirTipo,
  esGuia,
  valoresDeTipo,
  validarNumeroRnt,
  validarRegistro,
  validarTipo,
} from './rnt';

const CLAVE_DE_ERROR: Readonly<Record<MotivoRnt, ClaveTexto>> = {
  numeroRequerido: 'prestador.rnt.error.numeroRequerido',
  numeroInvalido: 'prestador.rnt.error.numeroInvalido',
  tipoRequerido: 'prestador.rnt.error.tipoRequerido',
  empleadosRequerido: 'prestador.rnt.error.empleadosRequerido',
  empleadosRango: 'prestador.rnt.error.empleadosRango',
  modalidadesRequerida: 'prestador.rnt.error.modalidadesRequerida',
  idiomasRequerido: 'prestador.rnt.error.idiomasRequerido',
  idiomasInvalidos: 'prestador.rnt.error.idiomasInvalidos',
};

/** Los campos que la API puede señalar en un DATO_INVALIDO, con el campo de la pantalla al que corresponden. */
const CAMPO_DE_API: Readonly<Record<string, CampoRnt>> = {
  numeroRnt: 'numeroRnt',
  tipo: 'tipo',
  numeroEmpleados: 'numeroEmpleados',
  modalidades: 'modalidades',
  idiomas: 'idiomas',
};

type Cargado = { prestador: PrestadorPropio | null } | { error: ErrorApi };

/**
 * P03.1 RNT y tipo de prestador (HU-PRE-02). Sin registro, pide el RNT y el tipo (`POST /prestador/registro`); con registro, muestra
 * cómo quedó la validación contra el registro oficial y deja cambiar el número (`PUT /prestador/rnt`) y el tipo (`PUT /prestador/tipo`).
 * La validación puede tardar unos segundos: mientras responde el botón queda deshabilitado y la petición no tiene tiempo máximo.
 * Tras registrar o cambiar el tipo se renueva la sesión, porque el tipo viaja en el token (RF-68).
 */
@Component({
  selector: 'app-rnt-prestador',
  imports: [ReactiveFormsModule, Icono, TPipe, FechaPipe],
  templateUrl: './rnt-prestador.html',
  styleUrl: './rnt-prestador.css',
})
export class RntPrestador {
  private readonly servicio = inject(PrestadorService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir el registro. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);

  protected readonly tipos = TIPOS_DE_PRESTADOR;
  protected readonly modalidades = MODALIDADES_DEL_GUIA;
  protected readonly etiquetaTipo = TIPO_PRESTADOR;
  protected readonly etiquetaModalidad = MODALIDAD;
  protected readonly etiquetaCategoria = CATEGORIA_PRESTADOR;
  protected readonly claveRnt = CLAVE_SITUACION_RNT;
  protected readonly claseRnt = CLASE_SITUACION_RNT;

  // --- El registro ---

  protected readonly cargando = signal(true);
  /** `undefined` mientras no se ha leído; `null` si todavía no registró el RNT. */
  protected readonly prestador = signal<PrestadorPropio | null | undefined>(undefined);
  protected readonly errorCarga = signal<ErrorApi | null>(null);
  protected readonly registrado = computed(() => !!this.prestador());
  protected readonly situacion = computed(() => {
    const prestador = this.prestador();
    return prestador === undefined ? null : situacionRnt(prestador);
  });
  /** Lo que se le explica cuando el RNT no está activo (el mismo texto del dashboard, sin el enlace a esta pantalla). */
  protected readonly aviso = computed(() => {
    const situacion = this.situacion();
    return situacion && situacion !== 'SIN_REGISTRAR' ? avisoDeRnt(situacion) : null;
  });

  // --- Registrar el RNT y el tipo, o cambiar el tipo ---

  protected readonly formulario = new FormGroup({
    numeroRnt: new FormControl('', { nonNullable: true }),
    tipo: new FormControl<TipoPrestador | ''>('', { nonNullable: true }),
    numeroEmpleados: new FormControl<number | null>(null),
    modalidades: new FormControl<ModalidadRecorrido[]>([], { nonNullable: true }),
    idiomas: new FormControl('', { nonNullable: true }),
  });
  private readonly valores = toSignal(
    this.formulario.valueChanges.pipe(startWith(null), map((): ValoresRegistro => this.formulario.getRawValue())),
    { initialValue: this.formulario.getRawValue() },
  );
  protected readonly tipoElegido = computed(() => !!this.valores().tipo);
  protected readonly guia = computed(() => esGuia(this.valores().tipo));
  private readonly erroresLocales = computed(() =>
    this.registrado() ? validarTipo(this.valores()) : validarRegistro(this.valores()),
  );
  private readonly intentado = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  private readonly erroresDeApi = signal<Partial<Record<CampoRnt, TextoVivo>>>({});
  /** Verdadero si se guardó pero no se pudo renovar la sesión: el tipo del menú no se actualiza hasta volver a entrar. */
  protected readonly sesionSinActualizar = signal(false);

  // --- Cambiar el número de RNT ---

  protected readonly grupoNumero = new FormGroup({ numeroRnt: new FormControl('', { nonNullable: true }) });
  private readonly numeroEscrito = toSignal(
    this.grupoNumero.controls.numeroRnt.valueChanges.pipe(startWith(this.grupoNumero.controls.numeroRnt.value)),
    { initialValue: '' },
  );
  private readonly intentoNumero = signal(false);
  protected readonly enviandoNumero = signal(false);
  protected readonly errorNumero = signal<TextoVivo | null>(null);

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.servicio.registro().pipe(
            map((prestador): Cargado => ({ prestador })),
            catchError((fallo: unknown) => of<Cargado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        if ('error' in resultado) {
          this.errorCarga.set(resultado.error);
          return;
        }
        this.errorCarga.set(null);
        this.prestador.set(resultado.prestador);
        if (resultado.prestador && !this.formulario.dirty) {
          this.llenarTipo(resultado.prestador);
        }
      });

    // Al corregir un dato desaparece el error que la API había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.erroresDeApi.set({}));
    this.grupoNumero.controls.numeroRnt.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.errorNumero.set(null));
  }

  // --- Los campos ---

  /** El error de un campo: primero el que dijo la API; si no, el de la validación local una vez tocado o al enviar. */
  protected errorDe(campo: CampoRnt): string | null {
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi();
    }
    const motivo = this.erroresLocales()[campo];
    if (!motivo || !(this.intentado() || this.tocado(campo))) {
      return null;
    }
    return traducir(CLAVE_DE_ERROR[motivo]);
  }

  private tocado(campo: CampoRnt): boolean {
    return campo === 'modalidades' ? false : this.formulario.controls[campo].touched;
  }

  protected errorDelNumero(): string | null {
    const motivo = validarNumeroRnt(this.numeroEscrito());
    const mostrar = this.intentoNumero() || this.grupoNumero.controls.numeroRnt.touched;
    if (this.errorNumero()) {
      return this.errorNumero()!();
    }
    return motivo && mostrar ? traducir(CLAVE_DE_ERROR[motivo]) : null;
  }

  protected modalidadMarcada(modalidad: ModalidadRecorrido): boolean {
    return this.valores().modalidades.includes(modalidad);
  }

  protected alternarModalidad(modalidad: ModalidadRecorrido, marcada: boolean): void {
    const control = this.formulario.controls.modalidades;
    const actuales = control.value.filter((otra) => otra !== modalidad);
    control.setValue(marcada ? [...actuales, modalidad] : actuales);
    control.markAsDirty();
  }

  private llenarTipo(prestador: PrestadorPropio): void {
    const tipo = valoresDeTipo(prestador);
    this.formulario.reset({
      numeroRnt: '',
      tipo: tipo.tipo,
      numeroEmpleados: tipo.numeroEmpleados,
      modalidades: [...tipo.modalidades],
      idiomas: tipo.idiomas,
    });
  }

  // --- Registrar o cambiar el tipo ---

  protected enviar(): void {
    if (this.enviando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    this.intentado.set(true);

    const errores = this.erroresLocales();
    const primero = CAMPOS_EN_ORDEN.find((campo) => errores[campo]);
    if (primero) {
      this.enfocar(primero);
      return;
    }

    const valores = this.formulario.getRawValue();
    const registrando = !this.registrado();
    this.enviando.set(true);
    const peticion = registrando
      ? this.servicio.registrar(construirRegistro(valores))
      : this.servicio.cambiarTipo(construirTipo(valores));
    peticion.subscribe({
      next: (guardado) => {
        this.enviando.set(false);
        this.intentado.set(false);
        this.prestador.set(guardado);
        this.llenarTipo(guardado);
        this.avisos.mostrar(
          traducir(registrando ? CLAVE_RESULTADO_RNT[guardado.estadoRnt] : 'prestador.rnt.tipoGuardado'),
        );
        this.actualizarSesion();
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        this.mostrarError(aErrorApi(fallo));
      },
    });
  }

  private mostrarError(error: ErrorApi): void {
    const campo = error.campo ? CAMPO_DE_API[error.campo] : undefined;
    if (campo) {
      this.erroresDeApi.set({ [campo]: () => error.detalleCampo ?? error.mensaje });
      this.enfocar(campo);
      return;
    }
    if (error.codigo === 'RNT_YA_REGISTRADO') {
      this.erroresDeApi.set({ numeroRnt: () => error.mensaje });
      this.enfocar('numeroRnt');
      return;
    }
    if (error.codigo === 'TIPO_NO_COINCIDE_CON_RNT') {
      this.erroresDeApi.set({ tipo: () => error.mensaje });
      this.enfocar('tipo');
      return;
    }
    this.errorGeneral.set(() => error.mensaje);
    if (error.codigo === 'PRESTADOR_YA_REGISTRADO' || error.codigo === 'PRESTADOR_NO_REGISTRADO') {
      // Otra pestaña ya registró (o todavía no existe): se vuelve a leer lo que hay.
      this.recarga.next();
    }
  }

  // --- Cambiar el número ---

  protected cambiarNumero(): void {
    if (this.enviandoNumero()) {
      return;
    }
    this.errorNumero.set(null);
    this.intentoNumero.set(true);
    const numero = this.grupoNumero.controls.numeroRnt.value;
    if (validarNumeroRnt(numero)) {
      this.anfitrion.nativeElement.querySelector<HTMLElement>('#rnt-numeroNuevo')?.focus();
      return;
    }
    this.enviandoNumero.set(true);
    this.servicio.cambiarRnt(numero.trim()).subscribe({
      next: (guardado) => {
        this.enviandoNumero.set(false);
        this.intentoNumero.set(false);
        this.prestador.set(guardado);
        this.grupoNumero.reset({ numeroRnt: '' });
        this.avisos.mostrar(traducir(CLAVE_RESULTADO_RNT[guardado.estadoRnt]));
      },
      error: (fallo: unknown) => {
        this.enviandoNumero.set(false);
        const error = aErrorApi(fallo);
        this.errorNumero.set(() => error.detalleCampo ?? error.mensaje);
        this.anfitrion.nativeElement.querySelector<HTMLElement>('#rnt-numeroNuevo')?.focus();
        if (error.codigo === 'PRESTADOR_NO_REGISTRADO') {
          this.recarga.next();
        }
      },
    });
  }

  // --- La sesión ---

  /** El tipo viaja en el token: se pide uno nuevo para que el menú y las ofertas lo vean al instante. */
  private actualizarSesion(): void {
    this.sesionSinActualizar.set(false);
    this.sesion.renovar().subscribe({ error: () => this.sesionSinActualizar.set(true) });
  }

  private enfocar(campo: CampoRnt): void {
    const raiz = this.anfitrion.nativeElement;
    const destino =
      campo === 'modalidades'
        ? raiz.querySelector<HTMLElement>('#rnt-modalidades input')
        : raiz.querySelector<HTMLElement>(`#rnt-${campo}`);
    destino?.focus();
  }
}
