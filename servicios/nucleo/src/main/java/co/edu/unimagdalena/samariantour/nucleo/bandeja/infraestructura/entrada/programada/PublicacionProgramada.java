package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.PublicarAvisosUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publica la bandeja de salida cada 15 s (DN-06, {@code nucleo.bandeja.publicacion.cron}). Sigue lote tras lote
 * mientras haya avisos por enviar, hasta un tope por ejecución para no ocupar la tarea indefinidamente.
 */
@Component
public class PublicacionProgramada {

    static final int LOTES_POR_EJECUCION = 50;

    private final PublicarAvisosUseCase publicar;

    public PublicacionProgramada(PublicarAvisosUseCase publicar) {
        this.publicar = publicar;
    }

    @Scheduled(cron = "${nucleo.bandeja.publicacion.cron}")
    public void publicar() {
        // Cada lote es una transacción propia: lo enviado queda guardado aunque falle el siguiente.
        int lotes = 0;
        while (lotes < LOTES_POR_EJECUCION && publicar.publicarLote() > 0) {
            lotes++;
        }
    }
}
