import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_CATALOGO } from './api.config';
import { PaginaOfertasPendientes, PaginaOpinionesPendientes, Zona, ZonaEdicion, ZonaRequest } from './modelos';

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

  /**
   * Registra una zona, que nace activa (HU-ADM-06, RF-84). El administrador escribe directo en el catálogo, que es el dueño de las
   * zonas (DC-49). Una zona nunca se elimina: se inactiva. 409 `ZONA_DUPLICADA` si el nombre ya existe, sin distinguir mayúsculas,
   * tildes ni espacios repetidos.
   */
  registrarZona(zona: ZonaRequest): Observable<Zona> {
    return this.http.post<Zona>(`${BASE}/zonas`, zona);
  }

  /**
   * Reemplaza todos los datos de la zona: lo que no se envía queda vacío, y las marcas que no se envían, en false. `activa` es
   * obligatoria: en false inactiva la zona (sale de la lista pública y no admite ofertas nuevas ni ediciones, pero sus ofertas siguen
   * visibles) y en true la reactiva. 404 `ZONA_NO_ENCONTRADA`; 409 `ZONA_DUPLICADA`.
   */
  editarZona(zonaId: string, zona: ZonaEdicion): Observable<Zona> {
    return this.http.put<Zona>(`${BASE}/zonas/${encodeURIComponent(zonaId)}`, zona);
  }

  /** Todas las zonas, activas e inactivas, por nombre (HU-ADM-06). */
  zonas(): Observable<Zona[]> {
    return this.http.get<Zona[]>(`${BASE}/zonas`);
  }
}
