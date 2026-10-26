package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoOferta;

/** La oferta como se copió al crear la solicitud (DC-35): no cambia si el prestador la edita. */
public record OfertaSolicitadaResponse(String id, TipoOferta tipo, String nombre, String zonaId) {
}
