package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import java.util.Objects;
import java.util.UUID;

/**
 * El turista puede opinar sobre la oferta porque tiene una solicitud pagada de ella (RF-60). El dato es del núcleo,
 * que lo avisa al catálogo cuando la solicitud queda pagada (DC-40).
 */
public record HabilitacionOpinion(UUID turistaId, String ofertaId) {

    public HabilitacionOpinion {
        Objects.requireNonNull(turistaId, "turistaId");
        ofertaId = Validaciones.texto(ofertaId, "ofertaId");
    }
}
