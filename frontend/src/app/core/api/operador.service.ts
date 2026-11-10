import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import {
  AtencionRequest,
  EstadoSolicitud,
  PaginaAvisos,
  PaginaColaOperador,
  SolicitudOperador,
} from './modelos-nucleo';

const BASE = `${API_NUCLEO}/operador`;

/**
 * Lo que hace el operador de disponibilidad con las solicitudes de sus prestadores (RF-32 a RF-40): su cola, su
 * historial, atender una solicitud y sus avisos. Solo ve las que tiene asignadas; la de otro operador responde 404.
 */
@Injectable({ providedIn: 'root' })
export class OperadorService {
  private readonly http = inject(HttpClient);

  /**
   * Sin `estado`, su cola: las pendientes de la más antigua a la más reciente. Con un `estado`, su historial de
   * trámites en ese estado, de la más reciente a la más antigua. Las páginas empiezan en 0.
   */
  cola(estado?: EstadoSolicitud, pagina = 0, tamano = 20): Observable<PaginaColaOperador> {
    let params = new HttpParams().set('page', pagina).set('size', tamano);
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<PaginaColaOperador>(`${BASE}/solicitudes`, { params });
  }

  /** Una solicitud de su cola, con el WhatsApp del prestador y su historial (RF-37). */
  consultar(id: string): Observable<SolicitudOperador> {
    return this.http.get<SolicitudOperador>(`${BASE}/solicitudes/${encodeURIComponent(id)}`);
  }

  /** La marca Atendida (con precio y condiciones) o No disponible (con su motivo), y devuelve su nuevo estado. */
  atender(id: string, atencion: AtencionRequest): Observable<SolicitudOperador> {
    return this.http.post<SolicitudOperador>(
      `${BASE}/solicitudes/${encodeURIComponent(id)}/atencion`,
      atencion,
    );
  }

  /** Sus avisos, del más reciente al más antiguo (RF-34). */
  avisos(pagina = 0, tamano = 20): Observable<PaginaAvisos> {
    const params = new HttpParams().set('page', pagina).set('size', tamano);
    return this.http.get<PaginaAvisos>(`${BASE}/avisos`, { params });
  }
}
