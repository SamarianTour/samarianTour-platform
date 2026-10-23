package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Validaciones;

import java.util.Objects;
import java.util.UUID;

/**
 * El núcleo ejecutó la supresión de los datos de un turista (derechos del titular, Ley 1581 de 2012): el catálogo
 * borra sus opiniones y sus habilitaciones (DC-45).
 */
public record AvisoSupresionTurista(String idAviso, UUID turistaId) implements AvisoOpinionCommand {

    public AvisoSupresionTurista {
        idAviso = Validaciones.texto(idAviso, "idAviso");
        Objects.requireNonNull(turistaId, "turistaId");
    }
}
