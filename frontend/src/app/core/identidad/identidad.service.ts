import { Injectable, signal } from '@angular/core';
import { PRESTADORES_DEMO, PrestadorDemo } from './prestadores-demo';

const CLAVE = 'samarian-tour.prestador-demo';

/**
 * El prestador demo elegido en el panel del prestador. Se guarda en localStorage para conservarlo al recargar.
 * Sustituye al inicio de sesión mientras no exista el JWT del núcleo.
 */
@Injectable({ providedIn: 'root' })
export class IdentidadService {
  private readonly elegido = signal<PrestadorDemo>(leer());

  readonly prestador = this.elegido.asReadonly();
  readonly prestadores = PRESTADORES_DEMO;

  elegir(id: string): void {
    const prestador = PRESTADORES_DEMO.find((candidato) => candidato.id === id);
    if (!prestador) {
      return;
    }
    this.elegido.set(prestador);
    try {
      localStorage.setItem(CLAVE, prestador.id);
    } catch {
      // Sin almacenamiento (modo privado), la elección dura lo que dure la página.
    }
  }
}

function leer(): PrestadorDemo {
  let guardado: string | null = null;
  try {
    guardado = localStorage.getItem(CLAVE);
  } catch {
    guardado = null;
  }
  return PRESTADORES_DEMO.find((prestador) => prestador.id === guardado) ?? PRESTADORES_DEMO[0];
}
