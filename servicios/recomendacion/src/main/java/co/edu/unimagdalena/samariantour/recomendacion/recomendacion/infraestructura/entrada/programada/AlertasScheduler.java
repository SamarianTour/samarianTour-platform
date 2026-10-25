package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ActualizarAlertasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Actualiza las alertas al arrancar y luego cada {@code recomendacion.alertas.actualizacion} (8 horas, DR-16). Si la
 * lectura del arranque falla, la caché queda vacía: mientras no haya ninguna lectura buena se reintenta cada
 * {@code recomendacion.alertas.reintento} (2 minutos), y con una lectura en la caché el reintento no hace nada.
 * <p>Las dos tareas corren en el mismo hilo del planificador de Spring, así que nunca se pisan. Con una réplica no
 * hace falta un candado: si se escala, cada réplica refresca su propia caché, y no pasa nada porque solo lee la fuente.
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

    @Scheduled(initialDelayString = "${recomendacion.alertas.reintento:PT2M}",
            fixedDelayString = "${recomendacion.alertas.reintento:PT2M}")
    public void reintentar() {
        long inicio = System.nanoTime();
        ActualizarAlertasUseCase.Reintento resultado = actualizarAlertas.reintentar();
        long latencia = (System.nanoTime() - inicio) / 1_000_000;
        switch (resultado) {
            case NO_HACE_FALTA -> { }
            case ACTUALIZADA -> LOG.info("alertas actualizadas en el reintento latenciaMs={}", latencia);
            case FALLIDA -> LOG.warn("alertas sin lectura todavia: se reintenta en el proximo ciclo latenciaMs={}",
                    latencia);
        }
    }
}
