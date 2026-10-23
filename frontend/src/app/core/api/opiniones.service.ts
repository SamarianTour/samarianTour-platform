import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_CATALOGO } from './api.config';
import { MiOpinion, OpinionRequest, OpinionesOferta, PaginaMisOpiniones } from './modelos';

/**
 * Opiniones: la lectura pública de una oferta y las del turista. La identidad del turista no se pasa aquí: la agrega
 * el interceptor de identidad en las rutas /turista/**.
 */
@Injectable({ providedIn: 'root' })
export class OpinionesService {
  private readonly http = inject(HttpClient);

  /** Las aprobadas de la oferta, de la más reciente a la más antigua, con su resumen (RF-59). */
  deOferta(ofertaId: string, pagina: number, tamano: number): Observable<OpinionesOferta> {
    return this.http.get<OpinionesOferta>(`${API_CATALOGO}/ofertas/${encodeURIComponent(ofertaId)}/opiniones`, {
      params: { page: pagina, size: tamano },
    });
  }

  /** La opinión del turista sobre una oferta que pagó; queda en moderación (RF-60). */
  publicar(opinion: OpinionRequest): Observable<MiOpinion> {
    return this.http.post<MiOpinion>(`${API_CATALOGO}/turista/opiniones`, opinion);
  }

  /** Las del turista en cualquier estado. */
  mias(pagina: number, tamano: number): Observable<PaginaMisOpiniones> {
    return this.http.get<PaginaMisOpiniones>(`${API_CATALOGO}/turista/opiniones`, {
      params: { page: pagina, size: tamano },
    });
  }
}
