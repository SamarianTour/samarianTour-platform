package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/** Cuántas solicitudes se enviaron a una zona y un tipo de oferta (RF-85). */
public record ConteoDemanda(String zonaId, TipoOferta tipo, long solicitudes) {
}
