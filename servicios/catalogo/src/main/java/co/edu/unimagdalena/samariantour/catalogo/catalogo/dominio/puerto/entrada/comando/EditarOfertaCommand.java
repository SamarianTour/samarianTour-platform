package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;

import java.util.Objects;

/**
 * @param contenido borrador de la nueva versión, que reemplaza a la pendiente
 */
public record EditarOfertaCommand(String ofertaId, Solicitante solicitante, VersionOferta contenido) {

    public EditarOfertaCommand {
        Objects.requireNonNull(ofertaId, "ofertaId");
        Objects.requireNonNull(solicitante, "solicitante");
        Objects.requireNonNull(contenido, "contenido");
    }
}
