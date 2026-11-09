import { Rol } from '../api/modelos-nucleo';
import { TipoPrestador } from '../api/modelos';

/** Lo que el token de acceso del núcleo declara sobre quien lo tiene (nucleo.openapi.yaml, tokenAcceso). */
export interface Claims {
  /** El usuario. */
  readonly sub: string;
  readonly rol: Rol;
  /** La sesión. */
  readonly sid?: string;
  /** Solo el del prestador que ya registró su RNT. */
  readonly tipoPrestador?: TipoPrestador;
  /** Cuándo vence, en segundos desde 1970. */
  readonly exp?: number;
}

/**
 * Lee las declaraciones del token para pintar la interfaz, por ejemplo el tipo de prestador, que la cuenta no trae.
 * NO valida la firma: eso lo hace el servidor en cada petición. Devuelve null si el token no tiene la forma esperada.
 */
export function leerClaims(token: string): Claims | null {
  const partes = token.split('.');
  if (partes.length !== 3) {
    return null;
  }
  try {
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')), (c) =>
      c.charCodeAt(0),
    );
    const claims: unknown = JSON.parse(new TextDecoder().decode(bytes));
    return esClaims(claims) ? claims : null;
  } catch {
    return null;
  }
}

function esClaims(valor: unknown): valor is Claims {
  if (valor === null || typeof valor !== 'object') {
    return false;
  }
  const { sub, rol } = valor as Record<string, unknown>;
  return typeof sub === 'string' && typeof rol === 'string';
}
