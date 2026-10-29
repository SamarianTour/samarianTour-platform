package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.CerrarSolicitudesSinRntUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.VencerSolicitudesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Aplica los vencimientos cada minuto [SUPUESTO] (DN-14, {@code nucleo.solicitudes.vencimiento.cron}), y cierra las
 * solicitudes no pagadas de los prestadores sin RNT activo (RF-77, DN-36). Corre en las dos réplicas sin candado: cada
 * lote se toma con {@code FOR UPDATE SKIP LOCKED}. Sigue lote tras lote mientras haya, hasta un tope por ejecución.
 */
@Component
public class VencimientoProgramado {

    static final int LOTES_POR_EJECUCION = 20;
    private static final Logger LOG = LoggerFactory.getLogger(VencimientoProgramado.class);

    private final VencerSolicitudesUseCase vencer;
    private final CerrarSolicitudesSinRntUseCase cerrarSinRnt;

    public VencimientoProgramado(VencerSolicitudesUseCase vencer, CerrarSolicitudesSinRntUseCase cerrarSinRnt) {
        this.vencer = vencer;
        this.cerrarSinRnt = cerrarSinRnt;
    }

    @Scheduled(cron = "${nucleo.solicitudes.vencimiento.cron}")
    public void vencer() {
        int total = 0;
        for (int lote = 0; lote < LOTES_POR_EJECUCION; lote++) {
            int vencidas = vencer.vencerLote();
            total += vencidas;
            if (vencidas == 0) {
                break;
            }
        }
        if (total > 0) {
            LOG.info("solicitudes vencidas: {}", total);
        }
        int cerradas = cerrarSinRnt.cerrar();
        if (cerradas > 0) {
            LOG.info("solicitudes cerradas porque el prestador no tiene el RNT activo: {}", cerradas);
        }
    }
}
