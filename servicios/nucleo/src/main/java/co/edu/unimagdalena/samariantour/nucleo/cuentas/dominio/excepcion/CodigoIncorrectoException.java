package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/** El código no es el que se envió; quedan los intentos que dice el mensaje (DN-18). */
public class CodigoIncorrectoException extends ExcepcionDominio {

    private final int intentosRestantes;

    public CodigoIncorrectoException(int intentosRestantes) {
        super("CODIGO_INCORRECTO", intentosRestantes == 0
                ? "El código no es correcto y ya no quedan intentos: pide uno nuevo"
                : "El código no es correcto; quedan %d intentos".formatted(intentosRestantes));
        this.intentosRestantes = intentosRestantes;
    }

    public int intentosRestantes() {
        return intentosRestantes;
    }
}
