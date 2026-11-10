import { DOCUMENT } from '@angular/common';
import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap, tap } from 'rxjs';
import { AdminCatalogoService } from '../../../core/api/admin-catalogo.service';
import { Zona } from '../../../core/api/modelos';
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
import { Icono } from '../../../shared/icono/icono';
import { NumeroPipe } from '../../../shared/numero.pipe';
import {
  CampoZona,
  FILTROS_ZONAS,
  FiltroZonas,
  MotivoZona,
  VALORES_ZONA_VACIOS,
  ValoresZona,
  construirEdicion,
  construirZona,
  filtrarZonas,
  filtroDeZonas,
  ordenarZonasPorNombre,
  peticionDesdeZona,
  validarZona,
  valoresDeZona,
} from '../zonas';

const CLAVE_DE_ERROR: Readonly<Record<MotivoZona, ClaveTexto>> = {
  nombreRequerido: 'admin.zonas.error.nombreRequerido',
  descripcionRequerida: 'admin.zonas.error.descripcionRequerida',
  latitudRequerida: 'admin.zonas.error.latitudRequerida',
  latitudRango: 'admin.zonas.error.latitudRango',
  longitudRequerida: 'admin.zonas.error.longitudRequerida',
  longitudRango: 'admin.zonas.error.longitudRango',
};

const CLAVE_DEL_FILTRO: Readonly<Record<FiltroZonas, ClaveTexto>> = {
  todas: 'admin.zonas.filtro.todas',
  activas: 'admin.zonas.filtro.activas',
  inactivas: 'admin.zonas.filtro.inactivas',
};

/** Los campos del formulario que el catálogo puede señalar en un DATO_INVALIDO (la ubicación se escribe en dos campos). */
const CAMPO_DE_API: Readonly<Record<string, CampoZona>> = {
  nombre: 'nombre',
  descripcion: 'descripcion',
  ubicacion: 'latitud',
  'ubicacion.latitud': 'latitud',
  'ubicacion.longitud': 'longitud',
};

type Cargado = { zonas: Zona[] } | { error: ErrorApi };

/**
 * P05.5 Zonas (HU-ADM-06, RF-84). El administrador escribe directo en el catálogo, que es el dueño de las zonas (DC-49): registra una,
 * edita sus datos y sus marcas (área protegida, alta afluencia), sus buenas prácticas y sus condiciones de visita, y la inactiva o la
 * reactiva. Una zona nunca se elimina. Editar **reemplaza todos los datos**, por eso inactivar y reactivar desde la lista mandan la zona
 * completa con solo `activa` cambiada, para no borrar nada. Una zona inactiva sale de la lista pública y no admite ofertas nuevas ni
 * ediciones, pero sus ofertas siguen visibles.
 */
