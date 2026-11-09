import { HttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';
import {
  Observable,
  catchError,
  defer,
  finalize,
  map,
  of,
  shareReplay,
  tap,
  throwError,
} from 'rxjs';
import { API_NUCLEO } from '../api/api.config';
import { Cuenta, RespuestaSesion } from '../api/modelos-nucleo';
import { ErrorApi } from '../errores/error-api';
import { aErrorApi } from '../errores/errores.interceptor';
import {
  CLAVE_SESION,
  Persistencia,
  SesionGuardada,
  aSesionGuardada,
  borrarSesion,
  guardarSesion,
  interpretar,
  leerSesion,
} from './almacen-sesion';
import { leerClaims } from './claims';

const BASE = `${API_NUCLEO}/cuentas/sesion`;

/** El token de acceso se renueva con este margen antes de vencer, para que ninguna petición salga con uno vencido. */
export const MARGEN_RENOVACION_MS = 60_000;

/** Espera mínima entre renovaciones, para no renovar sin parar si el reloj del servidor difiere mucho del nuestro. */
const ESPERA_MINIMA_MS = 5_000;

/**
 * La sesión del usuario (RF-04, RF-06): quién es, con qué rol y, si es prestador con RNT, de qué tipo. Inicia sesión
 * con POST /cuentas/sesion, la renueva sola antes de que el token de acceso venza (15 minutos) y la cierra con
 * POST /cuentas/sesion/cierre. Se guarda en el almacenamiento del navegador para sobrevivir a recargar la página.
 *
 * El token de renovación sirve una sola vez, y usar uno ya gastado cierra la sesión (DN-29): por eso hay una sola
 * renovación a la vez, aunque la pidan varias peticiones, y las pestañas se avisan entre sí de los tokens nuevos.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly http = inject(HttpClient);
  private readonly ventana = inject(DOCUMENT).defaultView;

  private readonly guardada = leerSesion();
  private readonly actual = signal<SesionGuardada | null>(this.guardada?.sesion ?? null);
  private persistencia: Persistencia = this.guardada?.persistencia ?? 'sesion';
  private temporizador: ReturnType<typeof setTimeout> | undefined;
  private renovacionEnCurso: Observable<string> | null = null;

  readonly cuenta = computed<Cuenta | null>(() => this.actual()?.cuenta ?? null);
  readonly autenticada = computed(() => this.actual() !== null);
  readonly rol = computed(() => this.cuenta()?.rol ?? null);
  readonly tokenAcceso = computed(() => this.actual()?.tokenAcceso ?? null);
  /** Solo el prestador que ya registró su RNT lo tiene: viene en el token, no en la cuenta. */
  readonly tipoPrestador = computed(() => {
    const token = this.tokenAcceso();
    return (token && leerClaims(token)?.tipoPrestador) || null;
  });

  constructor() {
    this.ventana?.addEventListener('storage', (evento) => this.alCambiarOtraPestana(evento));
  }

  /**
   * Inicia sesión. Con `recordar` la sesión queda en localStorage hasta que la renovación venza (7 días); sin él, en
   * sessionStorage: sobrevive a recargar, pero se pierde al cerrar la pestaña.
   */
  iniciarSesion(correo: string, contrasena: string, recordar: boolean): Observable<Cuenta> {
    return this.http.post<RespuestaSesion>(BASE, { correo, contrasena }).pipe(
      tap((respuesta) =>
        this.establecer(aSesionGuardada(respuesta), recordar ? 'local' : 'sesion'),
      ),
      map((respuesta) => respuesta.cuenta),
    );
  }

  /**
   * Pide tokens nuevos con el de renovación y devuelve el token de acceso nuevo. Si ya hay una renovación en curso,
   * comparte su resultado. Si el servidor responde 401, la sesión ya no sirve y se descarta.
   */
  renovar(): Observable<string> {
    if (this.renovacionEnCurso) {
      return this.renovacionEnCurso;
    }
    const sesion = this.actual();
    if (!sesion) {
      return throwError(() => new ErrorApi(401, 'SESION_INVALIDA', 'errores.codigo.SESION_INVALIDA'));
    }
    // Otra pestaña puede haber renovado ya: su token es el vigente y el nuestro, que sirvió una vez, ya no.
    const deOtraPestana = leerSesion();
    if (deOtraPestana && deOtraPestana.sesion.tokenRenovacion !== sesion.tokenRenovacion) {
      this.persistencia = deOtraPestana.persistencia;
      this.adoptar(deOtraPestana.sesion);
      return of(deOtraPestana.sesion.tokenAcceso);
    }

    const solicitud = this.http
      .post<RespuestaSesion>(`${BASE}/renovacion`, { tokenRenovacion: sesion.tokenRenovacion })
      .pipe(
        tap((respuesta) => this.establecer(aSesionGuardada(respuesta), this.persistencia)),
        map((respuesta) => respuesta.tokenAcceso),
        catchError((error: unknown) => {
          // Sin conexión (estado 0) la sesión sigue siendo buena: solo un 401 la invalida.
          if (aErrorApi(error).estado === 401) {
            this.descartarSesion();
          }
          return throwError(() => error);
        }),
        finalize(() => (this.renovacionEnCurso = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    this.renovacionEnCurso = solicitud;
    return solicitud;
  }

  /**
   * Se llama al arrancar la aplicación con una sesión guardada: si el token de acceso ya venció o está por vencer,
   * lo renueva antes de la primera petición; si no, deja programada la renovación. Nunca falla.
   */
  reanudar(): Observable<void> {
    const sesion = this.actual();
    if (!sesion) {
      return of(undefined);
    }
    if (Date.parse(sesion.accesoExpiraEn) - Date.now() > MARGEN_RENOVACION_MS) {
      this.programar();
      return of(undefined);
    }
    return this.renovar().pipe(
      map(() => undefined),
      catchError(() => of(undefined)),
    );
  }

  /** Cierra la sesión: la local se borra de inmediato y el servidor invalida el token de renovación (RF-06). */
  cerrarSesion(): Observable<void> {
    return defer(() => {
      const sesion = this.actual();
      this.descartarSesion();
      if (!sesion) {
        return of(undefined);
      }
      // Aunque el servidor no responda, la sesión local ya está cerrada.
      return this.http
        .post<void>(`${BASE}/cierre`, { tokenRenovacion: sesion.tokenRenovacion })
        .pipe(
          map(() => undefined),
          catchError(() => of(undefined)),
        );
    });
  }

  /** Olvida la sesión sin avisar al servidor: cuando ya no sirve (401) o cuando el servidor ya la cerró. */
  descartarSesion(): void {
    clearTimeout(this.temporizador);
    this.actual.set(null);
    borrarSesion();
  }

  /** Reemplaza la cuenta guardada, por ejemplo después de editar los datos personales. */
  actualizarCuenta(cuenta: Cuenta): void {
    const sesion = this.actual();
    if (sesion) {
      this.establecer({ ...sesion, cuenta }, this.persistencia);
    }
  }

  private establecer(sesion: SesionGuardada, persistencia: Persistencia): void {
    this.persistencia = persistencia;
    this.actual.set(sesion);
    guardarSesion(sesion, persistencia);
    this.programar();
  }

  /** Toma una sesión que otra pestaña ya guardó: no se vuelve a escribir. */
  private adoptar(sesion: SesionGuardada): void {
    this.actual.set(sesion);
    this.programar();
  }

  private programar(): void {
    clearTimeout(this.temporizador);
    const sesion = this.actual();
    if (!sesion) {
      return;
    }
    const espera = Date.parse(sesion.accesoExpiraEn) - Date.now() - MARGEN_RENOVACION_MS;
    this.temporizador = setTimeout(
      () => this.renovar().subscribe({ error: () => undefined }),
      Math.max(espera, ESPERA_MINIMA_MS),
    );
  }

  /** Una sesión en localStorage se comparte entre pestañas: iniciar o cerrar en una se refleja en las otras. */
  private alCambiarOtraPestana(evento: StorageEvent): void {
    if (evento.key !== CLAVE_SESION || evento.storageArea !== localStorageSeguro()) {
      return;
    }
    if (evento.newValue === null) {
      // Otra pestaña cerró la sesión: el almacenamiento ya está limpio, solo falta soltar la de esta.
      if (this.persistencia === 'local') {
        clearTimeout(this.temporizador);
        this.actual.set(null);
      }
      return;
    }
    // Un valor dañado no es un cierre de sesión: se ignora.
    const nueva = interpretar(evento.newValue);
    if (!nueva) {
      return;
    }
    if (this.persistencia === 'local' || !this.actual()) {
      this.persistencia = 'local';
      this.adoptar(nueva);
    }
  }
}

function localStorageSeguro(): Storage | null {
  try {
    return localStorage;
  } catch {
    return null;
  }
}
