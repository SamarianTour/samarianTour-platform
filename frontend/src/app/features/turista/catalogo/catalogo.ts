import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, map, of, switchMap, tap } from 'rxjs';
import { nivelEsfuerzoValues, tipoActividadValues, tipoOfertaValues } from '../../../core/api/catalogo-api';
import { CatalogoService } from '../../../core/api/catalogo.service';
import { NivelEsfuerzo, PaginaOfertas, TipoActividad, TipoOferta, Zona } from '../../../core/api/modelos';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { traducir } from '../../../core/idioma/traducir';
import { NIVEL_ESFUERZO, TIPO_ACTIVIDAD, TIPO_OFERTA } from '../../../shared/etiquetas';
import { ICONO_DE_TIPO, Icono } from '../../../shared/icono/icono';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { PrecioPipe } from '../../../shared/precio.pipe';
import { UnidadPipe } from '../../../shared/unidad.pipe';
import {
  FiltrosCatalogo,
  SIN_FILTROS,
  consultaDeFiltros,
  filtrosDesdeUrl,
  TEXTO_MAXIMO,
  hayFiltros,
  normalizarTexto,
  textoInvalido,
  urlDeFiltros,
} from './filtros';

type Resultado = { pagina: PaginaOfertas } | { error: ErrorApi };

/** Catálogo del turista: búsqueda de ofertas visibles con filtros y paginación (RF-14 a RF-17). */
@Component({
  selector: 'app-catalogo',
  imports: [ReactiveFormsModule, RouterLink, PrecioPipe, UnidadPipe, Paginacion, Icono, TPipe],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.css',
})
export class Catalogo {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogo = inject(CatalogoService);

  protected readonly tipos = tipoOfertaValues;
  protected readonly tiposActividad = tipoActividadValues;
  protected readonly niveles = nivelEsfuerzoValues;
  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly etiquetaActividad = TIPO_ACTIVIDAD;
  protected readonly etiquetaNivel = NIVEL_ESFUERZO;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;

  /** El largo máximo del texto, para el atributo `maxlength` del campo. */
  protected readonly textoMaximo = TEXTO_MAXIMO;

  protected readonly formulario = new FormGroup({
    texto: new FormControl('', { nonNullable: true }),
    tipo: new FormControl<TipoOferta | ''>('', { nonNullable: true }),
    zonaId: new FormControl('', { nonNullable: true }),
    tipoActividad: new FormControl<TipoActividad | ''>('', { nonNullable: true }),
    precioMin: new FormControl<number | null>(null),
    precioMax: new FormControl<number | null>(null),
    nivelEsfuerzoMaximo: new FormControl<NivelEsfuerzo | ''>('', { nonNullable: true }),
    accesibleSillaRuedas: new FormControl(false, { nonNullable: true }),
  });

  protected readonly filtros = signal<FiltrosCatalogo>(SIN_FILTROS);
  protected readonly cargando = signal(true);
  protected readonly pagina = signal<PaginaOfertas | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  protected readonly zonas = signal<readonly Zona[]>([]);
  /** Lo que hay escrito en el campo de búsqueda, aunque todavía no alcance para buscar. */
  protected readonly escrito = signal('');
  /** Hay texto, pero de menos de 2 caracteres: se avisa y no se busca (DC-37). */
  protected readonly textoCorto = computed(() => textoInvalido(this.escrito()));

  protected readonly conFiltros = computed(() => hayFiltros(this.filtros()));
  /** La zona del filtro, para avisar si es de alta afluencia o un área protegida (RF-25, RF-29). */
  protected readonly zonaElegida = computed(() => this.zonas().find((zona) => zona.id === this.filtros().zonaId));
  private readonly nombresDeZona = computed(() => new Map(this.zonas().map((zona) => [zona.id, zona.nombre])));

