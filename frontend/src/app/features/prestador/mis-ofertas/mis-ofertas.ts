import { Component, Injector, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { OfertaPrestador, PaginaOfertasPrestador } from '../../../core/api/modelos';
import { OfertasPrestadorService } from '../../../core/api/ofertas-prestador.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { IdentidadService } from '../../../core/identidad/identidad.service';
import { DialogoConfirmacion } from '../../../shared/dialogo-confirmacion/dialogo-confirmacion';
import { TIPO_OFERTA, UNIDAD_PRECIO } from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { PrecioPipe } from '../../../shared/precio.pipe';
import { CLASE_DE_ESTADO, ESTADO_OFERTA, EstadoOferta, estadoDe, motivoDeRechazo, sePuedeEditar, versionVigente } from '../estado-oferta';

const TAMANO_PAGINA = 10;

type Resultado = { pagina: PaginaOfertasPrestador } | { error: ErrorApi };

/** Una fila de la tabla, con lo que se calcula de la oferta ya resuelto. */
interface Fila {
  readonly oferta: OfertaPrestador;
  readonly nombre: string;
  readonly precio: number | undefined;
  readonly estado: EstadoOferta;
  readonly motivoRechazo: string | undefined;
  readonly editable: boolean;
}

/** Ofertas del prestador, con su estado calculado y las acciones de ver, editar y retirar (RF-65, RF-66). */
@Component({
  selector: 'app-mis-ofertas',
  imports: [RouterLink, PrecioPipe, Paginacion, DialogoConfirmacion, Icono],
  templateUrl: './mis-ofertas.html',
  styleUrl: './mis-ofertas.css',
})
export class MisOfertas {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly servicio = inject(OfertasPrestadorService);
  private readonly identidad = inject(IdentidadService);
  private readonly avisos = inject(AvisosService);
  private readonly injector = inject(Injector);

  private readonly dialogo = viewChild.required(DialogoConfirmacion);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly etiquetaEstado = ESTADO_OFERTA;
  protected readonly claseDeEstado = CLASE_DE_ESTADO;
  protected readonly unidad = UNIDAD_PRECIO;

  protected readonly cargando = signal(true);
  protected readonly pagina = signal<PaginaOfertasPrestador | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  protected readonly porRetirar = signal<Fila | null>(null);
  protected readonly retirando = signal(false);

  protected readonly filas = computed(() => (this.pagina()?.contenido ?? []).map(aFila));
  protected readonly mensajeDeRetiro = computed(
    () =>
      `«${this.porRetirar()?.nombre ?? ''}» dejará de verse en el catálogo. El retiro es definitivo: después no podrás editarla ni volver a publicarla.`,
  );

  constructor() {
    const numeroDePagina = this.ruta.queryParamMap.pipe(map((params) => aPagina(params.get('page'))));

    // Se vuelve a pedir la lista al cambiar de página, de prestador demo o después de un retiro.
    combineLatest([numeroDePagina, toObservable(this.identidad.prestador), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(([pagina]) =>
          this.servicio.listar(pagina, TAMANO_PAGINA).pipe(
            map((resultado): Resultado => ({ pagina: resultado })),
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
  }

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: pagina > 0 ? { page: pagina } : {} });
  }

  protected pedirRetiro(fila: Fila): void {
    this.porRetirar.set(fila);
    // Se abre después de pintar, para que el diálogo ya tenga el nombre de la oferta cuando recibe el foco.
    afterNextRender(() => this.dialogo().abrir(), { injector: this.injector });
  }

  protected retirar(): void {
    const fila = this.porRetirar();
    if (!fila || this.retirando()) {
      return;
    }
    this.retirando.set(true);
    this.servicio.retirar(fila.oferta.id).subscribe({
      next: () => {
        this.retirando.set(false);
        this.avisos.mostrar(`Retiramos «${fila.nombre}» del catálogo.`);
        this.recarga.next();
      },
      error: (error: unknown) => {
        this.retirando.set(false);
        this.error.set(aErrorApi(error));
      },
    });
  }
}

function aFila(oferta: OfertaPrestador): Fila {
  const version = versionVigente(oferta);
  return {
    oferta,
    nombre: version?.nombre ?? 'Oferta sin nombre',
    precio: version?.precioReferencia,
    estado: estadoDe(oferta),
    motivoRechazo: motivoDeRechazo(oferta),
    editable: sePuedeEditar(oferta),
  };
}

function aPagina(valor: string | null): number {
  const pagina = Number(valor);
  return Number.isInteger(pagina) && pagina > 0 ? pagina : 0;
}
