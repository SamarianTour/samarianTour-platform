package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

/** Según el catálogo, la oferta o la opinión no existe o no tiene nada en moderación (DN-08). */
public class NoEstaEnModeracionException extends ExcepcionDominio {

    public NoEstaEnModeracionException(String que) {
        super("NO_ESTA_EN_MODERACION", "%s no tiene nada en moderación".formatted(que));
    }
}
