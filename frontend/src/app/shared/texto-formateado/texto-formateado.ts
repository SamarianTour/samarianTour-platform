import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { idiomaActual } from '../../core/idioma/idioma';
import { type ClaveTexto, TEXTOS } from '../../core/idioma/textos';
import { type Bloque, parsearMarkdown, textoPlano } from './markdown-basico';

/** Minúsculas y sin tildes, para comparar títulos. */
function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim();
}

/**
 * Pinta un texto en el Markdown básico de la política de datos (ver `markdown-basico.ts`) sin librerías y sin `innerHTML`:
 * los bloques son datos y la plantilla los dibuja con `@switch` e interpolación, así que un `<script>` se ve como texto.
 *
 * Los títulos bajan un nivel (`#` → h2, `##` → h3) porque la pantalla que lo usa ya tiene su h1. Con `omitirTitulo`, si el
 * primer bloque es un título que repite el de la pantalla (en cualquiera de los dos idiomas), no se muestra.
 */
@Component({
  selector: 'app-texto-formateado',
  imports: [NgTemplateOutlet],
  templateUrl: './texto-formateado.html',
  styleUrl: './texto-formateado.css',
})
export class TextoFormateado {
  readonly texto = input.required<string>();
  /** La clave del título de la pantalla: si el texto abre con ese mismo título, se omite. */
  readonly omitirTitulo = input<ClaveTexto | null>(null);

  protected readonly bloques = computed<readonly Bloque[]>(() => {
    // Se lee el idioma para recalcular al cambiarlo: el título repetido puede estar en el idioma que no se ve.
    idiomaActual();
    const bloques = parsearMarkdown(this.texto());
    const clave = this.omitirTitulo();
    const primero = bloques[0];
    if (clave && primero?.tipo === 'titulo') {
      const titulo = normalizar(textoPlano(primero.segmentos));
      const repetido = (['es', 'en'] as const).some((idioma) => {
        const propio = TEXTOS[idioma][clave];
        return typeof propio === 'string' && titulo.startsWith(normalizar(propio));
      });
      if (repetido) {
        return bloques.slice(1);
      }
    }
    return bloques;
  });
}
