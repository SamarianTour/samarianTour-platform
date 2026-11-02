package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

/** @param tipoOferta ACTIVIDAD, PAQUETE, ALOJAMIENTO o RESTAURANTE */
public record DemandaContada(String zonaId, String tipoOferta, long solicitudes) {
}
