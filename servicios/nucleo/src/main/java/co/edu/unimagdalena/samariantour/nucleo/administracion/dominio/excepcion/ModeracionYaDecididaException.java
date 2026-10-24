package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;

/** Ya se tomó otra decisión sobre la misma versión u opinión (DN-08); repetir la misma no es un error. */
public class ModeracionYaDecididaException extends ExcepcionDominio {

    public ModeracionYaDecididaException(String que, Decision tomada) {
        super("MODERACION_YA_DECIDIDA", "%s ya se decidió: %s".formatted(que, tomada));
    }
}
