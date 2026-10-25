package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ActualizarAlertasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Actualiza las alertas al arrancar y luego cada {@code recomendacion.alertas.actualizacion} (8 horas [SUPUESTO],
 * DR-16). Con una réplica no hace falta un candado: si se escala, cada réplica refresca su propia caché, y no pasa
 * nada porque solo lee la fuente.
 */
public class AlertasScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AlertasScheduler.class);

    private final ActualizarAlertasUseCase actualizarAlertas;

    public AlertasScheduler(ActualizarAlertasUseCase actualizarAlertas) {
        this.actualizarAlertas = actualizarAlertas;
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "${recomendacion.alertas.actualizacion:PT8H}")
    public void actualizar() {
        long inicio = System.nanoTime();
        boolean actualizada = actualizarAlertas.actualizar();
        long latencia = (System.nanoTime() - inicio) / 1_000_000;
        if (actualizada) {
            LOG.info("alertas actualizadas latenciaMs={}", latencia);
        } else {
            LOG.warn("alertas sin actualizar: se conserva la ultima lectura buena latenciaMs={}", latencia);
        }
    }
}
