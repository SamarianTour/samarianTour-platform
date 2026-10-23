package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

/**
 * Los tamaños en que se guarda cada foto (DC-39). El turista descarga el que necesita y no la foto original, que
 * desde un celular puede pesar varios megas: importa en red móvil (QAS-07, RT-03).
 */
public enum TamanoFoto {

    /** Para la galería de la ficha o del perfil. */
    GALERIA(1280),
    /** Para las tarjetas del catálogo y de la portada. */
    MINIATURA(400);

    private final int ladoMaximo;

    TamanoFoto(int ladoMaximo) {
        this.ladoMaximo = ladoMaximo;
    }

    /** El lado más largo de la imagen guardada, en píxeles; una imagen más pequeña no se agranda. */
    public int ladoMaximo() {
        return ladoMaximo;
    }
}
