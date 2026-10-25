package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoAlerta;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Esquema {@code Alerta}: una condición activa del destino, con su fuente (RF-28). El título y la descripción van en el
 * idioma pedido; la fuente, tal cual (DR-21).
 *
 * @param descripcion  se omite si la fuente no la da
 * @param vigenteHasta se omite si la alerta no tiene fin anunciado
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertaResponse(String id, TipoAlerta tipo, NivelAlerta nivel, String titulo, String descripcion,
                             String fuente, Instant emitidaEn, Instant vigenteHasta) {
}
