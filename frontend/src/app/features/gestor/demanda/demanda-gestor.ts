import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { CatalogoService } from '../../../core/api/catalogo.service';
import { IndicadoresService } from '../../../core/api/indicadores.service';
import { Zona } from '../../../core/api/modelos';
import { Tablero } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { traducir } from '../../../core/idioma/traducir';
import { TIPO_OFERTA } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { BarraGrafico, GraficoBarras } from '../componentes/grafico-barras/grafico-barras';
import { TablaDatos } from '../componentes/tabla-datos/tabla-datos';
import { formatoCantidad } from '../formato';
import { Periodo, periodoDeLaRuta } from '../periodo';
import { anchoDeBarra, cifrasDeDemanda, datosDeZona, filasDeDemanda, maximoConocido } from './demanda';

type Cargado = { tablero: Tablero; zonas: Zona[] } | { error: ErrorApi };

/**
 * P09.1 Demanda (RF-85, RF-86). Cuántas solicitudes se enviaron en el periodo, por zona y por tipo de oferta, con las zonas
 * sensibles (área protegida, alta afluencia) marcadas. Son datos agregados (RET-03): un conteo de 1 o 2 no llega y se muestra como
 * "menos de 3", sin sumarlo ni estimarlo. El nombre de la zona sale de la lista pública de zonas; el tablero trae solo su id.
 */
@Component({
  selector: 'app-demanda-gestor',
  imports: [TPipe, Icono, GraficoBarras, TablaDatos],
  templateUrl: './demanda-gestor.html',
  styleUrl: './demanda-gestor.css',
})
export class DemandaGestor {
  private readonly ruta = inject(ActivatedRoute);
  private readonly indicadores = inject(IndicadoresService);
  private readonly catalogo = inject(CatalogoService);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  private readonly fecha = new FechaPipe();
  /** El periodo de la última consulta, para no mostrar cifras de un periodo viejo mientras llegan las nuevas. */
  private periodoMostrado = '';

  protected readonly cargando = signal(true);
  protected readonly tablero = signal<Tablero | null>(null);
  private readonly zonas = signal<readonly Zona[]>([]);
  protected readonly error = signal<ErrorApi | null>(null);

  private readonly filas = computed(() => {
    const tablero = this.tablero();
    return tablero ? filasDeDemanda(tablero) : [];
  });
  protected readonly hayFilas = computed(() => this.filas().length > 0);

  /** Las tres cifras de arriba: el total es el del núcleo; las zonas y los tipos se cuentan de las filas. */
  protected readonly cifras = computed(() => {
    const tablero = this.tablero();
    if (!tablero) {
      return [];
    }
    const cifras = cifrasDeDemanda(tablero);
    return [
      { clave: 'total', texto: traducir('gestor.demanda.cifra.total'), valor: formatoCantidad(cifras.total) },
      { clave: 'zonas', texto: traducir('gestor.demanda.cifra.zonas'), valor: formatoCantidad(cifras.zonas) },
      { clave: 'tipos', texto: traducir('gestor.demanda.cifra.tipos'), valor: formatoCantidad(cifras.tiposDeOferta) },
    ];
  });

  /** Cada fila con el nombre de su zona, sus marcas y su tipo de oferta ya escritos en el idioma vigente. */
  private readonly detalles = computed(() =>
    this.filas().map((fila) => {
      const zona = datosDeZona(fila.zonaId, this.zonas());
      const marcas = [
        ...(zona.areaProtegida ? [{ texto: traducir('admin.zonas.marca.areaProtegida'), clase: 'etiqueta--protegida' }] : []),
        ...(zona.altaAfluencia ? [{ texto: traducir('admin.zonas.marca.altaAfluencia'), clase: 'etiqueta--afluencia' }] : []),
      ];
      return {
        fila,
        marcas,
        zona: zona.nombre ?? traducir('gestor.demanda.zonaSinNombre', { id: fila.zonaId }),
        tipo: TIPO_OFERTA[fila.tipoOferta],
        cantidad: formatoCantidad(fila.solicitudes),
      };
    }),
  );

  protected readonly barras = computed((): BarraGrafico[] => {
    const maximo = maximoConocido(this.filas());
    return this.detalles().map(({ fila, marcas, zona, tipo, cantidad }) => ({
      id: fila.clave,
      etiqueta: zona,
      detalle: tipo,
      marcas,
      ancho: anchoDeBarra(fila.solicitudes, maximo),
      valor: cantidad,
      omitida: fila.solicitudes === null,
    }));
  });

  /** Lo que oyen los lectores de pantalla en lugar del dibujo; los datos completos están en la tabla. */
  protected readonly resumen = computed(() => {
    const primera = this.detalles()[0];
    return primera
      ? traducir('gestor.demanda.resumen', { zona: primera.zona, tipo: primera.tipo, cantidad: primera.cantidad })
      : '';
  });

  protected readonly columnas = computed(() => [
    traducir('gestor.demanda.col.zona'),
    traducir('gestor.demanda.col.tipo'),
    traducir('gestor.demanda.col.solicitudes'),
  ]);

  protected readonly datos = computed(() =>
    this.detalles().map(({ marcas, zona, tipo, cantidad }) => [
      marcas.length > 0 ? `${zona} (${marcas.map((marca) => marca.texto.toLowerCase()).join(', ')})` : zona,
      tipo,
      cantidad,
    ]),
  );

  protected readonly tituloDeTabla = computed(() => {
    const tablero = this.tablero();
    return tablero
      ? traducir('gestor.demanda.tabla.titulo', {
          desde: this.fecha.transform(tablero.desde, 'corta'),
          hasta: this.fecha.transform(tablero.hasta, 'corta'),
        })
      : '';
  });

  constructor() {
    combineLatest([periodoDeLaRuta(this.ruta), this.recarga])
      .pipe(
        tap(([periodo]) => this.preparar(periodo)),
        switchMap(([periodo]) =>
          forkJoin({
            tablero: this.indicadores.tablero(periodo),
            // Sin la lista de zonas el tablero igual se ve, con el id de cada zona.
            zonas: this.catalogo.zonas().pipe(catchError(() => of<Zona[]>([]))),
          }).pipe(
            map((respuesta): Cargado => respuesta),
            catchError((fallo: unknown) => of<Cargado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        if ('error' in resultado) {
          this.tablero.set(null);
          this.error.set(resultado.error);
          return;
        }
        this.error.set(null);
        this.zonas.set(resultado.zonas);
        this.tablero.set(resultado.tablero);
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
      this.tablero.set(null);
      this.error.set(null);
    }
  }
}
