package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;

import java.util.List;
import java.util.Optional;

/**
 * Almacenamiento de zonas (RF-84). El administrador las registra y las edita, y nunca se borran (DC-49).
 */
public interface ZonaRepositoryPort {

    Optional<Zona> buscarPorId(String id);

    /** Zonas activas ordenadas por nombre. */
    List<Zona> buscarActivas();

    /** Activas e inactivas, ordenadas por nombre. */
    List<Zona> buscarTodas();

    String nuevoId();

    /**
     * Inserta la zona o reemplaza todos sus datos de una vez. Si dos escrituras se cruzan, queda la última completa.
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException si otra zona
     *         tiene la misma {@link Zona#claveNombre()}; se comprueba aunque la otra la guarde otra réplica a la vez
     */
    void guardar(Zona zona);
}
