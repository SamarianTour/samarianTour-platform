package co.edu.unimagdalena.samariantour.seguridadjwt;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Escucha el registro de auditoría ({@link RegistroAccesos}) mientras dura una prueba. Los servicios la usan desde el
 * test-jar, como {@link TokensDePrueba}:
 * <pre>
 * try (AuditoriaDePrueba auditoria = AuditoriaDePrueba.escuchar()) {
 *     // la petición
 *     assertThat(auditoria.lineas()).singleElement().asString().contains("codigo=ACCESO_DENEGADO");
 * }
 * </pre>
 */
public final class AuditoriaDePrueba implements AutoCloseable {

    private final Logger logger = (Logger) LoggerFactory.getLogger(RegistroAccesos.class);
    private final ListAppender<ILoggingEvent> eventos = new ListAppender<>();

    private AuditoriaDePrueba() {
        eventos.start();
        logger.addAppender(eventos);
    }

    public static AuditoriaDePrueba escuchar() {
        return new AuditoriaDePrueba();
    }

    /** Los eventos registrados, con su nivel. */
    public List<ILoggingEvent> eventos() {
        return List.copyOf(eventos.list);
    }

    /** Las líneas registradas, ya con sus valores. */
    public List<String> lineas() {
        return eventos.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Override
    public void close() {
        logger.detachAppender(eventos);
        eventos.stop();
    }
}
