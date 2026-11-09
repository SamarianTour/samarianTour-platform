import { formatCurrency } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';
import { LOCALE, idiomaActual } from '../core/idioma/idioma';
import '../core/idioma/locales';

/**
 * Precio de referencia en pesos colombianos, sin decimales y seguido de su unidad (DC-22): "$ 150.000 por persona" en
 * español y "COP 150,000 per person" en inglés. Es impuro a propósito: se vuelve a calcular al cambiar de idioma.
 */
@Pipe({ name: 'precio', pure: false })
export class PrecioPipe implements PipeTransform {
  transform(valor: number | null | undefined, unidad?: string | null): string {
    if (valor === null || valor === undefined) {
      return '';
    }
    const idioma = idiomaActual();
    // En inglés el símbolo es la sigla COP (un "$" se confundiría con dólares) y lleva un espacio fijo antes del número.
    const simbolo = idioma === 'es' ? '$' : 'COP ';
    const precio = formatCurrency(valor, LOCALE[idioma], simbolo, 'COP', '1.0-0');
    return unidad ? `${precio} ${unidad}` : precio;
  }
}
