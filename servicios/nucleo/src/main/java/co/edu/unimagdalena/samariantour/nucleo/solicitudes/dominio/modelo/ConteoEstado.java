package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/** Cuántas solicitudes de un tipo de oferta pasaron alguna vez por un estado, según su historial (RF-87, RF-88). */
public record ConteoEstado(TipoOferta tipo, EstadoSolicitud estado, long solicitudes) {
}
