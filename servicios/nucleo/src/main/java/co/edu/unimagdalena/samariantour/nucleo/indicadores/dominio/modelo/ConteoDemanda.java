package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

/** Cuántas solicitudes se enviaron a una zona y un tipo de oferta, como las cuentan las solicitudes. */
public record ConteoDemanda(String zonaId, String tipoOferta, long solicitudes) {
}
