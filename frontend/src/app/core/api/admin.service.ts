import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_NUCLEO } from './api.config';
import {
  AlertaSesgo,
  AvisoBandeja,
  Comision,
  CuentaInterna,
  CuentaInternaRequest,
  DecisionModeracion,
  DecisionOfertaRequest,
  DecisionOpinionRequest,
  EventoPagoFallido,
  PagoTardio,
  PrestadorAdmin,
  SincronizacionRnt,
} from './modelos-nucleo';

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

  /** Las cuentas de administradores, operadores y gestores, activas y desactivadas, de la más antigua a la más reciente (RF-81). */
  cuentasInternas(): Observable<CuentaInterna[]> {
    return this.http.get<CuentaInterna[]>(`${BASE}/cuentas-internas`);
  }

  /**
   * Los avisos al catálogo que el catálogo rechazó, del más reciente al más antiguo, con lo que respondió (DN-06, DN-44). Un aviso que el
   * catálogo responde con un 4xx queda RECHAZADO y no se reintenta solo, porque fallaría igual: hay que corregir la causa y reenviarlo.
   */
  avisosRechazados(): Observable<AvisoBandeja[]> {
    return this.http.get<AvisoBandeja[]>(`${BASE}/bandeja/rechazados`);
  }

  /**
   * Reenvía un aviso rechazado, después de corregir la causa (DN-44). Queda pendiente, con los intentos en cero, y la bandeja lo envía en
   * los siguientes 15 segundos. 404 `AVISO_NO_ENCONTRADO` si no existe; 409 `AVISO_NO_RECHAZADO` si ya no está rechazado.
   */
  reenviarAviso(idAviso: string): Observable<AvisoBandeja> {
    return this.http.post<AvisoBandeja>(`${BASE}/bandeja/${encodeURIComponent(idAviso)}/reenvio`, null);
  }

  /**
   * Los eventos de la pasarela que fallaron cinco veces al procesarse, del recibido más reciente al más antiguo (DN-21, DN-44). Salen
   * de la cola y se avisa al administrador por WhatsApp; nunca traen datos del turista.
   */
  eventosPagoFallidos(): Observable<EventoPagoFallido[]> {
    return this.http.get<EventoPagoFallido[]>(`${BASE}/pagos/eventos-fallidos`);
  }

  /**
   * Devuelve un evento fallido a la cola (DN-44): queda pendiente, con los fallos en cero, y la tarea de eventos lo procesa en los
   * siguientes 5 segundos. Si vuelve a fallar cinco veces, se avisa otra vez. 404 `EVENTO_NO_ENCONTRADO`; 409 `EVENTO_NO_FALLIDO`.
   */
  reintentarEventoFallido(eventoId: string): Observable<EventoPagoFallido> {
    return this.http.post<EventoPagoFallido>(
      `${BASE}/pagos/eventos-fallidos/${encodeURIComponent(eventoId)}/reintento`,
      null,
    );
  }

  /** La comisión vigente (el porcentaje del precio base que se descuenta al prestador, RF-53) y su historial (RF-82). */
  comision(): Observable<Comision> {
    return this.http.get<Comision>(`${BASE}/comision`);
  }

  /**
   * Cambia el porcentaje de comisión (RF-82): de 0 a menos de 100, con hasta dos decimales. Rige para los pagos que se confirmen desde
   * ahora; los ya confirmados conservan el porcentaje que se les aplicó, sin efecto retroactivo (RF-83). Devuelve la nueva vigente.
   */
  cambiarComision(porcentaje: number): Observable<Comision> {
    return this.http.put<Comision>(`${BASE}/comision`, { porcentaje });
  }

  /**
   * Los pagos que la pasarela aprobó pero no pagaron la solicitud (llegaron con ella vencida, cerrada o pagada, o por otro monto), del
   * más reciente al más antiguo. La solicitud no cambia y el administrador los reembolsa a mano, fuera del sistema (RF-49, RF-50).
   */
  pagosTardios(): Observable<PagoTardio[]> {
    return this.http.get<PagoTardio[]>(`${BASE}/pagos-tardios`);
  }

  /**
   * Crea la cuenta de un administrador, un operador de disponibilidad o un gestor (RF-01, RF-81). Nace activa y sin contraseña: le
   * llega un correo con un enlace para ponerla, que vence a las 72 horas y sirve una sola vez (DN-40). La cuenta y el correo van
   * en la misma transacción: si el correo falla (503 `CORREO_NO_DISPONIBLE`) no queda nada y se puede reintentar.
   */
  crearCuentaInterna(cuenta: CuentaInternaRequest): Observable<CuentaInterna> {
    return this.http.post<CuentaInterna>(`${BASE}/cuentas-internas`, cuenta);
  }

  /**
   * Da de baja una cuenta interna (RF-81): ya no inicia sesión y se cierran sus sesiones; no se borra nada y se puede reactivar. A un
   * operador con prestadores asignados primero hay que reasignárselos (409 `OPERADOR_CON_PRESTADORES`).
   */
  desactivarCuentaInterna(usuarioId: string): Observable<CuentaInterna> {
    return this.http.post<CuentaInterna>(
      `${BASE}/cuentas-internas/${encodeURIComponent(usuarioId)}/desactivacion`,
      null,
    );
  }

  /** Vuelve a activar una cuenta desactivada (RF-81). Conserva su contraseña; un operador reactivado no recupera sus prestadores. */
  reactivarCuentaInterna(usuarioId: string): Observable<CuentaInterna> {
    return this.http.post<CuentaInterna>(
      `${BASE}/cuentas-internas/${encodeURIComponent(usuarioId)}/reactivacion`,
      null,
    );
  }

  /**
   * Asigna el operador de disponibilidad que atiende al prestador (RF-32, RF-81). Rige para las solicitudes que se envíen desde
   * ahora; las abiertas siguen con el operador que tenían. Devuelve el prestador con su operador.
   */
  asignarOperador(prestadorId: string, operadorId: string): Observable<PrestadorAdmin> {
    return this.http.put<PrestadorAdmin>(
      `${BASE}/prestadores/${encodeURIComponent(prestadorId)}/operador`,
      { operadorId },
    );
  }

  /**
   * Ejecuta ahora la sincronización con el RNT (RF-76 a RF-78). El núcleo responde cuando termina, así que puede tardar. Este
   * servicio no le pone ningún tiempo máximo: cortar la petición no detiene la sincronización, solo se perdería su resultado.
   */
  sincronizarRnt(): Observable<SincronizacionRnt> {
    return this.http.post<SincronizacionRnt>(`${BASE}/rnt/sincronizacion`, null);
  }

  /**
   * Aprueba o rechaza la versión de una oferta que está en moderación (RF-79). El núcleo verifica con el catálogo que esa versión
   * siga en moderación, guarda la decisión y la envía al catálogo por la bandeja de salida, que la aplica en los siguientes
   * segundos. Repetir la misma decisión responde 200; tomar la contraria, 409 `MODERACION_YA_DECIDIDA`.
   */
  moderarOferta(ofertaId: string, decision: DecisionOfertaRequest): Observable<DecisionModeracion> {
    return this.http.post<DecisionModeracion>(
      `${BASE}/moderacion/ofertas/${encodeURIComponent(ofertaId)}/decision`,
      decision,
    );
  }

  /** Aprueba o rechaza una opinión en moderación (RF-80); como la oferta, pero una opinión no tiene versiones. */
  moderarOpinion(opinionId: string, decision: DecisionOpinionRequest): Observable<DecisionModeracion> {
    return this.http.post<DecisionModeracion>(
      `${BASE}/moderacion/opiniones/${encodeURIComponent(opinionId)}/decision`,
      decision,
    );
  }

  /** Los prestadores que la recomendación muestra más de lo que les corresponde (RET-02): solo las alertas abiertas, de mayor a menor índice. */
  alertasSesgo(): Observable<AlertaSesgo[]> {
    return this.http.get<AlertaSesgo[]>(`${BASE}/alertas-sesgo`);
  }
}
