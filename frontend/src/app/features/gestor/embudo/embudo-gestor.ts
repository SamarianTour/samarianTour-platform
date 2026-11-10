import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { IndicadoresService } from '../../../core/api/indicadores.service';
import { Embudo } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { LOCALE, idiomaActual } from '../../../core/idioma/idioma';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { TIPO_OFERTA } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { BarraGrafico, GraficoBarras } from '../componentes/grafico-barras/grafico-barras';
import { TablaDatos } from '../componentes/tabla-datos/tabla-datos';
import { formatoCantidad, formatoPorcentaje } from '../formato';
import { Periodo, periodoDeLaRuta } from '../periodo';
import {
  EtapaCalculada,
  NombreEtapa,
  anchoDeEtapa,
  cifrasDeEmbudo,
  embudoVacio,
  embudosPorTipo,
  etapasCalculadas,
} from './embudo';

const CLAVE_DE_ETAPA: Readonly<Record<NombreEtapa, ClaveTexto>> = {
  ENVIADAS: 'gestor.embudo.etapa.ENVIADAS',
  ATENDIDAS: 'gestor.embudo.etapa.ATENDIDAS',
  CONFIRMADAS: 'gestor.embudo.etapa.CONFIRMADAS',
  PAGADAS: 'gestor.embudo.etapa.PAGADAS',
};

type Cargado = { embudo: Embudo } | { error: ErrorApi };

/**
 * P09.1 Embudo de conversión (RF-87, RF-88). De las solicitudes enviadas en el periodo, cuántas llegaron a ser atendidas,
 * confirmadas y pagadas, en total y por tipo de oferta. El porcentaje sobre las enviadas es el del núcleo; el paso de una etapa a la
 * siguiente se calcula aquí solo cuando las dos cantidades se conocen. Son datos agregados (RET-03): un conteo de 1 o 2 no llega y
 * se muestra como "menos de 3", sin estimarlo.
 */
@Component({
  selector: 'app-embudo-gestor',
  imports: [TPipe, Icono, GraficoBarras, TablaDatos],
  templateUrl: './embudo-gestor.html',
  styleUrl: './embudo-gestor.css',
})
export class EmbudoGestor {
  private readonly ruta = inject(ActivatedRoute);
  private readonly indicadores = inject(IndicadoresService);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  private readonly fecha = new FechaPipe();
  /** El periodo de la última consulta, para no mostrar cifras de un periodo viejo mientras llegan las nuevas. */
  private periodoMostrado = '';

