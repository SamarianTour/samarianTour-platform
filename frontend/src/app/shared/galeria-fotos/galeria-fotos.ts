import { DOCUMENT } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { Foto } from '../../core/api/modelos';
import { Icono } from '../icono/icono';
import { indiceCircular } from './navegacion';

/** El mosaico muestra hasta 3 fotos: una grande y dos pequeñas. El resto se ve en el visor. */
const FOTOS_EN_MOSAICO = 3;

/**
 * Galería de fotos de una oferta: un mosaico (1 grande y 2 pequeñas) y un visor a pantalla completa que se abre al
 * pulsar cualquier foto. El visor usa el elemento <dialog>: atrapa el foco, se cierra con Escape y devuelve el foco
 * a la foto que lo abrió. Se navega con los botones o con las flechas del teclado.
 */
@Component({
  selector: 'app-galeria-fotos',
  imports: [Icono],
  templateUrl: './galeria-fotos.html',
  styleUrl: './galeria-fotos.css',
})
export class GaleriaFotos {
  /** Las fotos de la oferta, en orden; la primera es la portada. */
  readonly fotos = input.required<readonly Foto[]>();
  /** Nombre de la oferta, para describir la galería a los lectores de pantalla. */
  readonly nombre = input.required<string>();

  private readonly documento = inject(DOCUMENT);
  private readonly visor = viewChild.required<ElementRef<HTMLDialogElement>>('visor');

  protected readonly indice = signal(0);
  protected readonly total = computed(() => this.fotos().length);
  protected readonly visibles = computed(() => this.fotos().slice(0, FOTOS_EN_MOSAICO));
  protected readonly actual = computed(() => this.fotos()[this.indice()]);
  /** Con una o dos fotos el mosaico se acomoda en una o dos columnas. */
  protected readonly disposicion = computed(() =>
    this.total() === 1 ? 'una' : this.total() === 2 ? 'dos' : 'mosaico',
  );

  constructor() {
    // Si la página cambia con el visor abierto, no se deja la página sin desplazamiento.
    inject(DestroyRef).onDestroy(() => this.soltarPagina());
  }

  protected abrir(indice: number): void {
    this.indice.set(indice);
    this.documento.body.style.overflow = 'hidden';
    this.visor().nativeElement.showModal();
  }

  protected cerrar(): void {
    this.visor().nativeElement.close();
  }

  /** Se dispara al cerrar por cualquier vía: botón, Escape o clic fuera de la foto. */
  protected alCerrar(): void {
    this.soltarPagina();
  }

  /** Un clic sobre el fondo oscuro, y no sobre la foto o los botones, cierra el visor. */
  protected cerrarSiFuera(evento: MouseEvent): void {
    if (evento.target === this.visor().nativeElement) {
      this.cerrar();
    }
  }

  protected ir(desplazamiento: -1 | 1): void {
    this.indice.set(indiceCircular(this.indice(), desplazamiento, this.total()));
  }

  protected teclado(evento: KeyboardEvent): void {
    if (this.total() < 2) {
      return;
    }
    if (evento.key === 'ArrowLeft') {
      this.ir(-1);
    } else if (evento.key === 'ArrowRight') {
      this.ir(1);
    }
  }

  /**
   * [SUPUESTO] La API no guarda una descripción de cada foto (DC-47): el texto alternativo es genérico, con la
   * posición de la foto y el nombre de la oferta.
   */
  protected textoAlternativo(indice: number): string {
    return `Foto ${indice + 1} de ${this.total()} de «${this.nombre()}»`;
  }

  private soltarPagina(): void {
    this.documento.body.style.overflow = '';
  }
}
