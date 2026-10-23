package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import java.util.Objects;

/**
 * Lo que el turista lee de una oferta (RF-59): una página de opiniones aprobadas y el resumen de todas ellas.
 */
public record OpinionesDeOferta(Pagina<Opinion> pagina, ResumenCalificaciones resumen) {

    public OpinionesDeOferta {
        Objects.requireNonNull(pagina, "pagina");
        Objects.requireNonNull(resumen, "resumen");
    }
}
