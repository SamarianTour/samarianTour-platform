package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

/**
 * Para qué se subió una foto (DC-47). Las fotos de un destino no se usan en el otro.
 */
public enum DestinoFoto {

    /** Una foto del perfil público. Solo la usa el perfil, así que se borra en cuanto el prestador la quita. */
    PERFIL,

    /**
     * Una foto para las ofertas del prestador; puede ir en varias. Como otra oferta puede estar usándola, solo la
     * borra la limpieza, cuando pasó el plazo sin uso.
     */
    OFERTAS
}
