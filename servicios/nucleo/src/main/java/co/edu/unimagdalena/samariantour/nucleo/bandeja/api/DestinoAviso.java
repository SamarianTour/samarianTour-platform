package co.edu.unimagdalena.samariantour.nucleo.bandeja.api;

/** A cuál de las dos entradas de avisos del catálogo va un aviso (contrato del catálogo). */
public enum DestinoAviso {
    /** {@code POST /internal/avisos}: HABILITACION y MODERACION de ofertas. */
    OFERTAS,
    /**
     * {@code POST /internal/opiniones/avisos}: HABILITACION_OPINION, MODERACION_OPINION y SUPRESION_TURISTA.
     */
    OPINIONES
}
