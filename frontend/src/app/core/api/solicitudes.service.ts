import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import {
  DecisionCondiciones,
  EnviarSolicitudRequest,
  PaginaSolicitudesTurista,
  SolicitudTurista,
} from './modelos-nucleo';

const BASE = `${API_NUCLEO}/turista/solicitudes`;

/**
 * Solicitudes de disponibilidad del turista (módulo 4, RF-30 a RF-34, RF-43). Enviar y responder son idempotentes: llevan
 * una `Idempotency-Key` que se repite en cada reintento de la misma operación (ver `ClaveDeIdempotencia`).
 */
@Injectable({ providedIn: 'root' })
export class SolicitudesService {
  private readonly http = inject(HttpClient);

  /** Pide disponibilidad sobre una oferta. Queda Pendiente, con 24 h para que el operador la atienda. */
  enviar(solicitud: EnviarSolicitudRequest, clave: string): Observable<SolicitudTurista> {
    return this.http.post<SolicitudTurista>(BASE, solicitud, {
      headers: { 'Idempotency-Key': clave },
    });
  }

  /** Mis solicitudes, de la más reciente a la más antigua. Las páginas empiezan en 0. */
  listar(pagina = 0, tamano = 20): Observable<PaginaSolicitudesTurista> {
    const params = new HttpParams().set('page', pagina).set('size', tamano);
    return this.http.get<PaginaSolicitudesTurista>(BASE, { params });
  }

  /** El estado real de una solicitud, con su historial y su plazo vigente. La de otro turista responde 404. */
  consultar(id: string): Observable<SolicitudTurista> {
    return this.http.get<SolicitudTurista>(`${BASE}/${encodeURIComponent(id)}`);
  }

  /** Confirma o declina las condiciones de una solicitud atendida, dentro de la ventana de confirmación (RF-43). */
  responder(
    id: string,
    decision: DecisionCondiciones,
    clave: string,
  ): Observable<SolicitudTurista> {
    return this.http.post<SolicitudTurista>(
      `${BASE}/${encodeURIComponent(id)}/respuesta`,
      { decision },
      { headers: { 'Idempotency-Key': clave } },
    );
  }
}
