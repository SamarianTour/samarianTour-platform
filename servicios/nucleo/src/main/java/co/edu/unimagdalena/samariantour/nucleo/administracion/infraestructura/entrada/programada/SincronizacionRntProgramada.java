package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.ResultadoSincronizacionRnt;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.SincronizarRntAdminUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * La sincronización semanal con el RNT (RF-76, DN-36), los lunes a las 3:00 de Colombia
 * ({@code nucleo.administracion.sincronizacion-rnt.cron}). Corre en las dos réplicas sin candado: los prestadores se
 * reparten con {@code FOR UPDATE SKIP LOCKED}. Con {@code -} no corre.
 */
@Component
public class SincronizacionRntProgramada {

    private static final Logger LOG = LoggerFactory.getLogger(SincronizacionRntProgramada.class);

    private final SincronizarRntAdminUseCase sincronizacion;

    public SincronizacionRntProgramada(SincronizarRntAdminUseCase sincronizacion) {
        this.sincronizacion = sincronizacion;
    }

    @Scheduled(cron = "${nucleo.administracion.sincronizacion-rnt.cron}", zone = "America/Bogota")
    public void sincronizar() {
        ResultadoSincronizacionRnt resultado = sincronizacion.sincronizar();
        LOG.info("sincronización RNT: {} revisados, {} retirados, {} restituidos, {} sin respuesta{}",
                resultado.revisados(), resultado.retirados(), resultado.restituidos(), resultado.sinRespuesta(),
                resultado.interrumpida() ? "; se interrumpió porque el RNT no responde" : "");
    }
}
