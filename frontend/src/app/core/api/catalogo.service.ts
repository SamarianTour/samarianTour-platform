import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_CATALOGO } from './api.config';
import { ConsultaOfertas, FichaOferta, PaginaOfertas, Zona } from './modelos';

/** Catálogo público: búsqueda, ficha y zonas. No exige identidad. */
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);

  buscar(consulta: ConsultaOfertas): Observable<PaginaOfertas> {
    return this.http.get<PaginaOfertas>(`${API_CATALOGO}/ofertas`, { params: parametros(consulta) });
  }

  ficha(id: string): Observable<FichaOferta> {
    return this.http.get<FichaOferta>(`${API_CATALOGO}/ofertas/${encodeURIComponent(id)}`);
  }

  /** La ficha de una zona; también responde por una zona inactiva (DC-49). 404 `ZONA_NO_ENCONTRADA`. */
  zona(id: string): Observable<Zona> {
    return this.http.get<Zona>(`${API_CATALOGO}/zonas/${encodeURIComponent(id)}`);
  }

  zonas(): Observable<Zona[]> {
    return this.http.get<Zona[]>(`${API_CATALOGO}/zonas`);
  }
}

/** Solo viajan los filtros con valor: los demás no se envían. */
function parametros(consulta: ConsultaOfertas): HttpParams {
  let params = new HttpParams();
  for (const [nombre, valor] of Object.entries(consulta)) {
    if (valor !== undefined && valor !== null && valor !== '') {
      params = params.set(nombre, String(valor));
    }
  }
  return params;
}
