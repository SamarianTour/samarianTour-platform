import { Cuenta, RespuestaSesion } from '../api/modelos-nucleo';

/** Dónde se guarda la sesión: `sesion` dura mientras la pestaña esté abierta; `local`, hasta que la renovación venza. */
export type Persistencia = 'sesion' | 'local';

/** Lo que se guarda de la sesión: los dos tokens, cuándo vencen y la cuenta. */
export interface SesionGuardada {
  readonly tokenAcceso: string;
  readonly accesoExpiraEn: string;
  readonly tokenRenovacion: string;
  readonly renovacionExpiraEn: string;
  readonly cuenta: Cuenta;
}

export const CLAVE_SESION = 'samarian-tour.sesion';

export function aSesionGuardada(respuesta: RespuestaSesion): SesionGuardada {
  const { tokenAcceso, accesoExpiraEn, tokenRenovacion, renovacionExpiraEn, cuenta } = respuesta;
  return { tokenAcceso, accesoExpiraEn, tokenRenovacion, renovacionExpiraEn, cuenta };
}

/** El almacenamiento puede faltar o fallar (modo privado, datos bloqueados): en ese caso la sesión dura la página. */
function almacen(persistencia: Persistencia): Storage | null {
  try {
    return persistencia === 'local' ? localStorage : sessionStorage;
  } catch {
    return null;
  }
}

/** Lo guardado, con la persistencia donde estaba. Descarta lo que esté dañado o ya no pueda renovarse. */
export function leerSesion(
  ahora = Date.now(),
): { sesion: SesionGuardada; persistencia: Persistencia } | null {
  for (const persistencia of ['local', 'sesion'] as const) {
    const sesion = interpretar(leerTexto(persistencia));
    if (sesion && Date.parse(sesion.renovacionExpiraEn) > ahora) {
      return { sesion, persistencia };
    }
  }
  borrarSesion();
  return null;
}

export function guardarSesion(sesion: SesionGuardada, persistencia: Persistencia): void {
  try {
    almacen(persistencia)?.setItem(CLAVE_SESION, JSON.stringify(sesion));
    // Solo en un lugar, para que cerrar la sesión no deje una copia en el otro.
    almacen(persistencia === 'local' ? 'sesion' : 'local')?.removeItem(CLAVE_SESION);
  } catch {
    // Sin almacenamiento, la sesión dura lo que dure la página.
  }
}

export function borrarSesion(): void {
  for (const persistencia of ['local', 'sesion'] as const) {
    try {
      almacen(persistencia)?.removeItem(CLAVE_SESION);
    } catch {
      // Nada que borrar.
    }
  }
}

/** Interpreta el texto de otra pestaña (evento `storage`); null si no es una sesión válida. */
export function interpretar(texto: string | null): SesionGuardada | null {
  if (!texto) {
    return null;
  }
  try {
    const valor: unknown = JSON.parse(texto);
    return esSesionGuardada(valor) ? valor : null;
  } catch {
    return null;
  }
}

function leerTexto(persistencia: Persistencia): string | null {
  try {
    return almacen(persistencia)?.getItem(CLAVE_SESION) ?? null;
  } catch {
    return null;
  }
}

function esSesionGuardada(valor: unknown): valor is SesionGuardada {
  if (valor === null || typeof valor !== 'object') {
    return false;
  }
  const { tokenAcceso, accesoExpiraEn, tokenRenovacion, renovacionExpiraEn, cuenta } =
    valor as Record<string, unknown>;
  return (
    typeof tokenAcceso === 'string' &&
    typeof accesoExpiraEn === 'string' &&
    typeof tokenRenovacion === 'string' &&
    typeof renovacionExpiraEn === 'string' &&
    cuenta !== null &&
    typeof cuenta === 'object' &&
    typeof (cuenta as Record<string, unknown>)['id'] === 'string' &&
    typeof (cuenta as Record<string, unknown>)['rol'] === 'string'
  );
}
