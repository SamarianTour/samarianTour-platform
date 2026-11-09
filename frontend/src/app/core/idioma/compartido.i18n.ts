import { diccionario } from './diccionario';

/** Textos de los componentes compartidos (`shared/`): paginación, diálogo, avisos, estrellas, mapa, galería y "próximamente". */
export const COMPARTIDO = diccionario(
  {
    'compartido.paginacion.etiqueta': 'Paginación',
    'compartido.paginacion.anterior': 'Anterior',
    'compartido.paginacion.siguiente': 'Siguiente',

    'compartido.dialogo.confirmar': 'Confirmar',

    'compartido.avisos.cerrar': 'Cerrar mensaje',

    'compartido.estrellas.descripcion': '{valor} de 5 estrellas',

    'compartido.mapa.descripcion': 'Mapa de la ubicación',

    'compartido.galeria.verFoto': 'Ver foto {indice} de {total}',
    'compartido.galeria.verTodas': 'Ver las {total} fotos',
    'compartido.galeria.etiqueta': 'Fotos de {nombre}',
    'compartido.galeria.laGaleria': 'la galería',
    'compartido.galeria.anterior': 'Foto anterior',
    'compartido.galeria.siguiente': 'Foto siguiente',
    'compartido.galeria.posicion': '{indice} de {total}',
    'compartido.galeria.alt': 'Foto {indice} de {total} de «{nombre}»',

    'compartido.proximamente.pantalla': 'Esta pantalla',
    'compartido.proximamente.descripcion': 'La construimos en una de las próximas fases.',
    'compartido.proximamente.estado':
      'Esta pantalla está en construcción y llegará en una de las próximas fases.',
    'compartido.proximamente.volver': 'Volver al catálogo',
  },
  {
    'compartido.paginacion.etiqueta': 'Pagination',
    'compartido.paginacion.anterior': 'Previous',
    'compartido.paginacion.siguiente': 'Next',

    'compartido.dialogo.confirmar': 'Confirm',

    'compartido.avisos.cerrar': 'Dismiss message',

    'compartido.estrellas.descripcion': '{valor} out of 5 stars',

    'compartido.mapa.descripcion': 'Location map',

    'compartido.galeria.verFoto': 'View photo {indice} of {total}',
    'compartido.galeria.verTodas': 'View all {total} photos',
    'compartido.galeria.etiqueta': 'Photos of {nombre}',
    'compartido.galeria.laGaleria': 'the gallery',
    'compartido.galeria.anterior': 'Previous photo',
    'compartido.galeria.siguiente': 'Next photo',
    'compartido.galeria.posicion': '{indice} of {total}',
    'compartido.galeria.alt': 'Photo {indice} of {total} of “{nombre}”',

    'compartido.proximamente.pantalla': 'This screen',
    'compartido.proximamente.descripcion': "We're building it in one of the upcoming phases.",
    'compartido.proximamente.estado':
      'This screen is under construction and will arrive in one of the upcoming phases.',
    'compartido.proximamente.volver': 'Back to the catalog',
  },
);
