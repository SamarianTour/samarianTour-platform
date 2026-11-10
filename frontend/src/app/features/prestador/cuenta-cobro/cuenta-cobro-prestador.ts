import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap, tap } from 'rxjs';
import { CuentaCobro, TipoCuentaCobro } from '../../../core/api/modelos-nucleo';
import { PrestadorService } from '../../../core/api/prestador.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import {
  BANCOS_SUGERIDOS,
  CAMPOS_CUENTA_EN_ORDEN,
  CampoCuenta,
  MotivoCuenta,
  TIPOS_DE_CUENTA,
  TIPO_CUENTA_COBRO,
  VALORES_CUENTA_VACIOS,
  ValoresCuenta,
  construirCuenta,
  esBilletera,
  pideEntidad,
  ultimosDigitos,
  validarCuenta,
  valoresDeCuenta,
} from './cuenta-cobro';

const CLAVE_DE_ERROR: Readonly<Record<MotivoCuenta, ClaveTexto>> = {
  tipoRequerido: 'prestador.cobro.error.tipoRequerido',
  entidadRequerida: 'prestador.cobro.error.entidadRequerida',
  entidadLarga: 'prestador.cobro.error.entidadLarga',
  numeroRequerido: 'prestador.cobro.error.numeroRequerido',
  numeroBancoInvalido: 'prestador.cobro.error.numeroBancoInvalido',
  numeroCelularInvalido: 'prestador.cobro.error.numeroCelularInvalido',
  titularRequerido: 'prestador.cobro.error.titularRequerido',
  titularLargo: 'prestador.cobro.error.titularLargo',
};

const CAMPOS_DE_API: ReadonlySet<string> = new Set<CampoCuenta>(CAMPOS_CUENTA_EN_ORDEN);

type Cargado = { cuenta: CuentaCobro | null } | { error: ErrorApi };

/**
 * P03.5 Cuenta de cobro (HU-PRE-05, RF-74, RF-75). El prestador registra una vez la cuenta bancaria o la billetera donde recibe lo que
 * le corresponde por cada pago; la comisión se descuenta de ahí y nunca se suma al turista.
 *
 * El número completo es un dato sensible (RNF-08): solo se escribe. El núcleo lo guarda cifrado y devuelve solo los últimos 4 dígitos,
 * que es lo único que se muestra después. El campo no se autocompleta, no se guarda en el navegador (ni en `localStorage`, ni en la
 * dirección), se vacía al guardar y ningún mensaje de error ni aviso lo repite: los de la API se reemplazan por un texto fijo.
 */
@Component({
  selector: 'app-cuenta-cobro-prestador',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe, FechaPipe],
  templateUrl: './cuenta-cobro-prestador.html',
  styleUrl: './cuenta-cobro-prestador.css',
})
export class CuentaCobroPrestador {
  private readonly servicio = inject(PrestadorService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la cuenta de cobro. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);

  protected readonly tipos = TIPOS_DE_CUENTA;
  protected readonly etiquetaTipo = TIPO_CUENTA_COBRO;
  protected readonly bancos = BANCOS_SUGERIDOS;
  protected readonly ultimos = ultimosDigitos;
  protected readonly esBanco = pideEntidad;

  // --- La cuenta vigente ---

  protected readonly cargando = signal(true);
  /** `undefined` mientras no se ha leído; `null` si todavía no registró ninguna. */
  protected readonly cuenta = signal<CuentaCobro | null | undefined>(undefined);
  protected readonly errorCarga = signal<ErrorApi | null>(null);
  /** Sin RNT registrado el núcleo todavía no tiene al prestador, así que no hay dónde guardar la cuenta. */
  protected readonly sinRnt = signal(false);

  // --- El formulario ---

