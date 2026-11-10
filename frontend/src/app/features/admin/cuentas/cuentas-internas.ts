import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap, tap } from 'rxjs';
import { AdminService } from '../../../core/api/admin.service';
import { CuentaInterna } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { DialogoConfirmacion } from '../../../shared/dialogo-confirmacion/dialogo-confirmacion';
import { ROL } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import {
  CLASE_DE_ESTADO_CUENTA,
  CORREO_MAXIMO,
  CampoCuenta,
  ETIQUETA_ESTADO_CUENTA,
  FILTROS_CUENTAS,
  FiltroCuentas,
  MotivoCuenta,
  NOMBRE_MAXIMO,
  ROLES_CREABLES,
  ValoresCuenta,
  accionDeCuenta,
  construirCuenta,
  filtrarCuentas,
  filtroDeCuentas,
  validarCuenta,
} from '../cuentas';

const CLAVE_DE_ERROR: Readonly<Record<MotivoCuenta, ClaveTexto>> = {
  rolRequerido: 'admin.cuentas.error.rolRequerido',
  nombreRequerido: 'admin.cuentas.error.nombreRequerido',
  nombreLargo: 'admin.cuentas.error.nombreLargo',
  correoRequerido: 'admin.cuentas.error.correoRequerido',
  correoFormato: 'admin.cuentas.error.correoFormato',
  correoLargo: 'admin.cuentas.error.correoLargo',
};

const CLAVE_DEL_FILTRO: Readonly<Record<FiltroCuentas, ClaveTexto>> = {
  todas: 'admin.cuentas.filtro.todas',
  OPERADOR_DISPONIBILIDAD: 'admin.cuentas.filtro.OPERADOR_DISPONIBILIDAD',
  GESTOR_DESTINO: 'admin.cuentas.filtro.GESTOR_DESTINO',
  ADMINISTRADOR: 'admin.cuentas.filtro.ADMINISTRADOR',
};

/**
 * P05.3 Cuentas internas (RF-81). Lista las cuentas de operadores, gestores y administradores, y deja crear, desactivar y reactivar
 * las de operador y gestor. Las de administrador solo se consultan: no se crean ni se desactivan desde el panel. Una cuenta nueva
 * nace activa y sin contraseña; el núcleo le manda un correo con el enlace para ponerla (72 horas, un solo uso).
 */
