import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import { DistribucionCategoria, Embudo, Tablero } from './modelos-nucleo';

const BASE = `${API_NUCLEO}/gestor/indicadores`;

/** El periodo de una consulta: dos fechas AAAA-MM-DD, las dos incluidas, en hora de Colombia. */
export interface PeriodoConsulta {
  readonly desde: string;
  readonly hasta: string;
}

/**
 * Los indicadores del gestor del destino (RF-85 a RF-89, módulo de indicadores). Las tres rutas exigen la sesión de un gestor y
 * aceptan los mismos filtros opcionales, `desde` y `hasta`; el núcleo no ofrece otros. Son datos agregados (RET-03): un conteo de 1 o 2
 * no viene en la respuesta, y su porcentaje tampoco.
 */
@Injectable({ providedIn: 'root' })
export class IndicadoresService {
  private readonly http = inject(HttpClient);

  /** La demanda del periodo por zona y tipo de oferta, primero las combinaciones con más solicitudes (RF-85, RF-86). */
  tablero(periodo: PeriodoConsulta): Observable<Tablero> {
    return this.http.get<Tablero>(`${BASE}/tablero`, { params: parametros(periodo) });
  }

  /** Cuántas solicitudes llegan a cada etapa (enviadas, atendidas, confirmadas y pagadas), en total y por tipo de oferta (RF-87, RF-88). */
  embudo(periodo: PeriodoConsulta): Observable<Embudo> {
    return this.http.get<Embudo>(`${BASE}/embudo`, { params: parametros(periodo) });
  }

  /** Cómo se reparte la demanda entre las categorías de prestador: grandes, medianos, pequeños y guías (RF-89). */
  categorias(periodo: PeriodoConsulta): Observable<DistribucionCategoria> {
    return this.http.get<DistribucionCategoria>(`${BASE}/categorias`, { params: parametros(periodo) });
  }
}

function parametros({ desde, hasta }: PeriodoConsulta): HttpParams {
  return new HttpParams().set('desde', desde).set('hasta', hasta);
}