  protected readonly formulario = new FormGroup({
    tipo: new FormControl<TipoCuentaCobro | ''>('', { nonNullable: true }),
    entidad: new FormControl('', { nonNullable: true }),
    numero: new FormControl('', { nonNullable: true }),
    titular: new FormControl('', { nonNullable: true }),
  });
  private readonly valores = toSignal(
    this.formulario.valueChanges.pipe(startWith(null), map((): ValoresCuenta => this.formulario.getRawValue())),
    { initialValue: this.formulario.getRawValue() },
  );
  protected readonly tipoElegido = computed(() => !!this.valores().tipo);
  protected readonly billetera = computed(() => esBilletera(this.valores().tipo));
  protected readonly banco = computed(() => pideEntidad(this.valores().tipo));
  private readonly erroresLocales = computed(() => validarCuenta(this.valores()));
  private readonly intentado = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  private readonly erroresDeApi = signal<Partial<Record<CampoCuenta, MotivoCuenta>>>({});

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.servicio.cuentaCobro().pipe(
            map((cuenta): Cargado => ({ cuenta })),
            catchError((fallo: unknown) => of<Cargado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        if ('error' in resultado) {
          if (resultado.error.codigo === 'PRESTADOR_NO_REGISTRADO') {
            this.sinRnt.set(true);
            this.cuenta.set(null);
          } else {
            this.errorCarga.set(resultado.error);
          }
          return;
        }
        this.errorCarga.set(null);
        this.sinRnt.set(false);
        this.cuenta.set(resultado.cuenta);
        if (resultado.cuenta && !this.formulario.dirty) {
          this.formulario.reset(valoresDeCuenta(resultado.cuenta));
        }
      });

    // Al corregir un dato desaparece el error que la API había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.erroresDeApi.set({}));
  }

  /** El error de un campo: primero el de la API (siempre con un texto fijo); si no, el local una vez tocado o al enviar. */
  protected errorDe(campo: CampoCuenta): string | null {
    const motivo = this.erroresDeApi()[campo] ?? this.erroresLocales()[campo];
    if (!motivo) {
      return null;
    }
    const mostrar = this.erroresDeApi()[campo] || this.intentado() || this.formulario.controls[campo].touched;
    return mostrar ? traducir(CLAVE_DE_ERROR[motivo]) : null;
  }

  protected guardar(): void {
    if (this.guardando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    this.intentado.set(true);

    const errores = this.erroresLocales();
    const primero = CAMPOS_CUENTA_EN_ORDEN.find((campo) => errores[campo]);
    if (primero) {
      this.enfocar(primero);
      return;
    }

    this.guardando.set(true);
    this.servicio.registrarCuentaCobro(construirCuenta(this.formulario.getRawValue())).subscribe({
      next: (guardada) => {
        this.guardando.set(false);
        this.intentado.set(false);
        this.sinRnt.set(false);
        this.cuenta.set(guardada);
        // El número se borra: de aquí en adelante solo existe enmascarado.
        this.formulario.reset(valoresDeCuenta(guardada));
        this.avisos.mostrar(traducir('prestador.cobro.guardada'));
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.mostrarError(aErrorApi(fallo));
      },
    });
  }

  /**
   * Un error del núcleo no repite lo que se escribió: el detalle de un campo puede traer el valor, y el número de la cuenta no debe
   * aparecer en ningún mensaje. Se muestra el texto fijo del campo.
   */
  private mostrarError(error: ErrorApi): void {
    const campo = error.campo as CampoCuenta | undefined;
    if (campo && CAMPOS_DE_API.has(campo)) {
      this.erroresDeApi.set({ [campo]: this.motivoFijo(campo) });
      this.enfocar(campo);
      return;
    }
    if (error.codigo === 'PRESTADOR_NO_REGISTRADO') {
      this.sinRnt.set(true);
      return;
    }
    this.errorGeneral.set(() => error.mensaje);
  }

  private motivoFijo(campo: CampoCuenta): MotivoCuenta {
    switch (campo) {
      case 'tipo':
        return 'tipoRequerido';
      case 'entidad':
        return 'entidadRequerida';
      case 'titular':
        return 'titularRequerido';
      case 'numero':
        return this.billetera() ? 'numeroCelularInvalido' : 'numeroBancoInvalido';
    }
  }

  private enfocar(campo: CampoCuenta): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#cobro-${campo}`)?.focus();
  }
}
