package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;

import java.util.Objects;

/**
 * @param contenido borrador de la primera versión; su tipo concreto define el tipo de la oferta
 */
public record PublicarOfertaCommand(Solicitante solicitante, VersionOferta contenido) {

    public PublicarOfertaCommand {
        Objects.requireNonNull(solicitante, "solicitante");
        Objects.requireNonNull(contenido, "contenido");
    }
}
