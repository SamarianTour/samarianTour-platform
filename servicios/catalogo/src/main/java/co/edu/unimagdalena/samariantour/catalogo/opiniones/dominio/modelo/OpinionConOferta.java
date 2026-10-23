package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import java.util.Objects;

/**
 * Una opinión con el nombre de su oferta, que da contexto al turista en "Mis opiniones" y al administrador en la
 * moderación. El nombre lo da el catálogo (DC-39).
 *
 * @param nombreOferta null si el catálogo ya no tiene la oferta
 */
public record OpinionConOferta(Opinion opinion, String nombreOferta) {

    public OpinionConOferta {
        Objects.requireNonNull(opinion, "opinion");
    }
}
