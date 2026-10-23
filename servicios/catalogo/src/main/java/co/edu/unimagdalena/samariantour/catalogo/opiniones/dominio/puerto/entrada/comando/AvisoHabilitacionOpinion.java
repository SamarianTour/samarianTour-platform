package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Validaciones;

import java.util.Objects;
import java.util.UUID;

/**
 * Una solicitud del turista quedó pagada, así que puede opinar sobre su oferta (RF-60, DC-40).
 */
public record AvisoHabilitacionOpinion(String idAviso, UUID turistaId, String ofertaId)
        implements AvisoOpinionCommand {

    public AvisoHabilitacionOpinion {
        idAviso = Validaciones.texto(idAviso, "idAviso");
        Objects.requireNonNull(turistaId, "turistaId");
        ofertaId = Validaciones.texto(ofertaId, "ofertaId");
    }
}
