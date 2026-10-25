package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.List;
import java.util.Set;

/**
 * Lo que trajo la fuente de alertas en una lectura (DR-16).
 *
 * @param alertas       las de las zonas que se pudieron leer
 * @param zonasFallidas las zonas que no se pudieron leer: conservan sus alertas anteriores
 */
public record ResultadoFuenteAlertas(List<AlertaClimatica> alertas, Set<String> zonasFallidas) {

    public ResultadoFuenteAlertas {
        alertas = List.copyOf(alertas);
        zonasFallidas = Set.copyOf(zonasFallidas);
    }

    /** Todas las zonas se leyeron. */
    public static ResultadoFuenteAlertas completo(List<AlertaClimatica> alertas) {
        return new ResultadoFuenteAlertas(alertas, Set.of());
    }
}
