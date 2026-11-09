package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Validaciones;

import java.util.Objects;
import java.util.UUID;

/** El prestador suprimió sus datos en el núcleo (RF-13, DN-45): se retiran sus ofertas y se borran perfil y fotos. */
public record AvisoSupresionPrestador(String idAviso, UUID prestadorId) implements AvisoNucleoCommand {

    public AvisoSupresionPrestador {
        idAviso = Validaciones.texto(idAviso, "idAviso");
        Objects.requireNonNull(prestadorId, "prestadorId");
    }
}
