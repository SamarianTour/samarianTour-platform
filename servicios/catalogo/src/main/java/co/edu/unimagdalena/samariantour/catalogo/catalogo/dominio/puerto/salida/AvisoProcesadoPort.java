package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

/**
 * Registro de los avisos del núcleo ya aplicados, para descartar los repetidos (idempotencia).
 */
public interface AvisoProcesadoPort {

    boolean fueProcesado(String idAviso);

    /** Debe tolerar que otra réplica ya lo haya marcado: en ese caso no hace nada. */
    void marcarProcesado(String idAviso);
}
