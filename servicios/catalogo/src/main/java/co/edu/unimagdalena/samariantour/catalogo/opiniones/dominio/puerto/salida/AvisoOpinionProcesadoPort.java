package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida;

/**
 * Registro de los avisos de opiniones ya aplicados, para descartar los repetidos (DC-04). Es propio del módulo: no
 * comparte el registro de los avisos de ofertas (DC-46).
 */
public interface AvisoOpinionProcesadoPort {

    boolean fueProcesado(String idAviso);

    /** Debe tolerar que otra réplica ya lo haya marcado: en ese caso no hace nada. */
    void marcarProcesado(String idAviso);
}
