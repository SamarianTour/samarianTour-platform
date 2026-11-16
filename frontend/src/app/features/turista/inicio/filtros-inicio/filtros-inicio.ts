import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Params, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { CatalogoService } from '../../../../core/api/catalogo.service';
import { TipoOferta, Zona } from '../../../../core/api/modelos';
import { TPipe } from '../../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../../core/idioma/textos';
import { PrecioPipe } from '../../../../shared/precio.pipe';
import { Icono } from '../../../../shared/icono/icono';

/** Los rangos de precio del selector. `min` y `max` son los valores que entiende el catálogo (en pesos). */
export type ClaveRango = 'cualquiera' | 'hasta' | 'entre' | 'mas';

export const RANGOS_DE_PRECIO: Readonly<Record<ClaveRango, { readonly min: number | null; readonly max: number | null }>> = {
  cualquiera: { min: null, max: null },
  hasta: { min: null, max: 100_000 },
  entre: { min: 100_000, max: 300_000 },
  mas: { min: 300_000, max: null },
};

/** Lo que la persona eligió en los filtros rápidos. */
export interface EleccionInicio {
  readonly tipo: TipoOferta | null;
  readonly zonaId: string;
  readonly rango: ClaveRango;
  readonly sillaRuedas: boolean;
  readonly bajoEsfuerzo: boolean;
}

/** Los query params de `/catalogo` para esta elección: solo viajan los que la persona eligió. */
export function consultaDeEleccion(eleccion: EleccionInicio): Params {
  const params: Params = {};
  if (eleccion.tipo) params['tipo'] = eleccion.tipo;
  if (eleccion.zonaId) params['zonaId'] = eleccion.zonaId;
  const { min, max } = RANGOS_DE_PRECIO[eleccion.rango];
  if (min !== null) params['precioMin'] = min;
  if (max !== null) params['precioMax'] = max;
  if (eleccion.sillaRuedas) params['accesibleSillaRuedas'] = true;
  if (eleccion.bajoEsfuerzo) params['nivelEsfuerzoMaximo'] = 'BAJO';
  return params;
}

interface OpcionDeTipo {
  readonly tipo: TipoOferta | null;
  readonly nombre: ClaveTexto;
}

const TIPOS: readonly OpcionDeTipo[] = [
  { tipo: null, nombre: 'turista.inicio.filtros.todos' },
  { tipo: 'ACTIVIDAD', nombre: 'turista.inicio.tipo.actividad' },
  { tipo: 'ALOJAMIENTO', nombre: 'turista.inicio.filtros.alojamientos' },
  { tipo: 'RESTAURANTE', nombre: 'turista.inicio.tipo.restaurante' },
  { tipo: 'PAQUETE', nombre: 'turista.inicio.tipo.paquete' },
];

/**
 * Filtros rápidos del Inicio: tipo, zona, rango de precio y accesibilidad. "Ver ofertas" lleva al catálogo con los
 * parámetros que ya entiende (`filtros.ts`). Si `GET /zonas` falla, el selector ofrece solo "Todas las zonas".
 */
@Component({
  selector: 'app-filtros-inicio',
  imports: [RouterLink, Icono, TPipe, PrecioPipe],
  templateUrl: './filtros-inicio.html',
  styleUrl: './filtros-inicio.css',
})
export class FiltrosInicio {
  private readonly catalogo = inject(CatalogoService);

  protected readonly tipos = TIPOS;
  protected readonly zonas = signal<readonly Zona[]>([]);

  protected readonly tipo = signal<TipoOferta | null>(null);
  protected readonly zonaId = signal('');
  protected readonly rango = signal<ClaveRango>('cualquiera');
  protected readonly sillaRuedas = signal(false);
  protected readonly bajoEsfuerzo = signal(false);

  protected readonly consulta = computed(() =>
    consultaDeEleccion({
      tipo: this.tipo(),
      zonaId: this.zonaId(),
      rango: this.rango(),
      sillaRuedas: this.sillaRuedas(),
      bajoEsfuerzo: this.bajoEsfuerzo(),
    }),
  );

  constructor() {
    this.catalogo
      .zonas()
      .pipe(
        catchError(() => of<Zona[]>([])),
        takeUntilDestroyed(),
      )
      .subscribe((zonas) => this.zonas.set(zonas));
  }

  protected elegirZona(evento: Event): void {
    this.zonaId.set((evento.target as HTMLSelectElement).value);
  }

  protected elegirRango(evento: Event): void {
    this.rango.set((evento.target as HTMLSelectElement).value as ClaveRango);
  }
}
