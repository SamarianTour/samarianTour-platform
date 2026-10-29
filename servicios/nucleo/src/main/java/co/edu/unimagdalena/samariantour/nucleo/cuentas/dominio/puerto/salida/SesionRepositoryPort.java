package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Las sesiones con su token de renovación (RF-04, RF-06). */
public interface SesionRepositoryPort {

    void insertar(Sesion sesion);

    /** Con bloqueo optimista: dos renovaciones a la vez con el mismo token no dan dos sesiones válidas. */
    void actualizar(Sesion sesion);

    Optional<Sesion> porId(UUID id);

    /** Cierra todas las sesiones del usuario: al cambiar la contraseña o al suprimir la cuenta. */
    void revocarTodas(UUID usuarioId, Instant ahora);
}
