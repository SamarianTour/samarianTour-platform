import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import { IntentoPago, SesionCobro } from './modelos-nucleo';

const BASE = `${API_NUCLEO}/turista/solicitudes`;

/**
 * Pagos de las solicitudes del turista (módulo de pagos, RF-46 a RF-52). Aquí nunca viajan datos de tarjeta ni de cuenta
 * (RF-47): el pago ocurre solo en la pasarela y el turista vuelve a la pantalla de su solicitud. El estado del pago lo decide
 * únicamente el servidor, con el evento de la pasarela (RF-50, RF-51); la redirección del navegador no prueba nada.
 */
@Injectable({ providedIn: 'root' })
export class PagosService {
  private readonly http = inject(HttpClient);

  /**
   * Abre el pago de una solicitud confirmada o con el pago fallido y devuelve cómo abrirlo (`checkout.url`). Si ya hay un
   * intento en curso devuelve ese mismo, así que repetir la llamada no cobra dos veces. Lleva una `Idempotency-Key`: la misma
   * clave devuelve la respuesta original.
   */
  abrir(solicitudId: string, clave: string): Observable<SesionCobro> {
    return this.http.post<SesionCobro>(`${BASE}/${encodeURIComponent(solicitudId)}/pago`, null, {
      headers: { 'Idempotency-Key': clave },
    });
  }

  /** Los intentos de pago de la solicitud, del primero al último, con su estado (RF-52). La de otro turista responde 404. */
  intentos(solicitudId: string): Observable<IntentoPago[]> {
    return this.http.get<IntentoPago[]>(`${BASE}/${encodeURIComponent(solicitudId)}/pagos`);
  }
}
