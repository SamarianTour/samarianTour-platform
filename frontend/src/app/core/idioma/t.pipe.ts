import { Pipe, PipeTransform } from '@angular/core';
import { ClaveTexto } from './textos';
import { Parametros, traducir } from './traducir';

/**
 * Traduce una clave en las plantillas: `{{ 'cuentas.login.titulo' | t }}`, con parámetros
 * `{{ 'comun.pagina' | t: { pagina: 2, total: 5 } }}` y plural `{{ 'comun.minutos' | t: { n: minutos } }}`.
 *
 * Solo acepta claves que existan: una clave mal escrita no compila. Es impuro a propósito: un pipe puro solo se vuelve a
 * evaluar si cambian sus argumentos, y la clave no cambia cuando cambia el idioma. El costo es una búsqueda en un
 * mapa por cada ciclo de detección de cambios.
 */
@Pipe({ name: 't', pure: false })
export class TPipe implements PipeTransform {
  transform(clave: ClaveTexto, parametros?: Parametros): string {
    return traducir(clave, parametros);
  }
}
