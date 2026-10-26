package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

import java.time.Instant;

/**
 * El plazo de la solicitud ya pasó aunque el vencimiento programado todavía no la haya cerrado: el operador ya no
 * puede atenderla ni el turista confirmarla (RF-41, RF-55). En menos de un minuto queda Sin respuesta o Vencida.
 */
public class PlazoVencidoException extends ExcepcionDominio {

    public PlazoVencidoException(Instant venceEn) {
        super("PLAZO_VENCIDO", "El plazo de la solicitud venció el " + venceEn);
    }
}
