package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

/**
 * Cierra las solicitudes no pagadas de los prestadores que perdieron la habilitación RNT (RF-77, HU-ADM-02, DN-36):
 * las Pendientes quedan No disponibles y las que esperaban el pago, Vencidas, con su notificación al turista.
 */
public interface CerrarSolicitudesSinRntUseCase {

    /** Devuelve cuántas cerró (0 si no había ninguna). */
    int cerrar();
}
