import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, expand, reduce } from 'rxjs';
import { API_CATALOGO } from './api.config';
import { Foto, OfertaPrestador, OfertaPropia, OfertaRequest, PaginaOfertasPrestador } from './modelos';

/** Tamaño máximo de página que acepta la API (DC-14). */
const TAMANO_MAXIMO = 100;

/**
 * Ofertas del prestador: listar las propias, publicar, editar y retirar.
 * La identidad no se pasa aquí: el interceptor de sesión agrega el token a cada petición.
 */
@Injectable({ providedIn: 'root' })
export class OfertasPrestadorService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_CATALOGO}/prestador/ofertas`;
  private readonly urlFotos = `${API_CATALOGO}/prestador/fotos`;

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
   * Una oferta propia con el contenido completo de su versión publicada y de la pendiente (DC-50), en cualquier estado: también la
   * retirada y la que nunca se aprobó. Sirve para revisarla y para corregir una versión rechazada. La de otro prestador responde 403.
   */
  consultar(id: string): Observable<OfertaPropia> {
    return this.http.get<OfertaPropia>(`${this.url}/${encodeURIComponent(id)}`);
  }

  publicar(oferta: OfertaRequest): Observable<OfertaPrestador> {
    return this.http.post<OfertaPrestador>(this.url, oferta);
  }

  editar(id: string, oferta: OfertaRequest): Observable<OfertaPrestador> {
    return this.http.put<OfertaPrestador>(`${this.url}/${encodeURIComponent(id)}`, oferta);
  }

  /**
   * Sube una foto para las ofertas (DC-47). Queda suelta: se pone en una oferta enviando su `id` en `fotos` al
   * publicarla o editarla. Va como multipart con el campo `foto`; el navegador agrega el Content-Type con su límite.
   */
  subirFoto(archivo: File): Observable<Foto> {
    const cuerpo = new FormData();
    cuerpo.append('foto', archivo);
    return this.http.post<Foto>(this.urlFotos, cuerpo);
  }

  retirar(id: string): Observable<void> {
    return this.http.patch<void>(`${this.url}/${encodeURIComponent(id)}/retiro`, null);
  }
}
