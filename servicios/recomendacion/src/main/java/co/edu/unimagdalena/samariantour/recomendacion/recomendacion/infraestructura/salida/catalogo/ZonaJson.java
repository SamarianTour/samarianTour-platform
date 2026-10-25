package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Del esquema {@code Zona} del contrato del catálogo se usan el nombre y la ubicación, con la que se consultan sus
 * alertas por punto (DR-15).
 *
 * @param ubicacion null si la zona no tiene
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ZonaJson(String id, String nombre, Coordenada ubicacion) {

    /** Esquema {@code Coordenada} del contrato del catálogo. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Coordenada(Double latitud, Double longitud) {
    }
}
