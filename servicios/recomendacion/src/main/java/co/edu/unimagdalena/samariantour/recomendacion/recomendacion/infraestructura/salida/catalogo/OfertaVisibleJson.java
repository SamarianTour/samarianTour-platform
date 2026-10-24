package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Esquema {@code OfertaVisible} del contrato del catálogo (DC-36). Los valores de los enum llegan como texto: uno que
 * este servicio no conozca no debe tumbar la lectura de todo el catálogo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OfertaVisibleJson(String id, String tipo, String nombre, String descripcion, UUID prestadorId, String zonaId,
                         BigDecimal precioReferencia, String unidadPrecio, List<String> tiposActividad,
                         String nivelEsfuerzo, boolean accesibleSillaRuedas) {
}
