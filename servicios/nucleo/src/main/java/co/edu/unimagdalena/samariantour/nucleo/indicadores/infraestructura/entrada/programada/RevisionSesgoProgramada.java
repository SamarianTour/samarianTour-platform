package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RevisionSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.EvaluarIndiceExposicionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El {@code RevisionSesgoScheduler} del diagrama de paquetes (RET-02, DN-43): cada hora intenta la revisión del día
 * ({@code nucleo.indicadores.revision-sesgo.cron}). Corre en las dos réplicas sin candado: la fila del día en
 * {@code revisiones_sesgo} deja pasar a una sola. Con {@code -} no corre.
 */
@Component
public class RevisionSesgoProgramada {

    private static final Logger LOG = LoggerFactory.getLogger(RevisionSesgoProgramada.class);

    private final EvaluarIndiceExposicionUseCase evaluacion;

    public RevisionSesgoProgramada(EvaluarIndiceExposicionUseCase evaluacion) {
        this.evaluacion = evaluacion;
    }

    @Scheduled(cron = "${nucleo.indicadores.revision-sesgo.cron}", zone = "America/Bogota")
    public void revisar() {
        RevisionSesgo revision = evaluacion.evaluar();
        if (revision.resultado() != RevisionSesgo.Resultado.YA_REVISADA) {
            LOG.info("revisión del sesgo del {}: {}, {} alertas abiertas, {} actualizadas, {} cerradas", revision.dia(),
                    revision.resultado(), revision.abiertas(), revision.actualizadas(), revision.cerradas());
        }
    }
}
