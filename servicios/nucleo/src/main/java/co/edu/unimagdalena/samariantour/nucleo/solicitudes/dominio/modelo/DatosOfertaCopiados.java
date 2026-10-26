package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.util.Objects;
import java.util.UUID;

/**
 * Lo que la solicitud copia de la oferta al crearse (DC-35 del catálogo): si el prestador la edita o la retira, la
 * solicitud conserva lo que el turista pidió. Sirve para las notificaciones y los indicadores (RF-85). Sin tipo de
 * actividad: el catálogo no lo entrega en la consulta interna (DC-35).
 */
public record DatosOfertaCopiados(TipoOferta tipo, String nombre, UUID prestadorId, String zonaId) {

    public DatosOfertaCopiados {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(prestadorId, "prestadorId");
        Objects.requireNonNull(zonaId, "zonaId");
    }
}
