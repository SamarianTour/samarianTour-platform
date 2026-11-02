package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

/** Otra cuenta ya tiene el correo de la cuenta interna nueva (RF-02); el mensaje es el de las cuentas. */
public class CorreoYaRegistradoException extends ExcepcionDominio {

    public CorreoYaRegistradoException(String mensaje) {
        super("CORREO_YA_REGISTRADO", mensaje);
    }
}
