import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { AdminCatalogoService } from '../../../core/api/admin-catalogo.service';
import { AdminService } from '../../../core/api/admin.service';
import {
  OfertaPendiente,
  OpinionPendiente,
  PaginaOfertasPendientes,
  PaginaOpinionesPendientes,
  Zona,
} from '../../../core/api/modelos';
import { DecisionModerar } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { TIPO_OFERTA } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { ICONO_DE_TIPO, Icono } from '../../../shared/icono/icono';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { codigoDelPrestador } from '../alertas-sesgo';
import {
  MOTIVO_MAXIMO,
  TipoCola,
  camposCambiados,
  decisionDeOferta,
  decisionDeOpinion,
  filasDeVersion,
  puedeAprobar,
  tipoDeCola,
  validarDecision,
} from '../moderacion';

const TAMANO_PAGINA = 10;

type Cola =
  | { tipo: 'ofertas'; pagina: PaginaOfertasPendientes }
  | { tipo: 'opiniones'; pagina: PaginaOpinionesPendientes };
type Resultado = Cola | { error: ErrorApi };

/** Los errores que dicen que lo que la pantalla muestra ya no es cierto: se vuelve a leer la cola. */
const CODIGOS_DE_COLA_CAMBIADA = new Set([
  'VERSION_NO_VIGENTE',
  'NO_ESTA_EN_MODERACION',
  'MODERACION_YA_DECIDIDA',
  'PRESTADOR_NO_HABILITADO',
]);

/**
 * P05.2 Moderación (RF-79, RF-80). El administrador lee las colas en el catálogo (ofertas con la versión pendiente y, en una edición,
 * lo que cambió frente a la publicada; opiniones sin datos del turista) y **decide en el núcleo**: aprobar, o rechazar con un motivo
 * que se notifica al prestador o al turista. El núcleo guarda la decisión y el catálogo la aplica en los siguientes segundos por la
 * bandeja de salida, así que lo decidido sigue apareciendo en la cola un momento: se marca como "decisión enviada" y se deshabilitan
 * sus botones hasta que desaparezca.
 */
