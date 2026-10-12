package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

import java.util.Objects;
import java.util.UUID;

public record RetirarOfertaCommand(String ofertaId, UUID prestadorId) {

    public RetirarOfertaCommand {
        Objects.requireNonNull(ofertaId, "ofertaId");
        Objects.requireNonNull(prestadorId, "prestadorId");
    }
}
