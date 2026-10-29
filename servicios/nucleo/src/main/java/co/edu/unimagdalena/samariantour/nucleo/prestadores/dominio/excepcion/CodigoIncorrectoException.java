package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

/** El código del WhatsApp no es el enviado; quedan los intentos que dice el mensaje (DN-33). */
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
