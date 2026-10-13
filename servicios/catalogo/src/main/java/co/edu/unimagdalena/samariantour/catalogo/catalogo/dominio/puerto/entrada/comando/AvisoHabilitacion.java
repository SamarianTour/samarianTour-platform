package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Validaciones;

import java.util.Objects;
import java.util.UUID;

/**
 * Cambio en la habilitación RNT de un prestador: habilitación, retiro o restitución (RF-77).
 *
 * @param version versión del prestador en el núcleo; crece con cada cambio y ordena los avisos
 */
public record AvisoHabilitacion(String idAviso, UUID prestadorId, boolean rntActivo, long version)
        implements AvisoNucleoCommand {

    public AvisoHabilitacion {
        idAviso = Validaciones.texto(idAviso, "idAviso");
        Objects.requireNonNull(prestadorId, "prestadorId");
        if (version < 1) {
            throw new IllegalArgumentException("La versión del prestador empieza en 1");
        }
    }
}
