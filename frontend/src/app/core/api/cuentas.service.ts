import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import { Cuenta, PoliticaDatos, RegistroRequest } from './modelos-nucleo';

/**
 * Cuentas del núcleo (módulo 1): registro, política de datos y, en los siguientes bloques, recuperar la contraseña, los
 * datos personales y la supresión. El inicio y cierre de sesión están en `SesionService`.
 */
@Injectable({ providedIn: 'root' })
export class CuentasService {
  private readonly http = inject(HttpClient);

  /** La versión de la política que hay que aceptar para registrarse (RF-09, RF-10). */
  politicaVigente(): Observable<PoliticaDatos> {
    return this.http.get<PoliticaDatos>(`${API_NUCLEO}/politica-datos/vigente`);
  }

  /** El texto completo de una versión de la política, en Markdown (RF-09, RF-11). */
  textoPolitica(version: string): Observable<string> {
    return this.http.get(`${API_NUCLEO}/politica-datos/${encodeURIComponent(version)}`, {
      responseType: 'text',
    });
  }

  /** Un turista o un prestador crea su cuenta (RF-01 a RF-03, RF-09 a RF-11). */
  registrar(solicitud: RegistroRequest): Observable<Cuenta> {
    return this.http.post<Cuenta>(`${API_NUCLEO}/cuentas/registro`, solicitud);
  }
}
