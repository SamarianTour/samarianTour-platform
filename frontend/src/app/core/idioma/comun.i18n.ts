import { diccionario } from './diccionario';

/** Textos que usa toda la aplicación. Los términos del inglés siguen el glosario (`GLOSARIO.md`). */
export const COMUN = diccionario(
  {
    'comun.descripcion':
      'Catálogo de turismo sostenible en Santa Marta: actividades, paquetes, alojamientos y restaurantes.',
    'comun.idioma.etiqueta': 'Idioma',
    'comun.cargando': 'Cargando…',
    'comun.cerrar': 'Cerrar',
    'comun.cancelar': 'Cancelar',
    'comun.reintentar': 'Intentar de nuevo',
    'comun.pagina': 'Página {pagina} de {total}',
    'comun.minutos': { uno: '{n} minuto', otros: '{n} minutos' },
  },
  {
    'comun.descripcion':
      'Sustainable tourism catalog for Santa Marta: activities, packages, accommodation and restaurants.',
    'comun.idioma.etiqueta': 'Language',
    'comun.cargando': 'Loading…',
    'comun.cerrar': 'Close',
    'comun.cancelar': 'Cancel',
    'comun.reintentar': 'Try again',
    'comun.pagina': 'Page {pagina} of {total}',
    'comun.minutos': { uno: '{n} minute', otros: '{n} minutes' },
  },
);
