package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que busca el turista (esquema {@code ConsultaRecomendacion}). Todo es opcional, pero hace falta el texto o al
 * menos un filtro.
 *
 * @param idioma es | en; por defecto es
 */
public record RecomendacionRequest(String texto, String idioma, List<TipoOferta> tiposOferta,
                                   List<TipoActividad> tiposActividad, String zonaId, Boolean accesibleSillaRuedas,
                                   NivelEsfuerzo nivelEsfuerzoMaximo, BigDecimal precioMaximo) {
}
