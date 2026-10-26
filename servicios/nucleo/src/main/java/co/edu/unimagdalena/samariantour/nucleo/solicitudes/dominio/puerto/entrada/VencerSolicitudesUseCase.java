package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

/**
 * Aplica los vencimientos automáticos (RF-41, RF-55, RF-56, DN-14): Sin respuesta a las 24 h sin atender y Vencida al
 * terminar la ventana de confirmación y pago, con la gracia de un intento de pago en curso.
 */
public interface VencerSolicitudesUseCase {

    /** Vence un lote en una transacción; devuelve cuántas venció (0 si no había ninguna por vencer). */
    int vencerLote();
}
