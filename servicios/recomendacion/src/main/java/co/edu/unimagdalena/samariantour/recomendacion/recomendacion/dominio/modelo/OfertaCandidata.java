package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Una oferta visible del catálogo: la base de conocimiento cerrada de la recomendación (RF-26). Trae solo los datos
 * de los criterios explícitos y lo que se muestra al turista; el catálogo no manda calificaciones, comisión ni tamaño
 * del prestador (DC-36).
 *
 * @param prestadorId  solo para el registro de exposición (RET-02); nunca es criterio ni va al proveedor de IA
 * @param zonaNombre        null si el catálogo no lo pudo dar
 * @param zonaAltaAfluencia la zona está marcada como de alta afluencia (RF-29): de ahí salen las alternativas
 * @param nivelEsfuerzo     null en alojamientos y restaurantes
 */
public record OfertaCandidata(String id, TipoOferta tipo, String nombre, String descripcion, UUID prestadorId,
                              String zonaId, String zonaNombre, boolean zonaAltaAfluencia,
                              BigDecimal precioReferencia, String unidadPrecio, Set<TipoActividad> tiposActividad,
                              NivelEsfuerzo nivelEsfuerzo, boolean accesibleSillaRuedas) {

    public OfertaCandidata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(precioReferencia, "precioReferencia");
        tiposActividad = Preferencias.copia(tiposActividad, TipoActividad.class);
        descripcion = descripcion == null ? "" : descripcion;
    }
}