  protected readonly cargando = signal(true);
  protected readonly embudo = signal<Embudo | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);

  private readonly etapas = computed(() => {
    const embudo = this.embudo();
    return embudo ? etapasCalculadas(embudo.etapas) : [];
  });
  protected readonly vacio = computed(() => embudoVacio(this.etapas()));

  protected readonly cifras = computed(() => {
    if (!this.embudo()) {
      return [];
    }
    const cifras = cifrasDeEmbudo(this.etapas());
    return [
      { clave: 'enviadas', texto: traducir('gestor.embudo.cifra.enviadas'), valor: formatoCantidad(cifras.enviadas) },
      { clave: 'pagadas', texto: traducir('gestor.embudo.cifra.pagadas'), valor: formatoCantidad(cifras.pagadas) },
      {
        clave: 'conversion',
        texto: traducir('gestor.embudo.cifra.conversion'),
        valor: formatoPorcentaje(cifras.conversionTotal),
      },
    ];
  });

  // --- El embudo del total ---

  protected readonly barras = computed(() => this.barrasDe(this.etapas()));
  protected readonly resumen = computed(() =>
    traducir('gestor.embudo.resumen', { etapas: this.etapasEnTexto(this.etapas()) }),
  );
  protected readonly columnas = computed(() => this.columnasDeTabla());
  protected readonly filas = computed(() => this.filasDe(this.etapas()));
  protected readonly tituloDeTabla = computed(() => {
    const embudo = this.embudo();
    return embudo ? traducir('gestor.embudo.tabla.titulo', this.fechas(embudo)) : '';
  });

  // --- El embudo de cada tipo de oferta ---

  protected readonly porTipo = computed(() => {
    const embudo = this.embudo();
    if (!embudo) {
      return [];
    }
    return embudosPorTipo(embudo).map(({ tipoOferta, etapas }) => {
      const tipo = TIPO_OFERTA[tipoOferta];
      return {
        clave: tipoOferta,
        nombre: tipo,
        vacio: embudoVacio(etapas),
        barras: this.barrasDe(etapas),
        resumen: traducir('gestor.embudo.resumenTipo', { tipo, etapas: this.etapasEnTexto(etapas) }),
        columnas: this.columnasDeTabla(),
        filas: this.filasDe(etapas),
        titulo: traducir('gestor.embudo.tabla.tituloTipo', { tipo, ...this.fechas(embudo) }),
      };
    });
  });

  constructor() {
    combineLatest([periodoDeLaRuta(this.ruta), this.recarga])
      .pipe(
        tap(([periodo]) => this.preparar(periodo)),
        switchMap(([periodo]) =>
          this.indicadores.embudo(periodo).pipe(
            map((embudo): Cargado => ({ embudo })),
            catchError((fallo: unknown) => of<Cargado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        if ('error' in resultado) {
          this.embudo.set(null);
          this.error.set(resultado.error);
          return;
        }
        this.error.set(null);
        this.embudo.set(resultado.embudo);
      });

    // Al volver a la pestaña o recuperar la conexión se pregunta de nuevo, para no dejar cifras viejas.
    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  private preparar(periodo: Periodo): void {
    this.cargando.set(true);
    const clave = `${periodo.desde}|${periodo.hasta}`;
    if (clave !== this.periodoMostrado) {
      this.periodoMostrado = clave;
      this.embudo.set(null);
      this.error.set(null);
    }
  }

  // --- Cómo se escribe cada cosa ---

  private fechas(embudo: Embudo): { desde: string; hasta: string } {
    return {
      desde: this.fecha.transform(embudo.desde, 'corta'),
      hasta: this.fecha.transform(embudo.hasta, 'corta'),
    };
  }

  private nombreDe(etapa: NombreEtapa): string {
    return traducir(CLAVE_DE_ETAPA[etapa]);
  }

  /** La cantidad de una etapa con su porcentaje: "30 (71,4 %)"; un conteo omitido es solo "menos de 3". */
  private valorDe(etapa: EtapaCalculada): string {
    if (etapa.solicitudes === null || etapa.porcentaje === null) {
      return formatoCantidad(etapa.solicitudes);
    }
    return `${formatoCantidad(etapa.solicitudes)} (${formatoPorcentaje(etapa.porcentaje)})`;
  }

  private barrasDe(etapas: readonly EtapaCalculada[]): BarraGrafico[] {
    return etapas.map((etapa, i) => ({
      id: etapa.etapa,
      etiqueta: this.nombreDe(etapa.etapa),
      // De cuántas de las anteriores llegaron a esta, para ver dónde se pierden.
      detalle:
        etapa.deLaAnterior === null || i === 0
          ? undefined
          : traducir('gestor.embudo.deLaAnterior', {
              porcentaje: formatoPorcentaje(etapa.deLaAnterior),
              anterior: this.nombreDe(etapas[i - 1].etapa).toLocaleLowerCase(LOCALE[idiomaActual()]),
            }),
      ancho: anchoDeEtapa(etapa),
      valor: this.valorDe(etapa),
      omitida: etapa.solicitudes === null,
    }));
  }

  /** Lo que oyen los lectores de pantalla en lugar del dibujo: cada etapa con su cantidad y su porcentaje. */
  private etapasEnTexto(etapas: readonly EtapaCalculada[]): string {
    return etapas.map((etapa) => `${this.nombreDe(etapa.etapa)}: ${this.valorDe(etapa)}`).join('. ');
  }

  private columnasDeTabla(): string[] {
    return [
      traducir('gestor.embudo.col.etapa'),
      traducir('gestor.embudo.col.solicitudes'),
      traducir('gestor.embudo.col.porcentaje'),
      traducir('gestor.embudo.col.anterior'),
    ];
  }

  private filasDe(etapas: readonly EtapaCalculada[]): string[][] {
    return etapas.map((etapa, i) => [
      this.nombreDe(etapa.etapa),
      formatoCantidad(etapa.solicitudes),
      formatoPorcentaje(etapa.porcentaje),
      i === 0 ? traducir('gestor.noAplica') : formatoPorcentaje(etapa.deLaAnterior),
    ]);
  }
}
