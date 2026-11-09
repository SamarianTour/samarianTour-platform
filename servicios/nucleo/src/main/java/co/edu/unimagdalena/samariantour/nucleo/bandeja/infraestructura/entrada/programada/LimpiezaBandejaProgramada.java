package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.LimpiarBandejaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Una vez al día borra los avisos enviados hace más de 30 días (DN-44). Corre en las dos réplicas sin candado: borrar
 * lo que la otra ya borró no hace nada.
 */
@Component
public class LimpiezaBandejaProgramada {

    private static final Logger LOG = LoggerFactory.getLogger(LimpiezaBandejaProgramada.class);

    private final LimpiarBandejaUseCase limpiar;

    public LimpiezaBandejaProgramada(LimpiarBandejaUseCase limpiar) {
        this.limpiar = limpiar;
    }

    @Scheduled(cron = "${nucleo.bandeja.limpieza.cron}", zone = "America/Bogota")
    public void limpiar() {
        int borrados = limpiar.limpiar();
        if (borrados > 0) {
            LOG.info("bandeja: {} avisos enviados hace más de 30 días borrados", borrados);
        }
    }
}
