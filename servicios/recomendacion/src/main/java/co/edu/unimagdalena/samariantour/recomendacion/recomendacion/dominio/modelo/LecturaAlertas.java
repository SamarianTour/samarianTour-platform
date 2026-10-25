package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una lectura de la fuente de alertas con su hora (DR-16). La caché guarda la última buena.
 *
 * @param alertas            todas las que trajo la fuente, activas o no: la vigencia se decide al consultar
 * @param actualizadoEn      cuándo se hizo la lectura
 * @param actualizadoPorZona la hora de cada zona que no se pudo leer esta vez y conserva sus alertas anteriores; las
 *                           demás zonas tienen {@code actualizadoEn}
 */
public record LecturaAlertas(List<AlertaClimatica> alertas, Instant actualizadoEn,
                             Map<String, Instant> actualizadoPorZona) {

    public LecturaAlertas {
        alertas = List.copyOf(alertas);
        Objects.requireNonNull(actualizadoEn, "actualizadoEn");
        actualizadoPorZona = Map.copyOf(actualizadoPorZona);
    }

    /** Una lectura en la que todas las zonas se leyeron a la vez. */
    public LecturaAlertas(List<AlertaClimatica> alertas, Instant actualizadoEn) {
        this(alertas, actualizadoEn, Map.of());
    }

    /** La hora de los datos de esa zona. */
    public Instant actualizadoEn(String zonaId) {
        return actualizadoPorZona.getOrDefault(zonaId, actualizadoEn);
    }
}
