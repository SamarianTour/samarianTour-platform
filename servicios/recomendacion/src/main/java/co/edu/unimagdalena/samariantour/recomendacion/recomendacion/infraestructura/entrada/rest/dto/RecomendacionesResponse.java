package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AvisoRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Esquema {@code Recomendaciones}: el motor que respondió, por transparencia, y las recomendaciones.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecomendacionesResponse(MotorRecomendacion motor, AvisoRecomendacion aviso,
                                      List<RecomendacionResponse> recomendaciones) {
}
