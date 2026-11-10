import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { IndicadoresService } from '../../../core/api/indicadores.service';
import { DistribucionCategoria } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { traducir } from '../../../core/idioma/traducir';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { BarraGrafico, GraficoBarras } from '../componentes/grafico-barras/grafico-barras';
import { TablaDatos } from '../componentes/tabla-datos/tabla-datos';
import { anchoDePorcentaje } from '../embudo/embudo';
import { formatoCantidad, formatoPorcentaje } from '../formato';
import { Periodo, periodoDeLaRuta } from '../periodo';
import {
  FilaCategoria,
  NOMBRE_DE_CATEGORIA,
  cifrasDeDistribucion,
  distribucionVacia,
  filasDeDistribucion,
  listaDeNombres,
} from './distribucion';

type Cargado = { distribucion: DistribucionCategoria } | { error: ErrorApi };

/**
 * P09.1 Distribución por tipo de prestador (RF-89). Cómo se reparte la demanda entre los prestadores grandes, medianos, pequeños y
 * los guías independientes, según la categoría que tenía el prestador al recibir cada solicitud, y cuántas de esas solicitudes se
 * pagaron. Son datos agregados (RET-03): un conteo de 1 o 2 no llega y se muestra como "menos de 3", sin estimarlo.
 */
@Component({
  selector: 'app-distribucion-gestor',
  imports: [TPipe, Icono, GraficoBarras, TablaDatos],
  templateUrl: './distribucion-gestor.html',
  styleUrl: './distribucion-gestor.css',
})
export class DistribucionGestor {
  private readonly ruta = inject(ActivatedRoute);
  private readonly indicadores = inject(IndicadoresService);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  private readonly fecha = new FechaPipe();
  /** El periodo de la última consulta, para no mostrar cifras de un periodo viejo mientras llegan las nuevas. */
  private periodoMostrado = '';

