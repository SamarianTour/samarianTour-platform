package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Qué ofertas mostró la recomendación en una consulta (RET-02, DN-42), como lo envía su {@code ExposicionPort}. No
 * lleva el texto ni ningún dato del turista.
 *
 * @param id    el de la consulta: agrupa sus ofertas
 * @param motor REGLAS o IA
 */
public record RegistroExposicion(UUID id, Instant fecha, OrigenExposicion origen, String motor,
                                 List<OfertaExpuesta> ofertas) {

    /** La recomendación muestra de 3 a 5; las alternativas, hasta 3 (RF-26, RF-29). Diez da margen. */
    public static final int MAXIMO_OFERTAS = 10;

    public RegistroExposicion {
        if (fecha == null) {
            throw new DatoInvalidoException("fecha", "es obligatoria");
        }
        if (origen == null) {
            throw new DatoInvalidoException("origen", "debe ser RECOMENDACION o ALTERNATIVAS");
        }
        if (!"REGLAS".equals(motor) && !"IA".equals(motor)) {
            throw new DatoInvalidoException("motor", "debe ser REGLAS o IA");
        }
        if (ofertas == null || ofertas.isEmpty() || ofertas.size() > MAXIMO_OFERTAS) {
            throw new DatoInvalidoException("ofertas", "debe tener de 1 a %d".formatted(MAXIMO_OFERTAS));
        }
        ofertas = List.copyOf(ofertas);
    }
}
