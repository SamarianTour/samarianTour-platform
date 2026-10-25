package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.Objects;

/**
 * Una zona del catálogo, con su ubicación: con ella se consultan sus alertas por punto (DR-15).
 *
 * @param nombre   null si el catálogo no lo dio
 * @param latitud  null si la zona no tiene ubicación
 * @param longitud null si la zona no tiene ubicación
 */
public record ZonaCatalogo(String id, String nombre, Double latitud, Double longitud) {

    public ZonaCatalogo {
        Objects.requireNonNull(id, "id");
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }
}
