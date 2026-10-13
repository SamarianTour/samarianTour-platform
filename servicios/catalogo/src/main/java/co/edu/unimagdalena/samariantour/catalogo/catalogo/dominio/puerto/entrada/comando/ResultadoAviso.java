package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

/**
 * Qué pasó con un aviso del núcleo. En los tres casos el aviso queda registrado y el núcleo no debe reintentarlo.
 */
public enum ResultadoAviso {
    /** El aviso cambió el estado del catálogo. */
    APLICADO,
    /** El aviso llegó tarde o ya no aplica (versión vieja, versión reemplazada, oferta inexistente o retirada). */
    IGNORADO,
    /** El aviso ya se había procesado antes. */
    DUPLICADO
}
