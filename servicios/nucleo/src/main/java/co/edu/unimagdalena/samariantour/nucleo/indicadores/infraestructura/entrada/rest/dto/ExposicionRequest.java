package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Lo que la recomendación mostró en una consulta (RET-02).
 *
 * @param origen RECOMENDACION o ALTERNATIVAS
 * @param motor  REGLAS o IA
 */
public record ExposicionRequest(Instant fecha, String origen, String motor, List<OfertaExpuestaRequest> ofertas) {
}
