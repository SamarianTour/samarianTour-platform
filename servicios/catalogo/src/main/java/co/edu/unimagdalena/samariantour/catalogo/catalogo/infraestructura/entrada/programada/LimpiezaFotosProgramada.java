package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.LimpiarFotosSinUsoUseCase;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Borra cada hora las fotos que nadie usa (DC-47). Con el perfil {@code mongo} las réplicas comparten los datos, así
 * que un candado en MongoDB (ShedLock) deja que solo una la ejecute; con los datos en memoria cada réplica limpia los
 * suyos.
 */
@Component
public class LimpiezaFotosProgramada {

    private static final Logger LOG = LoggerFactory.getLogger(LimpiezaFotosProgramada.class);

    private final LimpiarFotosSinUsoUseCase limpieza;

    public LimpiezaFotosProgramada(LimpiarFotosSinUsoUseCase limpieza) {
        this.limpieza = limpieza;
    }

    /**
     * El candado dura al menos 5 minutos, para que la otra réplica no la repita en seguida si su reloj va unos segundos
     * atrasado, y como mucho 30, por si la réplica que lo tiene se cae.
     */
    @Scheduled(cron = "${catalogo.fotos.limpieza.cron}")
    @SchedulerLock(name = "limpiar-fotos-sin-uso", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void limpiar() {
        int borradas = limpieza.limpiar();
        if (borradas > 0) {
            LOG.info("Se borraron {} fotos sin uso", borradas);
        }
    }
}
