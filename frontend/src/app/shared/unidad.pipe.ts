import { Pipe, PipeTransform } from '@angular/core';
import { unidadDePrecio } from './etiquetas';

/** La unidad del precio de una oferta ("por persona" / "per person") en el idioma vigente. */
@Pipe({ name: 'unidad', pure: false })
export class UnidadPipe implements PipeTransform {
  transform(unidad: string | null | undefined): string {
    return unidadDePrecio(unidad);
  }
}
