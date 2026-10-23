package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Registro de las fotos subidas (DC-47). El archivo vive en {@link FileStoragePort}; aquí solo queda de quién es y
 * cuándo se usó por última vez.
 *
 * <p>Asignar una foto y borrarla por falta de uso no se pisan: quien asigna marca el uso antes de comprobar que la foto
 * sigue registrada, y quien borra solo borra si el uso no cambió desde que lo leyó.
 */
public interface FotoSubidaRepositoryPort {

    /** Registra una foto recién subida. */
    void registrar(FotoSubida foto);

    /** Los registros de esas fotos, sin orden garantizado; omite las que no están registradas. */
    List<FotoSubida> buscar(Collection<Foto> fotos);

    /** Marca el uso de esas fotos en ese momento; si ya tenían uno posterior, lo conserva. Omite las que no están. */
    void marcarEnUso(Collection<Foto> fotos, Instant ahora);

    /** Fotos cuyo último uso es anterior a {@code limite}, de la más antigua a la más reciente; como máximo {@code maximo}. */
    List<FotoSubida> sinUsoDesde(Instant limite, int maximo);

    /**
     * Borra el registro solo si su último uso sigue siendo el que se leyó: si alguien la asignó después, no la borra.
     *
     * @return true si la borró
     */
    boolean eliminarSiSigueSinUso(FotoSubida leida);
}
