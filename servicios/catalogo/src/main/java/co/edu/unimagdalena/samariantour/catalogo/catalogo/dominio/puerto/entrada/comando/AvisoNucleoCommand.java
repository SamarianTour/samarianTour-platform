package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

/**
 * Aviso que el núcleo envía al catálogo por la bandeja de salida. Cada aviso trae un identificador único
 * para descartar los repetidos.
 */
public sealed interface AvisoNucleoCommand permits AvisoModeracion, AvisoHabilitacion {

    String idAviso();
}
