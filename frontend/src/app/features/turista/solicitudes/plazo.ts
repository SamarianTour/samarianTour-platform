/** Con 6 horas o menos para actuar, el plazo se marca como urgente (como en la cola del operador, RF-36). */
export const UMBRAL_URGENTE_MS = 6 * 60 * 60 * 1000;

export type NivelPlazo = 'sin-plazo' | 'vigente' | 'urgente' | 'vencido';

/** Cuánto falta para el fin del plazo vigente de una solicitud (RF-33, RF-55). */
export interface Plazo {
  readonly nivel: NivelPlazo;
  /** Milisegundos que faltan; 0 si ya venció o no hay plazo. */
  readonly restanteMs: number;
  readonly dias: number;
  readonly horas: number;
  readonly minutos: number;
}

const SIN_PLAZO: Plazo = { nivel: 'sin-plazo', restanteMs: 0, dias: 0, horas: 0, minutos: 0 };

/**
 * El plazo que queda hasta `venceEn`. La API omite `venceEn` en una solicitud cerrada, y eso es "sin plazo". Los minutos
 * se redondean hacia arriba para no decir "0 min" cuando todavía queda tiempo.
 */
export function plazoRestante(venceEn: string | null | undefined, ahora: number): Plazo {
  const fin = venceEn ? Date.parse(venceEn) : NaN;
  if (Number.isNaN(fin)) {
    return SIN_PLAZO;
  }
  const restanteMs = fin - ahora;
  if (restanteMs <= 0) {
    return { ...SIN_PLAZO, nivel: 'vencido' };
  }
  const total = Math.ceil(restanteMs / 60_000);
  return {
    nivel: restanteMs <= UMBRAL_URGENTE_MS ? 'urgente' : 'vigente',
    restanteMs,
    dias: Math.floor(total / 1440),
    horas: Math.floor((total % 1440) / 60),
    minutos: total % 60,
  };
}
