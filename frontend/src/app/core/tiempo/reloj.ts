import { DestroyRef, Signal, inject, signal } from '@angular/core';

/** Cada cuánto se actualiza el reloj de las pantallas con plazos: los plazos se cuentan en minutos. */
export const INTERVALO_DEL_RELOJ_MS = 30_000;

/**
 * El momento actual (milisegundos), que se renueva solo mientras la pantalla está abierta. Una pantalla que muestra "te quedan
 * 45 min" lo lee en un `computed` y el texto se actualiza sin recargar. Se llama dentro de un contexto de inyección: el
 * temporizador se detiene cuando la pantalla se destruye.
 */
export function ahoraConReloj(intervaloMs = INTERVALO_DEL_RELOJ_MS): Signal<number> {
  const ahora = signal(Date.now());
  const temporizador = setInterval(() => ahora.set(Date.now()), intervaloMs);
  inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
  return ahora.asReadonly();
}
