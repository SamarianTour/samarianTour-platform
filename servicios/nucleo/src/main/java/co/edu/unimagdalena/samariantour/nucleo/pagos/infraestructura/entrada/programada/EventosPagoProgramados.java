package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ProcesarEventoPagoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.PurgarClavesIdempotenciaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Las tareas de los eventos de pago (DN-20, DN-21), en las dos réplicas sin candado: cada una toma su trabajo con
 * {@code FOR UPDATE SKIP LOCKED}.
 * <ul>
 *   <li>los eventos guardados por el webhook, cada 5 s [SUPUESTO] ({@code nucleo.pagos.eventos.cron});</li>
 *   <li>los intentos sin resultado a los 30 min, cada minuto ({@code nucleo.pagos.abandonos.cron});</li>
 *   <li>las claves de idempotencia vencidas, cada hora ({@code nucleo.pagos.idempotencia.cron}).</li>
 * </ul>
 * Con {@code -} una tarea no corre (por ejemplo, en las pruebas).
 */
@Component
public class EventosPagoProgramados {

    static final int LOTES_POR_EJECUCION = 10;
    private static final Logger LOG = LoggerFactory.getLogger(EventosPagoProgramados.class);

    private final ProcesarEventoPagoUseCase eventos;
    private final PurgarClavesIdempotenciaUseCase claves;

    public EventosPagoProgramados(ProcesarEventoPagoUseCase eventos, PurgarClavesIdempotenciaUseCase claves) {
        this.eventos = eventos;
        this.claves = claves;
    }

    @Scheduled(cron = "${nucleo.pagos.eventos.cron}")
    public void procesarEventos() {
        int total = 0;
        for (int lote = 0; lote < LOTES_POR_EJECUCION; lote++) {
            int procesados = eventos.procesarPendientes();
            total += procesados;
            if (procesados == 0) {
                break;
            }
        }
        if (total > 0) {
            LOG.info("eventos de pago procesados: {}", total);
        }
    }

    @Scheduled(cron = "${nucleo.pagos.abandonos.cron}")
    public void abandonarIntentos() {
        int abandonados = eventos.abandonarIntentos();
        if (abandonados > 0) {
            LOG.info("intentos de pago abandonados: {}", abandonados);
        }
    }

    @Scheduled(cron = "${nucleo.pagos.idempotencia.cron}")
    public void purgarClaves() {
        claves.purgar();
    }
}
