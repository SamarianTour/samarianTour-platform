import { formatDate } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';
import { Idioma, LOCALE, idiomaActual } from '../core/idioma/idioma';
import '../core/idioma/locales';

/** Los tres tamaños de fecha de la interfaz. Cada idioma los escribe a su manera. */
export type FormatoFecha = 'corta' | 'larga' | 'conHora';

const FORMATOS: Readonly<Record<FormatoFecha, Readonly<Record<Idioma, string>>>> = {
  /** 5 nov 2026 / Nov 5, 2026 */
  corta: { es: 'd MMM y', en: 'MMM d, y' },
  /** 5 de noviembre de 2026 / November 5, 2026 */
  larga: { es: "d 'de' MMMM 'de' y", en: 'MMMM d, y' },
  /** 5 nov 2026, 3:30 p. m. / Nov 5, 2026, 3:30 PM */
  conHora: { es: 'd MMM y, h:mm a', en: 'MMM d, y, h:mm a' },
};

/**
 * Una fecha en el formato del idioma vigente. Reemplaza al pipe `date` de Angular, que usa un formato fijo (`LOCALE_ID`
 * no puede cambiar mientras la aplicación corre). Es impuro a propósito: se vuelve a calcular al cambiar de idioma.
 */
@Pipe({ name: 'fecha', pure: false })
export class FechaPipe implements PipeTransform {
  transform(
    valor: string | number | Date | null | undefined,
    formato: FormatoFecha = 'corta',
  ): string {
    if (valor === null || valor === undefined || valor === '') {
      return '';
    }
    const idioma = idiomaActual();
    return formatDate(valor, FORMATOS[formato][idioma], LOCALE[idioma]);
  }
}
