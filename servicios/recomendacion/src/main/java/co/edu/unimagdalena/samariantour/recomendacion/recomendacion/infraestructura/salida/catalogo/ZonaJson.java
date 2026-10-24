package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Del esquema {@code Zona} del contrato del catálogo solo se usa el nombre.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ZonaJson(String id, String nombre) {
}
