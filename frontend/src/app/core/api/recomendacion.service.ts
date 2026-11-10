import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { conIdioma, parametrosConIdioma } from '../idioma/idioma-api';
import { API_RECOMENDACION } from './api.config';
import {
  LIMITE_ALERTAS_PAGINA_MS,
  LIMITE_RECOMENDAR_MS,
  LIMITE_SECCION_MS,
  Resultado,
  conDegradacion,
} from './degradacion';
import {
  Alternativas,
  AlertasZona,
  ConsultaRecomendacion,
  Recomendaciones,
  ResumenAlertas,
} from './modelos-recomendacion';

/**
 * El servicio de recomendación (RF-26 a RF-29): recomendaciones explicadas, alertas del destino y alternativas a una oferta de una
 * zona con alta afluencia. Es público: no exige sesión. Es opcional (ADR-04): cada consulta tiene un límite de tiempo y devuelve un
 * `Resultado`, que dice si el servicio no está disponible en vez de fallar, para que la pantalla oculte su sección o avise y el
 * resto siga funcionando. Todas piden las explicaciones en el idioma vigente de la interfaz.
 */
@Injectable({ providedIn: 'root' })
export class RecomendacionService {
  private readonly http = inject(HttpClient);

  /**
   * De 3 a 5 ofertas visibles del catálogo con la explicación de cada una, o de 0 a 2 con el aviso de pocas coincidencias. Es POST
   * y no GET para que lo que escribe el turista no quede en la dirección ni en los registros (DR-09).
   */
  recomendar(
    consulta: ConsultaRecomendacion,
    limiteMs: number = LIMITE_RECOMENDAR_MS,
  ): Observable<Resultado<Recomendaciones>> {
    return this.http
      .post<Recomendaciones>(`${API_RECOMENDACION}/recomendaciones`, conIdioma(consulta))
      .pipe(conDegradacion(limiteMs));
  }

  /** Todas las alertas activas, agrupadas por zona (solo las zonas que tienen alguna), para la página de alertas del destino. */
  alertas(limiteMs: number = LIMITE_ALERTAS_PAGINA_MS): Observable<Resultado<ResumenAlertas>> {
    return this.http
      .get<ResumenAlertas>(`${API_RECOMENDACION}/alertas`, { params: parametrosConIdioma() })
      .pipe(conDegradacion(limiteMs));
  }

  /** Las alertas activas de una zona; la lista vacía si no tiene. Para la sección de la ficha de una oferta. */
  alertasDeZona(zonaId: string, limiteMs: number = LIMITE_SECCION_MS): Observable<Resultado<AlertasZona>> {
    return this.http
      .get<AlertasZona>(`${API_RECOMENDACION}/zonas/${encodeURIComponent(zonaId)}/alertas`, {
        params: parametrosConIdioma(),
      })
      .pipe(conDegradacion(limiteMs));
  }

  /**
   * De 2 a 3 ofertas parecidas en zonas sin alta afluencia, para la ficha de una oferta de una zona con alta afluencia. Si la zona
   * de la oferta no es de alta afluencia responde `aplica: false` y la ficha no muestra la sección.
   */
  alternativas(ofertaId: string, limiteMs: number = LIMITE_SECCION_MS): Observable<Resultado<Alternativas>> {
    return this.http
      .get<Alternativas>(`${API_RECOMENDACION}/ofertas/${encodeURIComponent(ofertaId)}/alternativas`, {
        params: parametrosConIdioma(),
      })
      .pipe(conDegradacion(limiteMs));
  }
}
