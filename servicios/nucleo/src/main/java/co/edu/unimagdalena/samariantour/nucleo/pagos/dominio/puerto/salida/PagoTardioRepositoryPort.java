package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PagoTardio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Los pagos tardíos en PostgreSQL (DN-22). */
public interface PagoTardioRepositoryPort {

    void guardar(PagoTardio pago);

    /** Todos, del más reciente al más antiguo, para que el administrador los reembolse (RF-50). */
    List<PagoTardio> todos();

    /**
     * Uno que todavía no se avisó al administrador ni se intentó avisar desde {@code inicio}, el de intento más antiguo
     * primero, bloqueado y saltando los de otra transacción: uno que no se puede avisar no detiene a los demás.
     */
    Optional<PagoTardio> tomarSinAviso(Instant inicio);

    void marcarAvisado(PagoTardio pago);

    /** Anota que se intentó avisarlo y no se pudo, para que espere su turno detrás de los demás. */
    void marcarAvisoIntentado(UUID id, Instant ahora);
}
