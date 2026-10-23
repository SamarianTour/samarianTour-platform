import { Injectable, computed, signal } from '@angular/core';
import { PRESTADORES_DEMO, PrestadorDemo } from './prestadores-demo';
import { TURISTAS_DEMO, TuristaDemo } from './turistas-demo';

const CLAVE = 'samarian-tour.usuario-demo';

/** El usuario demo elegido: un prestador o un turista, con el rol que el backend exige en cada ruta (DC-13). */
export type UsuarioDemo =
  | { readonly rol: 'PRESTADOR'; readonly prestador: PrestadorDemo }
  | { readonly rol: 'TURISTA'; readonly turista: TuristaDemo };

/**
 * El usuario demo elegido en el selector "Usuario demo" de la cabecera. Se guarda en localStorage para conservarlo al
 * recargar. Sustituye al inicio de sesión mientras no exista el JWT del núcleo.
 */
@Injectable({ providedIn: 'root' })
export class IdentidadService {
  private readonly elegido = signal<UsuarioDemo>(leer());

  readonly usuario = this.elegido.asReadonly();
  /** El prestador elegido, o null si el usuario es un turista. */
  readonly prestador = computed(() => {
    const usuario = this.elegido();
    return usuario.rol === 'PRESTADOR' ? usuario.prestador : null;
  });
  /** El turista elegido, o null si el usuario es un prestador. */
  readonly turista = computed(() => {
    const usuario = this.elegido();
    return usuario.rol === 'TURISTA' ? usuario.turista : null;
  });
  readonly prestadores = PRESTADORES_DEMO;
  readonly turistas = TURISTAS_DEMO;

  elegir(id: string): void {
    const usuario = buscar(id);
    if (!usuario) {
      return;
    }
    this.elegido.set(usuario);
    try {
      localStorage.setItem(CLAVE, id);
    } catch {
      // Sin almacenamiento (modo privado), la elección dura lo que dure la página.
    }
  }
}

function buscar(id: string | null): UsuarioDemo | undefined {
  const prestador = PRESTADORES_DEMO.find((candidato) => candidato.id === id);
  if (prestador) {
    return { rol: 'PRESTADOR', prestador };
  }
  const turista = TURISTAS_DEMO.find((candidato) => candidato.id === id);
  return turista ? { rol: 'TURISTA', turista } : undefined;
}

/** Sin elección guardada, el primer prestador, como antes de que existieran los turistas demo. */
function leer(): UsuarioDemo {
  let guardado: string | null = null;
  try {
    guardado = localStorage.getItem(CLAVE);
  } catch {
    guardado = null;
  }
  return buscar(guardado) ?? { rol: 'PRESTADOR', prestador: PRESTADORES_DEMO[0] };
}
