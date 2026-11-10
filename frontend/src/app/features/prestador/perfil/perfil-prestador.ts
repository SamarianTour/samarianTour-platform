import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BehaviorSubject, Observable, catchError, combineLatest, map, of, startWith, switchMap, tap } from 'rxjs';
import { Foto, PerfilPublico } from '../../../core/api/modelos';
import { PerfilPrestadorService } from '../../../core/api/perfil-prestador.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { Icono } from '../../../shared/icono/icono';
import { TextosFotos } from '../formulario-oferta/fotos-oferta';
import { SelectorFotos } from '../formulario-oferta/selector-fotos/selector-fotos';
import {
  CAMPOS_PERFIL_EN_ORDEN,
  CampoPerfil,
  MotivoPerfil,
  VALORES_PERFIL_VACIOS,
  ValoresPerfil,
  construirPerfil,
  validarPerfil,
  valoresDePerfil,
} from './perfil';

const CLAVE_DE_ERROR: Readonly<Record<MotivoPerfil, ClaveTexto>> = {
  descripcionRequerida: 'prestador.perfil.error.descripcionRequerida',
  sitioInvalido: 'prestador.perfil.error.sitioInvalido',
  redesInvalidas: 'prestador.perfil.error.redesInvalidas',
};

const CAMPOS_DE_API: ReadonlySet<string> = new Set<CampoPerfil>(CAMPOS_PERFIL_EN_ORDEN);

/** Los textos de las fotos del perfil: lo mismo que las de una oferta, pero "del perfil". */
const TEXTOS_FOTOS_PERFIL: TextosFotos = {
  leyenda: 'prestador.perfil.fotos.leyenda',
  ayuda: 'prestador.perfil.fotos.ayuda',
  alt: 'prestador.perfil.fotos.alt',
  altPortada: 'prestador.perfil.fotos.alt',
  agregada: 'prestador.perfil.fotos.agregada',
  quitada: 'prestador.perfil.fotos.quitada',
  quitadaSinFotos: 'prestador.perfil.fotos.quitadaSinFotos',
  motivoMaximo: 'prestador.perfil.fotos.motivoMaximo',
};

type Cargado = { perfil: PerfilPublico | null } | { error: ErrorApi };

/**
 * P03.2 Perfil público (HU-PRE-03, RF-64, RF-72). El prestador escribe cómo se presenta al turista —nombre comercial, descripción,
 * horario, sitio web y redes— y sube hasta 6 fotos. Solo la descripción es obligatoria. Se publica de inmediato, sin moderación.
 * Las fotos usan el mismo selector que las ofertas, pero cada foto se sube y se quita en el servidor al instante, sin orden ni
 * portada; para subirlas el perfil tiene que existir, así que primero se guardan los datos.
 */
@Component({
  selector: 'app-perfil-prestador',
  imports: [ReactiveFormsModule, Icono, TPipe, SelectorFotos],
  templateUrl: './perfil-prestador.html',
  styleUrl: './perfil-prestador.css',
})
export class PerfilPrestador {
  private readonly servicio = inject(PerfilPrestadorService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir el perfil. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);

  protected readonly textosFotos = TEXTOS_FOTOS_PERFIL;

  // --- El perfil ---

  protected readonly cargando = signal(true);
  /** `undefined` mientras no se ha leído; `null` si todavía no lo creó. */
  protected readonly perfil = signal<PerfilPublico | null | undefined>(undefined);
  protected readonly errorCarga = signal<ErrorApi | null>(null);
  /** Las fotos del perfil; el selector las agrega y las quita (enlace de dos vías). */
  protected readonly fotos = signal<Foto[]>([]);

  // --- El formulario ---

  protected readonly formulario = new FormGroup({
    nombreComercial: new FormControl('', { nonNullable: true }),
    descripcion: new FormControl('', { nonNullable: true }),
    horariosAtencion: new FormControl('', { nonNullable: true }),
    sitioWeb: new FormControl('', { nonNullable: true }),
    redesSociales: new FormControl('', { nonNullable: true }),
  });
  private readonly valores = toSignal(
    this.formulario.valueChanges.pipe(startWith(null), map((): ValoresPerfil => this.formulario.getRawValue())),
    { initialValue: this.formulario.getRawValue() },
  );
  private readonly erroresLocales = computed(() => validarPerfil(this.valores()));
  private readonly intentado = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  private readonly erroresDeApi = signal<Partial<Record<CampoPerfil, TextoVivo>>>({});

  /** Cómo se suben y se quitan las fotos: las rutas del perfil, no las de las ofertas. */
  protected readonly subirFoto = (archivo: File): Observable<Foto> =>
    this.servicio.subirFoto(archivo).pipe(
      tap((perfil) => this.perfil.set(perfil)),
      // La API responde con el perfil entero y la foto nueva al final.
      map((perfil) => perfil.fotos[perfil.fotos.length - 1]),
    );
  protected readonly quitarFoto = (foto: Foto): Observable<unknown> => this.servicio.quitarFoto(foto.id);

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.servicio.consultar().pipe(
            map((perfil): Cargado => ({ perfil })),
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
        this.perfil.set(resultado.perfil);
        if (resultado.perfil) {
          this.fotos.set([...resultado.perfil.fotos]);
          if (!this.formulario.dirty) {
            this.formulario.reset(valoresDePerfil(resultado.perfil));
          }
        }
      });

    // Al corregir un dato desaparece el error que la API había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.erroresDeApi.set({}));
  }

  /** El error de un campo: primero el de la API; si no, el local una vez tocado o al enviar. */
  protected errorDe(campo: CampoPerfil): string | null {
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

  protected guardar(): void {
    if (this.guardando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    this.intentado.set(true);

    const errores = this.erroresLocales();
    const primero = CAMPOS_PERFIL_EN_ORDEN.find((campo) => errores[campo]);
    if (primero) {
      this.enfocar(primero);
      return;
    }

    const creando = !this.perfil();
    this.guardando.set(true);
    this.servicio.guardar(construirPerfil(this.formulario.getRawValue())).subscribe({
      next: (guardado) => {
        this.guardando.set(false);
        this.intentado.set(false);
        this.perfil.set(guardado);
        // El perfil conserva las fotos que ya tenía: el servidor es quien dice cuáles son.
        this.fotos.set([...guardado.fotos]);
        this.formulario.reset(valoresDePerfil(guardado));
        this.avisos.mostrar(traducir(creando ? 'prestador.perfil.creado' : 'prestador.perfil.guardado'));
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.mostrarError(aErrorApi(fallo));
      },
    });
  }

  private mostrarError(error: ErrorApi): void {
    const campo = error.campo as CampoPerfil | undefined;
    if (campo && CAMPOS_DE_API.has(campo)) {
      this.erroresDeApi.set({ [campo]: () => error.detalleCampo ?? error.mensaje });
      this.enfocar(campo);
      return;
    }
    this.errorGeneral.set(() => error.mensaje);
    if (error.codigo === 'CONFLICTO_CONCURRENCIA') {
      // Otra pestaña cambió el perfil al mismo tiempo: se vuelve a leer lo que quedó.
      this.recarga.next();
    }
  }

  private enfocar(campo: CampoPerfil): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#perfil-${campo}`)?.focus();
  }
}
