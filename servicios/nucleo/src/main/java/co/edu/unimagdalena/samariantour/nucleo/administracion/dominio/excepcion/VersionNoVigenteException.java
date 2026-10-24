package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

/**
 * El administrador decide sobre una versión que ya no es la que está en moderación: el prestador la editó mientras
 * tanto. Tiene que volver a cargar la cola (DN-08).
 */
public class VersionNoVigenteException extends ExcepcionDominio {

    public VersionNoVigenteException(String ofertaId, int pedida, int enModeracion) {
        super("VERSION_NO_VIGENTE", "La oferta %s tiene en moderación la versión %d, no la %d"
                .formatted(ofertaId, enModeracion, pedida));
    }
}
