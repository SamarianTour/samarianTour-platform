package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando;

/**
 * Aviso que el núcleo envía al módulo de opiniones por la bandeja de salida. Cada aviso trae un identificador único
 * para descartar los repetidos (DC-04).
 */
public sealed interface AvisoOpinionCommand
        permits AvisoHabilitacionOpinion, AvisoModeracionOpinion, AvisoSupresionTurista {

    String idAviso();
}
