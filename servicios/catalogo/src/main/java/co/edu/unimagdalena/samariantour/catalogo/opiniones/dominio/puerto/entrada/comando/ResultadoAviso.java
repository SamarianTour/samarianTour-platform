package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando;

/**
 * Qué pasó con un aviso del núcleo; los mismos tres resultados que los avisos de ofertas (DC-04). En los tres casos
 * el aviso queda registrado y el núcleo no debe reintentarlo.
 */
public enum ResultadoAviso {
    /** El aviso cambió las opiniones o las habilitaciones. */
    APLICADO,
    /** El aviso ya no aplica: la opinión no existe o ya se moderó, o no había nada que habilitar ni borrar. */
    IGNORADO,
    /** El aviso ya se había procesado antes. */
    DUPLICADO
}
