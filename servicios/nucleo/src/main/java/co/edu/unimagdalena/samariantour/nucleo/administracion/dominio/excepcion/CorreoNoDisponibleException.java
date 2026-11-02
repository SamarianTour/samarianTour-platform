package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

/** El servidor de correo no respondió al enviar la invitación: la cuenta no se creó (DN-40). */
public class CorreoNoDisponibleException extends ExcepcionDominio {

    public CorreoNoDisponibleException(String mensaje) {
        super("CORREO_NO_DISPONIBLE", mensaje);
    }
}
