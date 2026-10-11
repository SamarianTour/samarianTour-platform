package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;

import java.util.List;
import java.util.Optional;

/**
 * Almacenamiento de zonas (RF-84). En el prototipo solo se leen.
 */
public interface ZonaRepositoryPort {

    Optional<Zona> buscarPorId(String id);

    /** Zonas activas ordenadas por nombre. */
    List<Zona> buscarActivas();
}
