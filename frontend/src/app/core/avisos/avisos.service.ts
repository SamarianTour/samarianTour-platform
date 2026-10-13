import { Injectable, signal } from '@angular/core';

export interface Aviso {
  readonly id: number;
  readonly texto: string;
}

const DURACION_MS = 6000;

/** Mensajes breves de confirmación, por ejemplo "Enviamos tu oferta a moderación". */
@Injectable({ providedIn: 'root' })
export class AvisosService {
  private readonly lista = signal<readonly Aviso[]>([]);
  private siguienteId = 1;

  readonly avisos = this.lista.asReadonly();

  mostrar(texto: string): void {
    const aviso: Aviso = { id: this.siguienteId++, texto };
    this.lista.update((avisos) => [...avisos, aviso]);
    setTimeout(() => this.cerrar(aviso.id), DURACION_MS);
  }

  cerrar(id: number): void {
    this.lista.update((avisos) => avisos.filter((aviso) => aviso.id !== id));
  }
}
