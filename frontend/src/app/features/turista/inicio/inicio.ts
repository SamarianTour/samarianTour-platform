import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { CatalogoService } from '../../../core/api/catalogo.service';
import { TipoOferta, Zona } from '../../../core/api/modelos';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { Icono, ICONO_DE_TIPO } from '../../../shared/icono/icono';
import { TEXTO_MAXIMO, normalizarTexto, textoInvalido } from '../catalogo/filtros';

/** Una de las cuatro tarjetas de tipo. El paquete tiene su propia pantalla (`/paquetes`); los demás filtran el catálogo. */
interface TarjetaDeTipo {
  readonly tipo: TipoOferta;
  readonly nombre: ClaveTexto;
  readonly descripcion: ClaveTexto;
  readonly ruta: string;
  readonly consulta?: Readonly<Record<string, string>>;
}

const TARJETAS: readonly TarjetaDeTipo[] = [
  {
    tipo: 'ACTIVIDAD',
    nombre: 'turista.inicio.tipo.actividad',
    descripcion: 'turista.inicio.tipo.actividad.desc',
    ruta: '/catalogo',
    consulta: { tipo: 'ACTIVIDAD' },
  },
  {
    tipo: 'PAQUETE',
    nombre: 'turista.inicio.tipo.paquete',
    descripcion: 'turista.inicio.tipo.paquete.desc',
    ruta: '/paquetes',
  },
  {
    tipo: 'ALOJAMIENTO',
    nombre: 'turista.inicio.tipo.alojamiento',
    descripcion: 'turista.inicio.tipo.alojamiento.desc',
    ruta: '/catalogo',
    consulta: { tipo: 'ALOJAMIENTO' },
  },
  {
    tipo: 'RESTAURANTE',
    nombre: 'turista.inicio.tipo.restaurante',
    descripcion: 'turista.inicio.tipo.restaurante.desc',
    ruta: '/catalogo',
    consulta: { tipo: 'RESTAURANTE' },
  },
];

/** Cuántas zonas se muestran con un mismo fondo antes de repetir. El contrato no trae imagen de la zona. */
const FONDOS_DE_ZONA = 6;

/**
 * P01.0 Inicio, público. Reúne el buscador (lleva al catálogo con el texto), las cuatro tarjetas de tipo, las zonas reales
 * (`GET /zonas`) y la invitación a pedir recomendaciones. No hay "destacados" ni patrocinados, y tampoco consulta el servicio
 * de recomendación: cada visita gastaría cupo de la IA y, sin la consulta del turista, la respuesta sería genérica.
 *
 * Las zonas van en una fila con desplazamiento horizontal, que se desliza en el celular y se mueve con los botones anterior y
 * siguiente. No avanza sola (WCAG 2.2.2).
 */
@Component({
  selector: 'app-inicio',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css',
})
export class Inicio {
  private readonly router = inject(Router);
  private readonly catalogo = inject(CatalogoService);

  private readonly pista = viewChild<ElementRef<HTMLElement>>('pista');

  protected readonly tarjetas = TARJETAS;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;
  protected readonly textoMaximo = TEXTO_MAXIMO;

  protected readonly texto = new FormControl('', { nonNullable: true });
  /** Se avisa al enviar, no mientras se escribe: la persona aún puede estar a medias. */
  protected readonly textoCorto = signal(false);

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

  protected buscar(evento: Event): void {
    evento.preventDefault();
    const escrito = this.texto.value;
    if (textoInvalido(escrito)) {
      this.textoCorto.set(true);
      return;
    }
    this.textoCorto.set(false);
    const texto = normalizarTexto(escrito);
    void this.router.navigate(['/catalogo'], { queryParams: texto ? { texto } : {} });
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
