package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;

import java.util.Optional;
import java.util.UUID;

/** Las cuentas, con sus aceptaciones de la política y su solicitud de supresión. */
public interface UsuarioRepositoryPort {

    /** @throws CorreoYaRegistradoException si otra cuenta ya tiene el correo, aunque se registre a la vez (RF-02) */
    void insertar(Usuario usuario);

    /**
     * Guarda los cambios. Las aceptaciones solo se agregan.
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException si la
     *         cuenta cambió desde que se leyó (ADR-05)
     */
    void actualizar(Usuario usuario);

    Optional<Usuario> porId(UUID id);

    /** @param correo ya normalizado, en minúsculas */
    Optional<Usuario> porCorreo(String correo);

    /**
     * Como {@link #porCorreo}, pero bloquea la fila hasta el final de la transacción: dos inicios de sesión de la misma
     * cuenta, aunque lleguen a réplicas distintas, se atienden uno tras otro y cada fallo queda contado (RF-05).
     */
    Optional<Usuario> porCorreoParaActualizar(String correo);
}
