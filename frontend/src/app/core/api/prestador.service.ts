import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { aErrorApi } from '../errores/errores.interceptor';
import { API_NUCLEO } from './api.config';
import { DatosTipoRequest, PrestadorPropio, RegistroPrestadorRequest } from './modelos-nucleo';

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

  /**
   * Registra el RNT y el tipo (RF-62, RF-63, RF-68 a RF-70). El núcleo consulta el RNT en la fuente oficial, que puede tardar
   * unos segundos (si no responde en 3 s queda `VERIFICACION_PENDIENTE`): por eso no se le pone ningún tiempo máximo.
   * Después hay que renovar la sesión para que el token traiga el tipo.
   */
  registrar(cuerpo: RegistroPrestadorRequest): Observable<PrestadorPropio> {
    return this.http.post<PrestadorPropio>(`${BASE}/registro`, cuerpo);
  }

  /** Cambia el número de RNT y lo vuelve a validar. */
  cambiarRnt(numeroRnt: string): Observable<PrestadorPropio> {
    return this.http.put<PrestadorPropio>(`${BASE}/rnt`, { numeroRnt });
  }

  /** Cambia el tipo y sus campos condicionales (RF-68, RF-69). Después hay que renovar la sesión. */
  cambiarTipo(cuerpo: DatosTipoRequest): Observable<PrestadorPropio> {
    return this.http.put<PrestadorPropio>(`${BASE}/tipo`, cuerpo);
  }

  /**
   * Configura el WhatsApp de contacto, que nunca es público (RF-71). Un número nuevo queda sin verificar y el núcleo le envía un
   * código de 6 dígitos que vale 30 minutos y admite 3 intentos.
   */
  configurarWhatsapp(whatsapp: string): Observable<PrestadorPropio> {
    return this.http.put<PrestadorPropio>(`${BASE}/whatsapp`, { whatsapp });
  }

  /** Envía otro código; el anterior deja de servir. Con el número ya verificado no envía nada. */
  enviarCodigoWhatsapp(): Observable<void> {
    return this.http.post<void>(`${BASE}/whatsapp/codigo`, null);
  }

  /** Confirma el WhatsApp con el código y devuelve al prestador con el número verificado. */
  verificarWhatsapp(codigo: string): Observable<PrestadorPropio> {
    return this.http.post<PrestadorPropio>(`${BASE}/whatsapp/verificacion`, { codigo });
  }
}
