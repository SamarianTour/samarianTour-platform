import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, expand, map, reduce } from 'rxjs';
import { API_CATALOGO } from './api.config';
import { OfertaPrestador, OfertaRequest, PaginaOfertasPrestador } from './modelos';

/** Tamaño máximo de página que acepta la API (DC-14). */
const TAMANO_MAXIMO = 100;

/**
 * Ofertas del prestador: listar las propias, publicar, editar y retirar.
 * La identidad no se pasa aquí: la agrega el interceptor de identidad.
 */
@Injectable({ providedIn: 'root' })
export class OfertasPrestadorService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_CATALOGO}/prestador/ofertas`;

  listar(pagina: number, tamano: number): Observable<PaginaOfertasPrestador> {
    return this.http.get<PaginaOfertasPrestador>(this.url, { params: { page: pagina, size: tamano } });
  }

  /** Todas las ofertas propias, recorriendo las páginas. */
  todas(): Observable<OfertaPrestador[]> {
    return this.listar(0, TAMANO_MAXIMO).pipe(
      expand((pagina) =>
        pagina.pagina + 1 < pagina.totalPaginas ? this.listar(pagina.pagina + 1, TAMANO_MAXIMO) : EMPTY,
      ),
      reduce((ofertas, pagina) => [...ofertas, ...pagina.contenido], [] as OfertaPrestador[]),
    );
  }

  /**
   * [SUPUESTO] La API no tiene un GET de una sola oferta propia: se vuelve a pedir la lista y se busca por id.
   * Devuelve undefined si la oferta no es del prestador.
   */
  buscarPropia(id: string): Observable<OfertaPrestador | undefined> {
    return this.todas().pipe(map((ofertas) => ofertas.find((oferta) => oferta.id === id)));
  }

  publicar(oferta: OfertaRequest): Observable<OfertaPrestador> {
    return this.http.post<OfertaPrestador>(this.url, oferta);
  }

  editar(id: string, oferta: OfertaRequest): Observable<OfertaPrestador> {
    return this.http.put<OfertaPrestador>(`${this.url}/${encodeURIComponent(id)}`, oferta);
  }

  retirar(id: string): Observable<void> {
    return this.http.patch<void>(`${this.url}/${encodeURIComponent(id)}/retiro`, null);
  }
}
