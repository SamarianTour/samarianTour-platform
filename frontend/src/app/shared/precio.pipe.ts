import { formatCurrency } from '@angular/common';
import { LOCALE_ID, Pipe, PipeTransform, inject } from '@angular/core';

/**
 * Precio de referencia en pesos colombianos, sin decimales y seguido de su unidad (DC-22):
 * "$ 150.000 por persona".
 */
@Pipe({ name: 'precio' })
export class PrecioPipe implements PipeTransform {
  private readonly locale = inject(LOCALE_ID);

  transform(valor: number | null | undefined, unidad?: string | null): string {
    if (valor === null || valor === undefined) {
      return '';
    }
    const precio = formatCurrency(valor, this.locale, '$', 'COP', '1.0-0');
    return unidad ? `${precio} ${unidad}` : precio;
  }
}
