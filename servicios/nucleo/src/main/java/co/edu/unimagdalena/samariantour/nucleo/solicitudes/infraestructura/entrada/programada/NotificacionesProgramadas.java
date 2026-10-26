package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.EnviarNotificacionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.PurgarClavesIdempotenciaUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Envía las notificaciones de WhatsApp pendientes cada 10 s [SUPUESTO] (DN-13) y borra cada hora las claves de
 * idempotencia vencidas (DN-12). Como la bandeja, sin candado entre réplicas: el envío toma cada lote con
 * {@code FOR UPDATE SKIP LOCKED}, y borrar dos veces lo vencido no hace daño.
 */
@Component
public class NotificacionesProgramadas {

    static final int LOTES_POR_EJECUCION = 20;

    private final EnviarNotificacionesUseCase enviar;
    private final PurgarClavesIdempotenciaUseCase purgar;

    public NotificacionesProgramadas(EnviarNotificacionesUseCase enviar, PurgarClavesIdempotenciaUseCase purgar) {
        this.enviar = enviar;
        this.purgar = purgar;
    }

    @Scheduled(cron = "${nucleo.solicitudes.notificaciones.cron}")
    public void enviar() {
        int lotes = 0;
        while (lotes < LOTES_POR_EJECUCION && enviar.enviarLote() > 0) {
            lotes++;
        }
    }

    @Scheduled(cron = "${nucleo.solicitudes.idempotencia.cron}")
    public void purgarClaves() {
        purgar.purgar();
    }
}