  protected readonly cargando = signal(true);
  protected readonly distribucion = signal<DistribucionCategoria | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);

  private readonly filas = computed(() => {
    const distribucion = this.distribucion();
    return distribucion ? filasDeDistribucion(distribucion) : [];
  });
  protected readonly vacia = computed(() => {
    const distribucion = this.distribucion();
    return !!distribucion && distribucionVacia(distribucion);
  });

  protected readonly cifras = computed(() => {
    const distribucion = this.distribucion();
    if (!distribucion) {
      return [];
    }
    const cifras = cifrasDeDistribucion(distribucion, this.filas());
    return [
      { clave: 'total', texto: traducir('gestor.distribucion.cifra.total'), valor: formatoCantidad(cifras.total) },
      {
        clave: 'mayor',
        texto: traducir('gestor.distribucion.cifra.mayor'),
        valor:
          cifras.mayores.length > 0
            ? listaDeNombres(cifras.mayores.map((categoria) => NOMBRE_DE_CATEGORIA[categoria]))
            : traducir('gestor.sinDato'),
      },
      {
        clave: 'categorias',
        texto: traducir('gestor.distribucion.cifra.categorias'),
        valor: formatoCantidad(cifras.categoriasConSolicitudes),
      },
    ];
  });

  // --- Solicitudes por tipo de prestador ---

  protected readonly barrasSolicitudes = computed((): BarraGrafico[] =>
    this.filas().map((fila) => ({
      id: fila.categoria,
      etiqueta: NOMBRE_DE_CATEGORIA[fila.categoria],
      ancho: anchoDePorcentaje(fila.solicitudes, fila.porcentaje),
      valor: this.solicitudesEnTexto(fila),
      omitida: fila.solicitudes === null,
    })),
  );
  protected readonly resumenSolicitudes = computed(() =>
    traducir('gestor.distribucion.solicitudes.resumen', {
      categorias: this.filas()
        .map((fila) => `${NOMBRE_DE_CATEGORIA[fila.categoria]}: ${this.solicitudesEnTexto(fila)}`)
        .join('. '),
    }),
  );
  protected readonly columnasSolicitudes = computed(() => [
    traducir('gestor.distribucion.col.tipo'),
    traducir('gestor.distribucion.col.solicitudes'),
    traducir('gestor.distribucion.col.porcentaje'),
  ]);
  protected readonly datosSolicitudes = computed(() =>
    this.filas().map((fila) => [
      NOMBRE_DE_CATEGORIA[fila.categoria],
      formatoCantidad(fila.solicitudes),
      formatoPorcentaje(fila.porcentaje),
    ]),
  );
  protected readonly tituloTablaSolicitudes = computed(() => this.tituloDeTabla('gestor.distribucion.solicitudes.tabla'));

  // --- Solicitudes pagadas por tipo de prestador ---

  protected readonly barrasPagadas = computed((): BarraGrafico[] =>
    this.filas().map((fila) => ({
      id: fila.categoria,
      etiqueta: NOMBRE_DE_CATEGORIA[fila.categoria],
      // La barra es la tasa de pago; si falta alguna cantidad no se dibuja una tasa: va la marca rayada.
      ancho: anchoDePorcentaje(fila.solicitudes === null || fila.pagadas === null ? null : fila.pagadas, fila.tasaDePago),
      valor: this.pagadasEnTexto(fila),
      omitida: fila.solicitudes === null || fila.pagadas === null,
    })),
  );
  protected readonly resumenPagadas = computed(() =>
    traducir('gestor.distribucion.pagadas.resumen', {
      categorias: this.filas()
        .map((fila) => `${NOMBRE_DE_CATEGORIA[fila.categoria]}: ${this.pagadasEnTexto(fila)}`)
        .join('. '),
    }),
  );
  protected readonly columnasPagadas = computed(() => [
    traducir('gestor.distribucion.col.tipo'),
    traducir('gestor.distribucion.col.solicitudes'),
    traducir('gestor.distribucion.col.pagadas'),
    traducir('gestor.distribucion.col.tasa'),
  ]);
  protected readonly datosPagadas = computed(() =>
    this.filas().map((fila) => [
      NOMBRE_DE_CATEGORIA[fila.categoria],
      formatoCantidad(fila.solicitudes),
      formatoCantidad(fila.pagadas),
      formatoPorcentaje(fila.tasaDePago),
    ]),
  );
  protected readonly tituloTablaPagadas = computed(() => this.tituloDeTabla('gestor.distribucion.pagadas.tabla'));

  constructor() {
    combineLatest([periodoDeLaRuta(this.ruta), this.recarga])
      .pipe(
        tap(([periodo]) => this.preparar(periodo)),
        switchMap(([periodo]) =>
          this.indicadores.categorias(periodo).pipe(
            map((distribucion): Cargado => ({ distribucion })),
            catchError((fallo: unknown) => of<Cargado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        if ('error' in resultado) {
          this.distribucion.set(null);
          this.error.set(resultado.error);
          return;
        }
        this.error.set(null);
        this.distribucion.set(resultado.distribucion);
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
      this.distribucion.set(null);
      this.error.set(null);
    }
  }

  // --- Cómo se escribe cada cosa ---

  /** Las solicitudes de una categoría con su porcentaje del total: "25 (59,5%)"; un conteo omitido es solo "menos de 3". */
  private solicitudesEnTexto(fila: FilaCategoria): string {
    if (fila.solicitudes === null || fila.porcentaje === null) {
      return formatoCantidad(fila.solicitudes);
    }
    return `${formatoCantidad(fila.solicitudes)} (${formatoPorcentaje(fila.porcentaje)})`;
  }

  /** Cuántas de las solicitudes de una categoría se pagaron: "10 de 25 (40,0%)". Sin solicitudes conocidas es solo "menos de 3". */
  private pagadasEnTexto(fila: FilaCategoria): string {
    if (fila.solicitudes === null) {
      return formatoCantidad(null);
    }
    const base = traducir('gestor.distribucion.pagadasDe', {
      pagadas: formatoCantidad(fila.pagadas),
      solicitudes: formatoCantidad(fila.solicitudes),
    });
    return fila.tasaDePago === null ? base : `${base} (${formatoPorcentaje(fila.tasaDePago)})`;
  }

  private tituloDeTabla(clave: 'gestor.distribucion.solicitudes.tabla' | 'gestor.distribucion.pagadas.tabla'): string {
    const distribucion = this.distribucion();
    return distribucion
      ? traducir(clave, {
          desde: this.fecha.transform(distribucion.desde, 'corta'),
          hasta: this.fecha.transform(distribucion.hasta, 'corta'),
        })
      : '';
  }
}
