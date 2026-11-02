package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

/** Cuántas solicitudes de un tipo de oferta pasaron alguna vez por un estado. */
public record ConteoEstado(String tipoOferta, String estado, long solicitudes) {
}
