package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;

import java.util.UUID;

/** Una oferta que la recomendación mostró, con su prestador y su posición desde 1. */
public record OfertaExpuesta(String ofertaId, UUID prestadorId, int posicion) {

    public OfertaExpuesta {
        if (ofertaId == null || ofertaId.isBlank()) {
            throw new DatoInvalidoException("ofertaId", "es obligatorio");
        }
        if (prestadorId == null) {
            throw new DatoInvalidoException("prestadorId", "es obligatorio");
        }
        if (posicion < 1) {
            throw new DatoInvalidoException("posicion", "empieza en 1");
        }
    }
}
