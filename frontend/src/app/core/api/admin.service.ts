import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import {
  AlertaSesgo,
  CuentaInterna,
  DecisionModeracion,
  DecisionOfertaRequest,
  DecisionOpinionRequest,
  PrestadorAdmin,
  SincronizacionRnt,
} from './modelos-nucleo';

const BASE = `${API_NUCLEO}/admin`;

/**
 * Lo que el administrador hace en el núcleo (módulo de administración, RF-76 a RF-84, RET-02). Todas las rutas exigen la sesión
 * de un administrador. Las zonas y las colas de moderación están en el catálogo (`AdminCatalogoService`).
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  /** Los prestadores y su operador, del registrado hace más tiempo al más reciente; con `sinOperador`, solo los que faltan por asignar. */
  prestadores(sinOperador = false): Observable<PrestadorAdmin[]> {
    let params = new HttpParams();
    if (sinOperador) {
      params = params.set('sinOperador', true);
    }
    return this.http.get<PrestadorAdmin[]>(`${BASE}/prestadores`, { params });
  }

  /** Las cuentas de administradores, operadores y gestores, activas y desactivadas, de la más antigua a la más reciente (RF-81). */
  cuentasInternas(): Observable<CuentaInterna[]> {
    return this.http.get<CuentaInterna[]>(`${BASE}/cuentas-internas`);
  }

  /**
   * Asigna el operador de disponibilidad que atiende al prestador (RF-32, RF-81). Rige para las solicitudes que se envíen desde
   * ahora; las abiertas siguen con el operador que tenían. Devuelve el prestador con su operador.
   */
  asignarOperador(prestadorId: string, operadorId: string): Observable<PrestadorAdmin> {
    return this.http.put<PrestadorAdmin>(
      `${BASE}/prestadores/${encodeURIComponent(prestadorId)}/operador`,
      { operadorId },
    );
  }

  /**
   * Ejecuta ahora la sincronización con el RNT (RF-76 a RF-78). El núcleo responde cuando termina, así que puede tardar. Este
   * servicio no le pone ningún tiempo máximo: cortar la petición no detiene la sincronización, solo se perdería su resultado.
   */
  sincronizarRnt(): Observable<SincronizacionRnt> {
    return this.http.post<SincronizacionRnt>(`${BASE}/rnt/sincronizacion`, null);
  }

  /**
   * Aprueba o rechaza la versión de una oferta que está en moderación (RF-79). El núcleo verifica con el catálogo que esa versión
   * siga en moderación, guarda la decisión y la envía al catálogo por la bandeja de salida, que la aplica en los siguientes
   * segundos. Repetir la misma decisión responde 200; tomar la contraria, 409 `MODERACION_YA_DECIDIDA`.
   */
  moderarOferta(ofertaId: string, decision: DecisionOfertaRequest): Observable<DecisionModeracion> {
    return this.http.post<DecisionModeracion>(
      `${BASE}/moderacion/ofertas/${encodeURIComponent(ofertaId)}/decision`,
      decision,
    );
  }

  /** Aprueba o rechaza una opinión en moderación (RF-80); como la oferta, pero una opinión no tiene versiones. */
  moderarOpinion(opinionId: string, decision: DecisionOpinionRequest): Observable<DecisionModeracion> {
    return this.http.post<DecisionModeracion>(
      `${BASE}/moderacion/opiniones/${encodeURIComponent(opinionId)}/decision`,
      decision,
    );
  }

  /** Los prestadores que la recomendación muestra más de lo que les corresponde (RET-02): solo las alertas abiertas, de mayor a menor índice. */
  alertasSesgo(): Observable<AlertaSesgo[]> {
    return this.http.get<AlertaSesgo[]>(`${BASE}/alertas-sesgo`);
  }
}
