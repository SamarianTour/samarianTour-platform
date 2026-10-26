package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

import java.time.Instant;

/**
 * La ventana de confirmación y pago, con su gracia, terminaría después del inicio del servicio: la solicitud solo
 * puede marcarse No disponible (RF-39).
 */
public class SinTiempoParaConfirmarException extends ExcepcionDominio {

    public SinTiempoParaConfirmarException(Instant finVentana, Instant inicioServicio) {
        super("SIN_TIEMPO_PARA_CONFIRMAR", ("La ventana de confirmación y pago terminaría el %s, después del "
                + "inicio del servicio (%s): solo puede marcarse NO_DISPONIBLE").formatted(finVentana, inicioServicio));
    }
}
