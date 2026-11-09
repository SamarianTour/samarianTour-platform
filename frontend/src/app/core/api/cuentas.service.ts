import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import { Cuenta, DatosPersonalesRequest, PoliticaDatos, RegistroRequest } from './modelos-nucleo';

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

  /** Los datos personales de quien tiene la sesión, de cualquier rol (RF-12). */
  consultarCuenta(): Observable<Cuenta> {
    return this.http.get<Cuenta>(`${API_NUCLEO}/cuenta`);
  }

  /**
   * Reemplaza el nombre y, el turista, su WhatsApp (RF-12). Un WhatsApp distinto queda sin verificar y recibe un código
   * nuevo (RF-03). El correo no se cambia (DN-31).
   */
  guardarDatos(datos: DatosPersonalesRequest): Observable<Cuenta> {
    return this.http.put<Cuenta>(`${API_NUCLEO}/cuenta`, datos);
  }

  /**
   * El turista elimina sus datos personales, de inmediato y sin vuelta atrás (RF-13, Ley 1581). Confirma con su contraseña
   * actual; un error cuenta para el bloqueo por intentos fallidos (RF-05). Cierra todas sus sesiones.
   */
  suprimirTurista(contrasena: string): Observable<void> {
    return this.http.post<void>(`${API_NUCLEO}/turista/supresion`, { contrasena });
  }

  /**
   * El prestador elimina sus datos personales (RF-13, DN-45). Como el turista, confirma con su contraseña. Si todavía tiene
   * compromisos con turistas o dinero por recibir, el núcleo se niega (409 `PRESTADOR_CON_COMPROMISOS`) y dice cuáles.
   */
  suprimirPrestador(contrasena: string): Observable<void> {
    return this.http.post<void>(`${API_NUCLEO}/prestador/supresion`, { contrasena });
  }

  /** Envía otro código de 6 dígitos al WhatsApp del turista; el anterior deja de servir (RF-03). */
  enviarCodigoWhatsapp(): Observable<void> {
    return this.http.post<void>(`${API_NUCLEO}/turista/whatsapp/codigo`, null);
  }

  /** El turista confirma su WhatsApp con el código (RF-03). Devuelve la cuenta con el número verificado. */
  verificarWhatsapp(codigo: string): Observable<Cuenta> {
    return this.http.post<Cuenta>(`${API_NUCLEO}/turista/whatsapp/verificacion`, { codigo });
  }

  /**
   * Pide el enlace para poner una contraseña nueva (RF-07). Responde igual exista o no la cuenta, para no revelar qué
   * correos están registrados. Un enlace nuevo invalida los anteriores (DN-30).
   */
  solicitarRestablecimiento(correo: string): Observable<void> {
    return this.http.post<void>(`${API_NUCLEO}/cuentas/contrasena/restablecimiento`, { correo });
  }

  /**
   * Pone la contraseña nueva con el `token` del enlace del correo (RF-08). Gasta el enlace y cierra todas las sesiones de
   * la cuenta (DN-30).
   */
  restablecerContrasena(token: string, contrasena: string): Observable<void> {
    return this.http.post<void>(`${API_NUCLEO}/cuentas/contrasena`, { token, contrasena });
  }
}
