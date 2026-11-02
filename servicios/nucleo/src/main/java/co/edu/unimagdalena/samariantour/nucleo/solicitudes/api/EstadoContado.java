package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

/** @param estado uno de los nueve estados de la solicitud, como PENDIENTE o PAGADA */
public record EstadoContado(String tipoOferta, String estado, long solicitudes) {
}
