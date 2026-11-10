import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import { AlertaSesgo, PrestadorAdmin } from './modelos-nucleo';

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

  /** Los prestadores que la recomendación muestra más de lo que les corresponde (RET-02): solo las alertas abiertas, de mayor a menor índice. */
  alertasSesgo(): Observable<AlertaSesgo[]> {
    return this.http.get<AlertaSesgo[]>(`${BASE}/alertas-sesgo`);
  }
}
