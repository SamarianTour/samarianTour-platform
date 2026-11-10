import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { aErrorApi } from '../errores/errores.interceptor';
import { API_CATALOGO } from './api.config';
import { PerfilPublico, PerfilRequest } from './modelos';

const BASE = `${API_CATALOGO}/prestador/perfil`;

/**
 * El perfil público del prestador y sus fotos (RF-64, RF-72), en el catálogo. Se publica de inmediato, sin moderación (DC-39). La
 * identidad no se pasa aquí: el interceptor de sesión agrega el token a cada petición.
 */
@Injectable({ providedIn: 'root' })
export class PerfilPrestadorService {
  private readonly http = inject(HttpClient);

  /** Su perfil tal como lo ve el turista. Devuelve `null` si todavía no lo creó (`PERFIL_NO_ENCONTRADO`). */
  consultar(): Observable<PerfilPublico | null> {
    return this.http.get<PerfilPublico>(BASE).pipe(
      catchError((fallo: unknown) =>
        aErrorApi(fallo).codigo === 'PERFIL_NO_ENCONTRADO' ? of(null) : throwError(() => fallo),
      ),
    );
  }

  /** Crea el perfil la primera vez y después reemplaza sus datos; las fotos que ya tiene se conservan. */
  guardar(perfil: PerfilRequest): Observable<PerfilPublico> {
    return this.http.put<PerfilPublico>(BASE, perfil);
  }

  /**
   * Sube una foto al perfil (JPG, PNG o WebP de hasta 5 MB; como máximo 6). Responde con el perfil entero y la foto nueva al final.
   * Hay que crear el perfil antes. Va como multipart con el campo `foto`.
   */
  subirFoto(archivo: File): Observable<PerfilPublico> {
    const cuerpo = new FormData();
    cuerpo.append('foto', archivo);
    return this.http.post<PerfilPublico>(`${BASE}/fotos`, cuerpo);
  }

  /** Quita una foto del perfil y la borra del almacenamiento. Quitarla dos veces no es un error. */
  quitarFoto(fotoId: string): Observable<void> {
    return this.http.delete<void>(`${BASE}/fotos/${encodeURIComponent(fotoId)}`);
  }
}