@Component({
  selector: 'app-zonas-admin',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe, NumeroPipe, DialogoConfirmacion],
  templateUrl: './zonas-admin.html',
  styleUrl: './zonas-admin.css',
})
export class ZonasAdmin {
  private readonly ruta = inject(ActivatedRoute);
  private readonly catalogo = inject(AdminCatalogoService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly documento = inject(DOCUMENT);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly dialogo = viewChild.required(DialogoConfirmacion);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir las zonas. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly filtros = FILTROS_ZONAS;
  protected readonly claveDelFiltro = CLAVE_DEL_FILTRO;

  // --- La lista ---

  protected readonly filtro = signal<FiltroZonas>('todas');
  protected readonly cargando = signal(true);
  protected readonly zonas = signal<Zona[] | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  protected readonly visibles = computed(() => filtrarZonas(this.zonas() ?? [], this.filtro()));

  /** La zona cuya baja se está confirmando. */
  protected readonly porInactivar = signal<Zona | null>(null);
  /** La zona a la que se le está aplicando una acción. */
  protected readonly accionEn = signal<string | null>(null);
  private readonly erroresDeFila = signal<Readonly<Record<string, TextoVivo>>>({});

  protected readonly filas = computed(() =>
    this.visibles().map((zona) => ({
      zona,
      error: (this.erroresDeFila()[zona.id] as TextoVivo | undefined) ?? null,
    })),
  );

  // --- El formulario (registrar o editar) ---

  protected readonly formulario = new FormGroup({
    nombre: new FormControl(VALORES_ZONA_VACIOS.nombre, { nonNullable: true }),
    descripcion: new FormControl(VALORES_ZONA_VACIOS.descripcion, { nonNullable: true }),
    latitud: new FormControl<number | null>(null),
    longitud: new FormControl<number | null>(null),
    areaProtegida: new FormControl(false, { nonNullable: true }),
    altaAfluencia: new FormControl(false, { nonNullable: true }),
    buenasPracticas: new FormControl('', { nonNullable: true }),
    condicionesVisita: new FormControl('', { nonNullable: true }),
  });
  private readonly valores = toSignal(
    this.formulario.valueChanges.pipe(startWith(null), map((): ValoresZona => this.formulario.getRawValue())),
    { initialValue: this.formulario.getRawValue() },
  );
  private readonly erroresLocales = computed(() => validarZona(this.valores()));
  private readonly intentado = signal(false);
  /** La zona que se está editando; null mientras se registra una nueva. */
  protected readonly editando = signal<Zona | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  private readonly erroresDeApi = signal<Partial<Record<CampoZona, TextoVivo>>>({});

  constructor() {
    const filtro$ = this.ruta.queryParamMap.pipe(map((params) => filtroDeZonas(params.get('estado'))));

    combineLatest([filtro$, toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(([filtro]) => {
          this.filtro.set(filtro);
          this.cargando.set(true);
        }),
        switchMap(() =>
          this.catalogo.zonas().pipe(
            map((zonas): Cargado => ({ zonas })),
            catchError((fallo: unknown) => of<Cargado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('zonas' in resultado) {
          this.zonas.set(ordenarZonasPorNombre(resultado.zonas));
        }
        this.error.set('error' in resultado ? resultado.error : null);
      });

    // Al corregir un dato desaparece el error que el catálogo había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.erroresDeApi.set({}));

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  protected claveDeFiltro(filtro: FiltroZonas): ClaveTexto {
    return this.claveDelFiltro[filtro];
  }

  // --- Registrar y editar ---

  /** El mensaje de error de un campo: primero el del catálogo; si no, el de la validación local una vez tocado o al enviar. */
  protected errorDe(campo: CampoZona): string | null {
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi();
    }
    const motivo = this.erroresLocales()[campo];
    if (!motivo || !(this.intentado() || this.formulario.controls[campo].touched)) {
      return null;
    }
    return traducir(CLAVE_DE_ERROR[motivo]);
  }

  protected editar(zona: Zona): void {
    this.editando.set(zona);
    this.intentado.set(false);
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    this.formulario.reset(valoresDeZona(zona));
    // El formulario está arriba de la lista: se lleva la vista allí y el foco al primer campo.
    const campo = this.anfitrion.nativeElement.querySelector<HTMLElement>('#zona-nombre');
    campo?.scrollIntoView?.({ block: 'center' });
    campo?.focus();
  }

  protected cancelarEdicion(): void {
    this.limpiarFormulario();
  }

  private limpiarFormulario(): void {
    this.editando.set(null);
    this.intentado.set(false);
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    this.formulario.reset({ ...VALORES_ZONA_VACIOS });
  }

  protected guardar(): void {
    if (this.guardando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    this.intentado.set(true);

    const errores = this.erroresLocales();
    const primero = (['nombre', 'descripcion', 'latitud', 'longitud'] as const).find((campo) => errores[campo]);
    if (primero) {
      this.enfocar(primero);
      return;
    }

    const valores = this.formulario.getRawValue();
    const enEdicion = this.editando();
    this.guardando.set(true);
    const peticion = enEdicion
      ? this.catalogo.editarZona(enEdicion.id, construirEdicion(valores, enEdicion.activa))
      : this.catalogo.registrarZona(construirZona(valores));
    peticion.subscribe({
      next: (guardada) => {
        this.guardando.set(false);
        this.zonas.update((lista) =>
          lista === null
            ? lista
            : ordenarZonasPorNombre(
                enEdicion
                  ? lista.map((otra) => (otra.id === guardada.id ? guardada : otra))
                  : [...lista, guardada],
              ),
        );
        this.limpiarFormulario();
        this.avisos.mostrar(
          traducir(enEdicion ? 'admin.zonas.editada' : 'admin.zonas.registrada', { nombre: guardada.nombre }),
        );
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.mostrarErrorDeGuardado(aErrorApi(fallo));
      },
    });
  }

  private mostrarErrorDeGuardado(error: ErrorApi): void {
    if (error.codigo === 'ZONA_DUPLICADA') {
      this.erroresDeApi.set({ nombre: () => traducir('admin.zonas.error.nombreRepetido') });
      this.enfocar('nombre');
      return;
    }
    const campo = error.campo ? CAMPO_DE_API[error.campo] : undefined;
    if (campo) {
      this.erroresDeApi.set({ [campo]: () => error.detalleCampo ?? error.mensaje });
      this.enfocar(campo);
      return;
    }
    this.errorGeneral.set(() => error.mensaje);
    if (error.codigo === 'ZONA_NO_ENCONTRADA') {
      // La zona que se editaba ya no existe: se vuelve a leer la lista.
      this.limpiarFormulario();
      this.errorGeneral.set(() => error.mensaje);
      this.recarga.next();
    }
  }

  /** Lleva el foco al campo con error; los campos tienen como id el nombre de su control. */
  private enfocar(campo: CampoZona): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#zona-${campo}`)?.focus();
  }

  // --- Inactivar y reactivar ---

  protected pedirInactivar(zona: Zona): void {
    if (this.accionEn() !== null) {
      return;
    }
    this.porInactivar.set(zona);
    this.dialogo().abrir();
  }

  protected inactivar(): void {
    const zona = this.porInactivar();
    if (zona) {
      this.cambiarEstado(zona, false, 'admin.zonas.inactivada');
    }
  }

  protected reactivar(zona: Zona): void {
    this.cambiarEstado(zona, true, 'admin.zonas.reactivada');
  }

  private cambiarEstado(zona: Zona, activa: boolean, avisoOk: ClaveTexto): void {
    if (this.accionEn() !== null) {
      return;
    }
    this.erroresDeFila.update((errores) => sinClave(errores, zona.id));
    // Editar reemplaza todos los datos: se manda la zona completa con solo `activa` cambiada.
    const cuerpo = peticionDesdeZona(zona, activa);
    if (!cuerpo) {
      this.erroresDeFila.update((errores) => ({ ...errores, [zona.id]: () => traducir('admin.zonas.error.incompleta') }));
      return;
    }
    this.accionEn.set(zona.id);
    this.catalogo.editarZona(zona.id, cuerpo).subscribe({
      next: (actualizada) => {
        this.accionEn.set(null);
        this.zonas.update((lista) =>
          lista === null ? lista : lista.map((otra) => (otra.id === actualizada.id ? actualizada : otra)),
        );
        this.avisos.mostrar(traducir(avisoOk, { nombre: zona.nombre }));
      },
      error: (fallo: unknown) => {
        this.accionEn.set(null);
        const problema = aErrorApi(fallo);
        this.erroresDeFila.update((errores) => ({ ...errores, [zona.id]: () => problema.mensaje }));
        if (problema.codigo === 'ZONA_NO_ENCONTRADA') {
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
