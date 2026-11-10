import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_CATALOGO } from './api.config';
import { PaginaOfertasPendientes, PaginaOpinionesPendientes, Zona } from './modelos';

const BASE = `${API_CATALOGO}/admin`;

/**
 * Lo que el administrador lee y escribe directamente en el catálogo, que es su dueño: las colas de moderación (solo lectura; la
 * decisión se toma en el núcleo, `AdminService`) y las zonas. Todas las rutas exigen la sesión de un administrador.
 */
@Injectable({ providedIn: 'root' })
export class AdminCatalogoService {
  private readonly http = inject(HttpClient);

  /** Las ofertas con una versión en moderación, de la enviada hace más tiempo a la más reciente (RF-79). Las páginas empiezan en 0. */
  ofertasEnModeracion(pagina = 0, tamano = 10): Observable<PaginaOfertasPendientes> {
    return this.http.get<PaginaOfertasPendientes>(`${BASE}/moderacion/ofertas`, {
      params: new HttpParams().set('page', pagina).set('size', tamano),
    });
  }

  /** Las opiniones en moderación, de la enviada hace más tiempo a la más reciente (RF-80). Las páginas empiezan en 0. */
  opinionesEnModeracion(pagina = 0, tamano = 10): Observable<PaginaOpinionesPendientes> {
    return this.http.get<PaginaOpinionesPendientes>(`${BASE}/moderacion/opiniones`, {
      params: new HttpParams().set('page', pagina).set('size', tamano),
    });
  }

  /** Todas las zonas, activas e inactivas, por nombre (HU-ADM-06). */
  zonas(): Observable<Zona[]> {
    return this.http.get<Zona[]>(`${BASE}/zonas`);
  }
}
