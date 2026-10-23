package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.GuardarPerfilCommand;

import java.util.UUID;

/**
 * El prestador gestiona su perfil público (RF-64, RF-72). Los cambios se publican sin moderación (DC-39).
 */
public interface GestionarPerfilPublicoUseCase {

    /** @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PerfilNoEncontradoException si todavía no lo creó */
    PerfilPublico consultar(UUID prestadorId);

    /** Crea el perfil o reemplaza sus datos; las fotos que ya tiene se conservan. */
    PerfilPublico guardar(GuardarPerfilCommand comando);

    /**
     * Agrega una foto al final del perfil.
     *
     * @param imagen el archivo que subió el prestador: JPG, PNG o WebP de hasta 5 MB
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PerfilNoEncontradoException si todavía no creó el perfil
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException si no es una imagen aceptada, pesa más de 5 MB o el perfil ya tiene 6 fotos
     */
    PerfilPublico agregarFoto(UUID prestadorId, byte[] imagen);

    /**
     * Quita la foto del perfil y la borra del almacenamiento. Si el perfil no la tiene, no hace nada: borrar dos veces
     * no es un error.
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PerfilNoEncontradoException si todavía no creó el perfil
     */
    void quitarFoto(UUID prestadorId, String fotoId);
}
