package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

/** Otro prestador ya registró ese número de RNT [SUPUESTO] (DN-33): un RNT es de un solo prestador. */
public class RntYaRegistradoException extends ExcepcionDominio {

    public RntYaRegistradoException(String numeroRnt) {
        super("RNT_YA_REGISTRADO", "Otro prestador ya registró el RNT " + numeroRnt);
    }
}
