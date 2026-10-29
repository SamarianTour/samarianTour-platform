package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/** Ya hay una cuenta con ese correo (RF-02). */
public class CorreoYaRegistradoException extends ExcepcionDominio {

    public CorreoYaRegistradoException(String correo) {
        super("CORREO_YA_REGISTRADO", "Ya hay una cuenta con el correo " + correo);
    }
}
