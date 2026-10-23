package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Validaciones;

import java.util.Objects;
import java.util.UUID;

/**
 * La opinión que envía el turista (RF-60). La calificación y el comentario los valida {@code Opinion}.
 *
 * @param turistaId el de la identidad verificada de la petición, nunca el del cuerpo
 */
public record PublicarOpinionCommand(UUID turistaId, String ofertaId, Integer calificacion, String comentario) {

    public PublicarOpinionCommand {
        Objects.requireNonNull(turistaId, "turistaId");
        ofertaId = Validaciones.texto(ofertaId, "ofertaId");
    }
}
