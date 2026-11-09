import { Pipe, PipeTransform } from '@angular/core';
import { LOCALE, idiomaActual } from '../core/idioma/idioma';

/** Un número con el separador decimal del idioma vigente: 4,7 en español y 4.7 en inglés. */
@Pipe({ name: 'numero', pure: false })
export class NumeroPipe implements PipeTransform {
  transform(valor: number | null | undefined, decimales = 0): string {
    if (valor === null || valor === undefined) {
      return '';
    }
    return valor.toLocaleString(LOCALE[idiomaActual()], {
      minimumFractionDigits: decimales,
      maximumFractionDigits: decimales,
    });
  }
}
