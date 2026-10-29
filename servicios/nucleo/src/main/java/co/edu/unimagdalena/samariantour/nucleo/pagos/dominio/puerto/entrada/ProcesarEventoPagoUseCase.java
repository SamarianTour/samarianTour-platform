package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

/**
 * Lleva a los intentos y a las solicitudes lo que informó la pasarela (RF-50 a RF-53, RF-57, RF-58, DN-21), y abandona
 * los intentos que se quedaron sin resultado (DN-20). Lo llama una tarea programada.
 */
public interface ProcesarEventoPagoUseCase {

    /** @return cuántos eventos procesó */
    int procesarPendientes();

    /** @return cuántos intentos abandonó */
    int abandonarIntentos();
}
