import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { aErrorApi } from '../errores/errores.interceptor';
import { API_NUCLEO } from './api.config';
import { PrestadorPropio } from './modelos-nucleo';

const BASE = `${API_NUCLEO}/prestador`;

/**
 * Lo que el prestador hace en el núcleo (módulo de prestadores, RF-62 a RF-75): su registro con el RNT, el WhatsApp de contacto y
 * la cuenta de cobro. Todas las rutas exigen la sesión de un prestador; el interceptor de sesión agrega el token. Su catálogo
 * (ofertas, perfil público y fotos) está en `OfertasPrestadorService` y `PerfilPrestadorService`.
 */
@Injectable({ providedIn: 'root' })
export class PrestadorService {
  private readonly http = inject(HttpClient);

  /**
   * Sus datos y el estado de su RNT con la fecha de la última sincronización (RF-73). Devuelve `null` si todavía no registró el
   * RNT (`PRESTADOR_NO_REGISTRADO`): no es un fallo, es el primer paso que le falta.
   */
  registro(): Observable<PrestadorPropio | null> {
    return this.http.get<PrestadorPropio>(`${BASE}/registro`).pipe(
      catchError((fallo: unknown) =>
        aErrorApi(fallo).codigo === 'PRESTADOR_NO_REGISTRADO' ? of(null) : throwError(() => fallo),
      ),
    );
  }
}
