package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.LecturaAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.AlertaCachePort;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Optional;

/**
 * La última lectura buena de alertas, en memoria con Caffeine (DR-16). No vence: si la fuente deja de responder, la
 * lectura anterior sigue sirviendo, con su hora, hasta que llegue una nueva. Cada réplica tiene la suya.
 */
public class AlertaCacheCaffeineAdapter implements AlertaCachePort {

    private static final String CLAVE = "ultima-lectura";

    private final Cache<String, LecturaAlertas> cache = Caffeine.newBuilder().maximumSize(1).build();

    @Override
    public void guardar(LecturaAlertas lectura) {
        cache.put(CLAVE, lectura);
    }

    @Override
    public Optional<LecturaAlertas> ultima() {
        return Optional.ofNullable(cache.getIfPresent(CLAVE));
    }
}
