package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

/** @param solicitudes vacío si es 1 o 2 (RET-03) */
public record DemandaZona(String zonaId, String tipoOferta, Long solicitudes) {
}
