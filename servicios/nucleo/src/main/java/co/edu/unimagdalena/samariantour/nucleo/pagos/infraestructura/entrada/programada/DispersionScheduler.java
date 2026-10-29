package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.EjecutarDispersionesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecuta las dispersiones pendientes y envía los avisos de pagos (RF-50, RF-54, DN-22, DN-24) cada 15 s [SUPUESTO]
 * ({@code nucleo.pagos.dispersiones.cron}), en las dos réplicas sin candado: cada lote se toma con
 * {@code FOR UPDATE SKIP LOCKED}. Con {@code -} no corre.
 */
@Component
public class DispersionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(DispersionScheduler.class);

    private final EjecutarDispersionesUseCase dispersiones;

    public DispersionScheduler(EjecutarDispersionesUseCase dispersiones) {
        this.dispersiones = dispersiones;
    }

    @Scheduled(cron = "${nucleo.pagos.dispersiones.cron}")
    public void ejecutar() {
        int intentadas = dispersiones.ejecutarPendientes();
        int avisos = dispersiones.enviarAvisos();
        if (intentadas > 0 || avisos > 0) {
            LOG.info("dispersiones intentadas: {}; avisos de pagos enviados: {}", intentadas, avisos);
        }
    }
}
