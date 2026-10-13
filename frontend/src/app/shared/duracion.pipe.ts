import { Pipe, PipeTransform } from '@angular/core';

/** Duración en minutos como texto: 150 → "2 h 30 min". */
@Pipe({ name: 'duracion' })
export class DuracionPipe implements PipeTransform {
  transform(minutos: number | null | undefined): string {
    if (minutos === null || minutos === undefined) {
      return '';
    }
    const horas = Math.floor(minutos / 60);
    const resto = minutos % 60;
    if (horas === 0) {
      return `${resto} min`;
    }
    return resto === 0 ? `${horas} h` : `${horas} h ${resto} min`;
  }
}