  constructor() {
    this.catalogo
      .zonas()
      .pipe(
        catchError(() => of<Zona[]>([])),
        takeUntilDestroyed(),
      )
      .subscribe((zonas) => this.zonas.set(zonas));

    // La URL manda: cada cambio de sus query params vuelve a llenar el formulario y a buscar.
    this.ruta.queryParamMap
      .pipe(
        map(filtrosDesdeUrl),
        tap((filtros) => {
          this.filtros.set(filtros);
          this.cargando.set(true);
          // Si el campo tiene un texto que todavía no se puede buscar ("b"), no se pisa mientras la persona escribe.
          const campo = this.formulario.controls.texto.value;
          const texto = normalizarTexto(campo) === filtros.texto ? campo : (filtros.texto ?? '');
          this.formulario.setValue({ ...aFormulario(filtros), texto }, { emitEvent: false });
          this.escrito.set(texto);
        }),
        switchMap((filtros) =>
          this.catalogo.buscar(consultaDeFiltros(filtros)).pipe(
            map((pagina): Resultado => ({ pagina })),
            catchError((error: unknown) => of<Resultado>({ error: aErrorApi(error) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        this.pagina.set('pagina' in resultado ? resultado.pagina : null);
        this.error.set('error' in resultado ? resultado.error : null);
      });

    // El formulario solo cambia la URL; al cambiar un filtro se vuelve a la primera página.
    this.formulario.valueChanges
      .pipe(debounceTime(350), takeUntilDestroyed())
      .subscribe(() => this.navegar({ ...this.filtrosDelFormulario(), pagina: 0 }));

    this.formulario.controls.texto.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((valor) => this.escrito.set(valor));
  }

  protected zona(id: string): string {
    return this.nombresDeZona().get(id) ?? '';
  }

  protected irAPagina(pagina: number): void {
    this.navegar({ ...this.filtros(), pagina });
  }

  protected borrarTexto(): void {
    this.formulario.controls.texto.setValue('');
  }

  protected limpiar(): void {
    this.navegar(SIN_FILTROS);
  }

  /** El mensaje de la API sobre un campo de precio, si el error es de ese campo. */
  protected errorDe(campo: 'precioMin' | 'precioMax'): string | null {
    const error = this.error();
    if (error?.campo !== campo) {
      return null;
    }
    const { precioMin, precioMax } = this.filtros();
    if (precioMin !== null && precioMax !== null && precioMin > precioMax) {
      return traducir('turista.catalogo.precioInvertido');
    }
    return error.mensaje;
  }

  private navegar(filtros: FiltrosCatalogo): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: urlDeFiltros(filtros) });
  }

  private filtrosDelFormulario(): FiltrosCatalogo {
    const valores = this.formulario.getRawValue();
    return {
      texto: normalizarTexto(valores.texto),
      tipo: valores.tipo || null,
      zonaId: valores.zonaId || null,
      tipoActividad: valores.tipoActividad || null,
      precioMin: precio(valores.precioMin),
      precioMax: precio(valores.precioMax),
      nivelEsfuerzoMaximo: valores.nivelEsfuerzoMaximo || null,
      accesibleSillaRuedas: valores.accesibleSillaRuedas,
      pagina: 0,
    };
  }
}

function aFormulario(filtros: FiltrosCatalogo) {
  return {
    texto: filtros.texto ?? '',
    tipo: filtros.tipo ?? ('' as const),
    zonaId: filtros.zonaId ?? '',
    tipoActividad: filtros.tipoActividad ?? ('' as const),
    precioMin: filtros.precioMin,
    precioMax: filtros.precioMax,
    nivelEsfuerzoMaximo: filtros.nivelEsfuerzoMaximo ?? ('' as const),
    accesibleSillaRuedas: filtros.accesibleSillaRuedas,
  };
}

/** Un precio vacío o negativo no filtra. */
function precio(valor: number | null): number | null {
  return valor !== null && Number.isFinite(valor) && valor >= 0 ? valor : null;
}
