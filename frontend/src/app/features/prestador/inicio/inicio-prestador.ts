import { Component, Signal, computed, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, Observable, catchError, combineLatest, map, of, switchMap } from 'rxjs';
import { OfertaPrestador } from '../../../core/api/modelos';
import { PrestadorPropio } from '../../../core/api/modelos-nucleo';
import { OfertasPrestadorService } from '../../../core/api/ofertas-prestador.service';
import { PrestadorService } from '../../../core/api/prestador.service';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { SesionService } from '../../../core/sesion/sesion.service';
import { TIPO_PRESTADOR } from '../../../shared/etiquetas';
import { Icono, NombreIcono } from '../../../shared/icono/icono';
import { CLASE_DE_ESTADO, ESTADO_OFERTA } from '../estado-oferta';
import {
  CLASE_SITUACION_RNT,
  CLASE_SITUACION_WHATSAPP,
  CLAVE_SITUACION_RNT,
  CLAVE_SITUACION_WHATSAPP,
  ESTADOS_DEL_DASHBOARD,
  avisoDeRnt,
  contarPorEstado,
  situacionRnt,
  situacionWhatsapp,
} from './inicio';

/** Una consulta que terminó bien o mal; mientras no termina, la señal vale `null`. */
type Carga<T> = { valor: T } | { error: ErrorApi };

/** Una sección del panel a la que el dashboard lleva. */
interface Acceso {
  readonly ruta: string;
  readonly titulo: ClaveTexto;
  readonly texto: ClaveTexto;
  readonly icono: NombreIcono;
}

/**
 * P03.0 Dashboard del prestador (HU-PRE-01). Resume el estado de su registro y de su RNT, cuántas ofertas tiene en cada estado y
 * lleva a cada sección. Si el RNT no está activo lo explica con claridad: sin RNT activo sus ofertas no se muestran en el catálogo.
 * El registro y las ofertas se piden por separado: si una consulta falla, la otra igual se ve.
 */
@Component({
  selector: 'app-inicio-prestador',
  imports: [RouterLink, Icono, TPipe, FechaPipe],
  templateUrl: './inicio-prestador.html',
  styleUrl: './inicio-prestador.css',
})
export class InicioPrestador {
  private readonly sesion = inject(SesionService);
  private readonly registros = inject(PrestadorService);
  private readonly ofertas = inject(OfertasPrestadorService);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir nada. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);

  protected readonly nombre = computed(() => this.sesion.cuenta()?.nombre ?? '');
  protected readonly etiquetaTipo = TIPO_PRESTADOR;
  protected readonly etiquetaEstado = ESTADO_OFERTA;
  protected readonly claseDeEstado = CLASE_DE_ESTADO;
  protected readonly estados = ESTADOS_DEL_DASHBOARD;
  protected readonly claveRnt = CLAVE_SITUACION_RNT;
  protected readonly claseRnt = CLASE_SITUACION_RNT;
  protected readonly claveWhatsapp = CLAVE_SITUACION_WHATSAPP;
  protected readonly claseWhatsapp = CLASE_SITUACION_WHATSAPP;

  protected readonly accesos: readonly Acceso[] = [
    { ruta: '/prestador/rnt', titulo: 'app.titulo.rntPrestador', texto: 'prestador.inicio.accesos.rnt', icono: 'escudo' },
    { ruta: '/prestador/perfil', titulo: 'app.titulo.perfilPublico', texto: 'prestador.inicio.accesos.perfil', icono: 'perfil' },
    { ruta: '/prestador/whatsapp', titulo: 'app.titulo.whatsappPrestador', texto: 'prestador.inicio.accesos.whatsapp', icono: 'mensaje' },
    { ruta: '/prestador/cuenta-cobro', titulo: 'app.titulo.cuentaCobro', texto: 'prestador.inicio.accesos.cuentaCobro', icono: 'tarjeta' },
  ];

  private readonly cargaRegistro = this.cargar(() => this.registros.registro());
  private readonly cargaOfertas = this.cargar(() => this.ofertas.todas());

  // --- El registro y el RNT ---

  /** `undefined` mientras se consulta o si falló; `null` si todavía no registró el RNT. */
  protected readonly prestador = computed<PrestadorPropio | null | undefined>(() => {
    const carga = this.cargaRegistro();
    return carga && 'valor' in carga ? carga.valor : undefined;
  });
  protected readonly errorRegistro = computed(() => {
    const carga = this.cargaRegistro();
    return carga && 'error' in carga ? carga.error : null;
  });
  protected readonly cargandoRegistro = computed(() => this.cargaRegistro() === null);
  protected readonly situacion = computed(() => {
    const prestador = this.prestador();
    return prestador === undefined ? null : situacionRnt(prestador);
  });
  protected readonly aviso = computed(() => {
    const situacion = this.situacion();
    return situacion ? avisoDeRnt(situacion) : null;
  });
  protected readonly situacionWhatsapp = computed(() => {
    const prestador = this.prestador();
    return prestador === undefined ? null : situacionWhatsapp(prestador);
  });

  // --- Las ofertas ---

  protected readonly listaOfertas = computed<OfertaPrestador[] | undefined>(() => {
    const carga = this.cargaOfertas();
    return carga && 'valor' in carga ? carga.valor : undefined;
  });
  protected readonly errorOfertas = computed(() => {
    const carga = this.cargaOfertas();
    return carga && 'error' in carga ? carga.error : null;
  });
  protected readonly cargandoOfertas = computed(() => this.cargaOfertas() === null);
  protected readonly conteo = computed(() => {
    const lista = this.listaOfertas();
    return lista ? contarPorEstado(lista) : null;
  });

  constructor() {
    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  /**
   * Pide algo al empezar, cuando entra otra cuenta y cuando hay que volver a consultar. Mientras llega la respuesta nueva se
   * conserva la anterior, para que la pantalla no parpadee.
   */
  private cargar<T>(origen: () => Observable<T>): Signal<Carga<T> | null> {
    return toSignal(
      combineLatest([toObservable(this.idDeLaCuenta), this.recarga]).pipe(
        switchMap(() =>
          origen().pipe(
            map((valor): Carga<T> => ({ valor })),
            catchError((fallo: unknown) => of<Carga<T>>({ error: aErrorApi(fallo) })),
          ),
        ),
      ),
      { initialValue: null },
    );
  }
}
