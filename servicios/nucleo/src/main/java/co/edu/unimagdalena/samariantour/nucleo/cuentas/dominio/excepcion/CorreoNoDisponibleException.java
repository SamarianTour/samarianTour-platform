package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/** El servidor de correo no respondió: el enlace no se envió ni quedó guardado (DN-30). Se puede intentar de nuevo. */
public class CorreoNoDisponibleException extends ExcepcionDominio {

    public CorreoNoDisponibleException(Throwable causa) {
        super("CORREO_NO_DISPONIBLE", "No se pudo enviar el correo; vuelve a intentarlo en unos minutos");
        initCause(causa);
    }
}
