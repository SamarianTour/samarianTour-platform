package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Dispersion;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Las dispersiones en PostgreSQL (DN-24): una por solicitud. */
public interface DispersionRepositoryPort {

    void insertar(Dispersion dispersion);

    void actualizar(Dispersion dispersion);

    Optional<Dispersion> deLaSolicitud(UUID solicitudId);

    /**
     * La pendiente cuyo próximo intento ya toca, la más atrasada, bloqueada hasta el final de la transacción; las de
     * otra transacción se saltan, así dos réplicas nunca envían la misma.
     */
    Optional<Dispersion> tomarPendiente(Instant ahora);

    /**
     * La dispersión si sigue pendiente, bloqueada hasta el final de la transacción (espera a quien la tenga): para
     * registrar un error inesperado sin pisar a la réplica que la haya ejecutado mientras tanto.
     */
    Optional<Dispersion> pendientePorId(UUID id);

    /**
     * Una fallida que todavía no se avisó al prestador ni se intentó avisar desde {@code inicio}, la de intento más
     * antiguo primero, bloqueada y saltando las de otra transacción: una que no se puede avisar no detiene a las demás.
     */
    Optional<Dispersion> tomarFallidaSinAviso(Instant inicio);

    /** Anota que se intentó avisarla y no se pudo, para que espere su turno detrás de las demás. */
    void marcarAvisoIntentado(UUID id, Instant ahora);
}