@Component({
  selector: 'app-moderacion-admin',
  imports: [RouterLink, Icono, TPipe, FechaPipe, Paginacion],
  templateUrl: './moderacion-admin.html',
  styleUrl: './moderacion-admin.css',
})
export class ModeracionAdmin {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogo = inject(AdminCatalogoService);
  private readonly admin = inject(AdminService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la cola. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;
  protected readonly codigo = codigoDelPrestador;
  protected readonly motivoMaximo = MOTIVO_MAXIMO;

  protected readonly tipo = signal<TipoCola>('ofertas');
  protected readonly cargando = signal(true);
  protected readonly cola = signal<Cola | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  private readonly zonas = signal<readonly Zona[]>([]);

  /** Lo ya decidido en esta sesión de la pantalla, por elemento: sigue en la cola hasta que el catálogo aplique la decisión. */
  private readonly decididas = signal<Readonly<Record<string, DecisionModerar>>>({});
  /** El elemento cuyo formulario de rechazo está abierto. */
  protected readonly rechazando = signal<string | null>(null);
  private readonly motivos = signal<Readonly<Record<string, string>>>({});
  protected readonly enviando = signal<string | null>(null);
  private readonly errores = signal<Readonly<Record<string, TextoVivo>>>({});
  /** Se intentó confirmar un rechazo sin motivo válido (para mostrar el error solo después de intentarlo). */
  private readonly intentado = signal<string | null>(null);

  protected readonly ofertas = computed(() => {
    const cola = this.cola();
    if (cola?.tipo !== 'ofertas') {
      return [];
    }
    return cola.pagina.contenido.map((oferta) => {
      const llave = llaveDeOferta(oferta);
      return {
        oferta,
        llave,
        esEdicion: !!oferta.publicada,
        filas: filasDeVersion(oferta.pendiente, oferta.publicada, this.zonas()),
        fotosCambiaron: camposCambiados(oferta.pendiente, oferta.publicada).has('fotos'),
        puedeAprobar: puedeAprobar(oferta),
        ...this.estadoDe(llave),
      };
    });
  });

  protected readonly opiniones = computed(() => {
    const cola = this.cola();
    if (cola?.tipo !== 'opiniones') {
      return [];
    }
    return cola.pagina.contenido.map((opinion) => {
      const llave = llaveDeOpinion(opinion);
      return { opinion, llave, ...this.estadoDe(llave) };
    });
  });

  constructor() {
    // Un solo flujo para la cola y la página: así un cambio de dirección hace una sola consulta.
    const consulta = this.ruta.queryParamMap.pipe(
      map((params) => ({ tipo: tipoDeCola(params.get('tipo')), pagina: aPagina(params.get('page')) })),
    );

    combineLatest([consulta, toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(([{ tipo }]) => {
          this.tipo.set(tipo);
          this.cargando.set(true);
        }),
        switchMap(([{ tipo, pagina }]) =>
          tipo === 'ofertas'
            ? this.catalogo.ofertasEnModeracion(pagina, TAMANO_PAGINA).pipe(
                map((datos): Resultado => ({ tipo: 'ofertas', pagina: datos })),
                catchError((fallo: unknown) => of<Resultado>({ error: aErrorApi(fallo) })),
              )
            : this.catalogo.opinionesEnModeracion(pagina, TAMANO_PAGINA).pipe(
                map((datos): Resultado => ({ tipo: 'opiniones', pagina: datos })),
                catchError((fallo: unknown) => of<Resultado>({ error: aErrorApi(fallo) })),
              ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if (!('error' in resultado)) {
          this.cola.set(resultado);
        }
        this.error.set('error' in resultado ? resultado.error : null);
      });

    // Las zonas solo ponen nombre a la zona de cada oferta: si no se pueden leer, se muestra su identificador.
    this.catalogo
      .zonas()
      .pipe(
        catchError(() => of<Zona[]>([])),
        takeUntilDestroyed(),
      )
      .subscribe((zonas) => this.zonas.set(zonas));

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  private estadoDe(llave: string) {
    const decision = this.decididas()[llave] ?? null;
    const motivo = this.motivos()[llave] ?? '';
    const motivoValido = validarDecision('RECHAZAR', motivo);
    return {
      decision,
      abierto: this.rechazando() === llave,
      motivo,
      largoMotivo: motivo.trim().length,
      errorDeMotivo: this.intentado() === llave ? motivoValido : null,
      error: (this.errores()[llave] as TextoVivo | undefined) ?? null,
      enviando: this.enviando() === llave,
    };
  }

  protected claveDeError(error: 'motivoRequerido' | 'motivoLargo'): ClaveTexto {
    return error === 'motivoRequerido'
      ? 'admin.moderacion.error.motivoRequerido'
      : 'admin.moderacion.error.motivoLargo';
  }

  protected claveDeEnviada(decision: DecisionModerar): ClaveTexto {
    return decision === 'APROBAR' ? 'admin.moderacion.enviada.APROBAR' : 'admin.moderacion.enviada.RECHAZAR';
  }

  protected actualizar(): void {
    this.recarga.next();
  }

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { tipo: this.tipo(), ...(pagina > 0 ? { page: pagina } : {}) },
    });
  }

  // --- Decidir ---

  protected abrirRechazo(llave: string): void {
    this.rechazando.set(llave);
    this.intentado.set(null);
  }

  protected cancelarRechazo(): void {
    this.rechazando.set(null);
    this.intentado.set(null);
  }

  protected escribirMotivo(llave: string, evento: Event): void {
    const valor = (evento.target as HTMLTextAreaElement).value;
    this.motivos.update((motivos) => ({ ...motivos, [llave]: valor }));
  }

  protected aprobarOferta(oferta: OfertaPendiente): void {
    this.decidir(llaveDeOferta(oferta), 'APROBAR', (decision, motivo) =>
      this.admin.moderarOferta(oferta.id, decisionDeOferta(oferta.numeroVersion, decision, motivo)),
    );
  }

  protected rechazarOferta(oferta: OfertaPendiente): void {
    this.decidir(llaveDeOferta(oferta), 'RECHAZAR', (decision, motivo) =>
      this.admin.moderarOferta(oferta.id, decisionDeOferta(oferta.numeroVersion, decision, motivo)),
    );
  }

  protected aprobarOpinion(opinion: OpinionPendiente): void {
    this.decidir(llaveDeOpinion(opinion), 'APROBAR', (decision, motivo) =>
      this.admin.moderarOpinion(opinion.id, decisionDeOpinion(decision, motivo)),
    );
  }

  protected rechazarOpinion(opinion: OpinionPendiente): void {
    this.decidir(llaveDeOpinion(opinion), 'RECHAZAR', (decision, motivo) =>
      this.admin.moderarOpinion(opinion.id, decisionDeOpinion(decision, motivo)),
    );
  }

  private decidir(
    llave: string,
    decision: DecisionModerar,
    enviar: (decision: DecisionModerar, motivo: string) => ReturnType<AdminService['moderarOferta']>,
  ): void {
    if (this.enviando() !== null || this.decididas()[llave]) {
      return;
    }
    const motivo = this.motivos()[llave] ?? '';
    if (validarDecision(decision, motivo) !== null) {
      this.intentado.set(llave);
      return;
    }
    this.enviando.set(llave);
    this.errores.update((errores) => sinClave(errores, llave));
    enviar(decision, motivo).subscribe({
      next: () => {
        this.enviando.set(null);
        this.rechazando.set(null);
        this.intentado.set(null);
        this.decididas.update((decididas) => ({ ...decididas, [llave]: decision }));
        this.avisos.mostrar(traducir(decision === 'APROBAR' ? 'admin.moderacion.aviso.APROBAR' : 'admin.moderacion.aviso.RECHAZAR'));
      },
      error: (fallo: unknown) => {
        this.enviando.set(null);
        const problema = aErrorApi(fallo);
        this.errores.update((errores) => ({ ...errores, [llave]: () => problema.mensaje }));
        if (problema.codigo && CODIGOS_DE_COLA_CAMBIADA.has(problema.codigo)) {
          // Lo que se veía ya no es cierto (otra versión, otra decisión, el RNT cambió): se vuelve a leer la cola.
          this.recarga.next();
        }
      },
    });
  }
}

const llaveDeOferta = (oferta: OfertaPendiente): string => `o:${oferta.id}:${oferta.numeroVersion}`;
const llaveDeOpinion = (opinion: OpinionPendiente): string => `p:${opinion.id}`;

function aPagina(valor: string | null): number {
  const pagina = Number(valor);
  return Number.isInteger(pagina) && pagina > 0 ? pagina : 0;
}

/** Una copia del registro sin esa clave. */
function sinClave<T>(registro: Readonly<Record<string, T>>, clave: string): Record<string, T> {
  const copia = { ...registro };
  delete copia[clave];
  return copia;
}
