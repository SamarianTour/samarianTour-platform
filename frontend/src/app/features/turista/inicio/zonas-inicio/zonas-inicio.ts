import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { CatalogoService } from '../../../../core/api/catalogo.service';
import { Zona } from '../../../../core/api/modelos';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { Icono } from '../../../../shared/icono/icono';

/** Cuántas zonas se muestran con un mismo fondo antes de repetir. El contrato no trae imagen de la zona. */
const FONDOS_DE_ZONA = 6;

/**
 * Las zonas reales del Inicio (`GET /zonas`), en una fila con desplazamiento horizontal con botones anterior y siguiente, que
 * también se desliza en el celular. No avanza sola (WCAG 2.2.2). Si la petición falla o no hay zonas, la sección no aparece.
 */
@Component({
  selector: 'app-zonas-inicio',
  imports: [RouterLink, Icono, TPipe],
  templateUrl: './zonas-inicio.html',
  styleUrl: './zonas-inicio.css',
})
export class ZonasInicio {
  private readonly catalogo = inject(CatalogoService);

  private readonly pista = viewChild<ElementRef<HTMLElement>>('pista');

  /** `null` mientras carga; la lista vacía si falla o no hay zonas, y entonces la sección no aparece. */
  protected readonly zonas = signal<readonly Zona[] | null>(null);
  protected readonly hayZonas = computed(() => (this.zonas()?.length ?? 0) > 0);
  protected readonly hayAnterior = signal(false);
  protected readonly haySiguiente = signal(false);

  constructor() {
    this.catalogo
      .zonas()
      .pipe(
        catchError(() => of<Zona[]>([])),
        takeUntilDestroyed(),
      )
      .subscribe((zonas) => {
        this.zonas.set(zonas);
        // Las tarjetas se pintan en el siguiente ciclo: ahí se sabe si la fila se desborda.
        queueMicrotask(() => this.actualizarBotones());
      });
  }

  protected fondoDeZona(posicion: number): string {
    return `zona--fondo-${posicion % FONDOS_DE_ZONA}`;
  }

  /** Mueve la fila casi un ancho de pantalla; sin animación si la persona pidió menos movimiento. */
  protected mover(sentido: -1 | 1): void {
    const pista = this.pista()?.nativeElement;
    // Los botones usan aria-disabled y no disabled: así conservan el foco del teclado al llegar al extremo.
    if (!pista || !(sentido === -1 ? this.hayAnterior() : this.haySiguiente())) {
      return;
    }
    const reducido = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    pista.scrollBy?.({ left: sentido * pista.clientWidth * 0.85, behavior: reducido ? 'auto' : 'smooth' });
  }

  protected actualizarBotones(): void {
    const pista = this.pista()?.nativeElement;
    if (!pista) {
      return;
    }
    this.hayAnterior.set(pista.scrollLeft > 4);
    this.haySiguiente.set(pista.scrollLeft + pista.clientWidth < pista.scrollWidth - 4);
  }
}