@Component({
  selector: 'app-cuentas-internas',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe, FechaPipe, DialogoConfirmacion],
  templateUrl: './cuentas-internas.html',
  styleUrl: './cuentas-internas.css',
})
export class CuentasInternas {
  private readonly ruta = inject(ActivatedRoute);
  private readonly admin = inject(AdminService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly dialogo = viewChild.required(DialogoConfirmacion);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la lista. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly etiquetaRol = ROL;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_CUENTA;
  protected readonly claseDeEstado = CLASE_DE_ESTADO_CUENTA;
  protected readonly rolesCreables = ROLES_CREABLES;
  protected readonly filtros = FILTROS_CUENTAS;
  protected readonly claveDelFiltro = CLAVE_DEL_FILTRO;
  protected readonly nombreMaximo = NOMBRE_MAXIMO;
  protected readonly correoMaximo = CORREO_MAXIMO;

  // --- La lista ---

  protected readonly filtro = signal<FiltroCuentas>('todas');
  protected readonly cargando = signal(true);
  protected readonly cuentas = signal<CuentaInterna[] | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  protected readonly miId = computed(() => this.sesion.cuenta()?.id);

  /** El operador o gestor cuya baja se está confirmando. */
  protected readonly porDesactivar = signal<CuentaInterna | null>(null);
  /** La cuenta a la que se le está aplicando una acción. */
  protected readonly accionEn = signal<string | null>(null);
  private readonly erroresDeFila = signal<Readonly<Record<string, TextoVivo>>>({});
  /** Las filas cuyo error es que el operador todavía tiene prestadores: allí se ofrece ir a reasignarlos. */
  private readonly conPrestadores = signal<ReadonlySet<string>>(new Set());

  protected readonly filas = computed(() =>
    filtrarCuentas(this.cuentas() ?? [], this.filtro()).map((cuenta) => ({
      cuenta,
      esMia: cuenta.id === this.miId(),
      accion: accionDeCuenta(cuenta),
      error: (this.erroresDeFila()[cuenta.id] as TextoVivo | undefined) ?? null,
      pidePrestadores: this.conPrestadores().has(cuenta.id),
    })),
  );

  // --- Crear una cuenta ---

  protected readonly formulario = new FormGroup({
    rol: new FormControl('', { nonNullable: true }),
    nombre: new FormControl('', { nonNullable: true }),
    correo: new FormControl('', { nonNullable: true }),
  });
  private readonly valores = toSignal(
    this.formulario.valueChanges.pipe(startWith(null), map((): ValoresCuenta => this.formulario.getRawValue())),
    { initialValue: this.formulario.getRawValue() },
  );
  private readonly erroresLocales = computed(() => validarCuenta(this.valores()));
  private readonly intentado = signal(false);
  protected readonly creando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  private readonly erroresDeApi = signal<Partial<Record<CampoCuenta, TextoVivo>>>({});
  protected readonly largoNombre = computed(() => this.valores().nombre.trim().length);

  constructor() {
    const filtro$ = this.ruta.queryParamMap.pipe(map((params) => filtroDeCuentas(params.get('rol'))));

    combineLatest([filtro$, toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(([filtro]) => {
          this.filtro.set(filtro);
          this.cargando.set(true);
        }),
        switchMap(() =>
          this.admin.cuentasInternas().pipe(
            map((cuentas): { cuentas: CuentaInterna[] } | { error: ErrorApi } => ({ cuentas })),
            catchError((fallo: unknown) => of({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('cuentas' in resultado) {
          this.cuentas.set(resultado.cuentas);
        }
        this.error.set('error' in resultado ? resultado.error : null);
      });

    // Al corregir un dato desaparece el error que la API había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.erroresDeApi.set({}));

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  // --- Crear ---

  /** El mensaje de error de un campo: primero el de la API; si no, el de la validación local una vez tocado o al enviar. */
  protected errorDe(campo: CampoCuenta): string | null {
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi();
    }
    const motivo = this.erroresLocales()[campo];
    if (!motivo || !(this.intentado() || this.formulario.controls[campo].touched)) {
      return null;
    }
    return traducir(CLAVE_DE_ERROR[motivo], { maximo: campo === 'nombre' ? NOMBRE_MAXIMO : CORREO_MAXIMO });
  }

  protected crear(): void {
    if (this.creando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    this.intentado.set(true);

    const errores = this.erroresLocales();
    const primero = (['rol', 'nombre', 'correo'] as const).find((campo) => errores[campo]);
    if (primero) {
      this.enfocar(primero);
      return;
    }

    const cuenta = construirCuenta(this.formulario.getRawValue());
    this.creando.set(true);
    this.admin.crearCuentaInterna(cuenta).subscribe({
      next: (creada) => {
        this.creando.set(false);
        this.cuentas.update((lista) => (lista === null ? lista : [...lista, creada]));
        this.formulario.reset({ rol: '', nombre: '', correo: '' });
        this.intentado.set(false);
        this.avisos.mostrar(traducir('admin.cuentas.creada', { nombre: creada.nombre }));
      },
      error: (fallo: unknown) => {
        this.creando.set(false);
        this.mostrarErrorDeCreacion(aErrorApi(fallo));
      },
    });
  }

  private mostrarErrorDeCreacion(error: ErrorApi): void {
    if (error.codigo === 'CORREO_YA_REGISTRADO') {
      this.erroresDeApi.set({ correo: () => traducir('admin.cuentas.error.correoRepetido') });
      this.enfocar('correo');
      return;
    }
    const campo = (['rol', 'nombre', 'correo'] as const).find((nombre) => nombre === error.campo);
    if (campo) {
      this.erroresDeApi.set({ [campo]: () => error.detalleCampo ?? error.mensaje });
      this.enfocar(campo);
      return;
    }
    this.errorGeneral.set(() => error.mensaje);
  }

  /** Lleva el foco al campo con error; los campos tienen como id el nombre de su control. */
  private enfocar(campo: CampoCuenta): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#cuenta-${campo}`)?.focus();
  }

  // --- Desactivar y reactivar ---

  protected pedirDesactivar(cuenta: CuentaInterna): void {
    if (this.accionEn() !== null) {
      return;
    }
    this.porDesactivar.set(cuenta);
    this.dialogo().abrir();
  }

  protected desactivar(): void {
    const cuenta = this.porDesactivar();
    if (cuenta) {
      this.aplicar(cuenta, this.admin.desactivarCuentaInterna(cuenta.id), 'admin.cuentas.desactivada');
    }
  }

  protected reactivar(cuenta: CuentaInterna): void {
    this.aplicar(
      cuenta,
      this.admin.reactivarCuentaInterna(cuenta.id),
      cuenta.rol === 'OPERADOR_DISPONIBILIDAD' ? 'admin.cuentas.reactivadaOperador' : 'admin.cuentas.reactivada',
    );
  }

  private aplicar(
    cuenta: CuentaInterna,
    peticion: ReturnType<AdminService['desactivarCuentaInterna']>,
    avisoOk: ClaveTexto,
  ): void {
    if (this.accionEn() !== null) {
      return;
    }
    this.accionEn.set(cuenta.id);
    this.erroresDeFila.update((errores) => sinClave(errores, cuenta.id));
    this.conPrestadores.update((conjunto) => new Set([...conjunto].filter((id) => id !== cuenta.id)));
    peticion.subscribe({
      next: (actualizada) => {
        this.accionEn.set(null);
        this.cuentas.update((lista) =>
          lista === null ? lista : lista.map((otra) => (otra.id === actualizada.id ? actualizada : otra)),
        );
        this.avisos.mostrar(traducir(avisoOk, { nombre: cuenta.nombre }));
      },
      error: (fallo: unknown) => {
        this.accionEn.set(null);
        const problema = aErrorApi(fallo);
        this.erroresDeFila.update((errores) => ({ ...errores, [cuenta.id]: () => problema.mensaje }));
        if (problema.codigo === 'OPERADOR_CON_PRESTADORES') {
          this.conPrestadores.update((conjunto) => new Set([...conjunto, cuenta.id]));
        } else if (problema.codigo === 'CUENTA_NO_ENCONTRADA' || problema.codigo === 'CONFLICTO_CONCURRENCIA') {
          // La cuenta cambió por su lado: se vuelve a leer la lista.
          this.recarga.next();
        }
      },
    });
  }
}

/** Una copia del registro sin esa clave. */
function sinClave<T>(registro: Readonly<Record<string, T>>, clave: string): Record<string, T> {
  const copia = { ...registro };
  delete copia[clave];
  return copia;
}
