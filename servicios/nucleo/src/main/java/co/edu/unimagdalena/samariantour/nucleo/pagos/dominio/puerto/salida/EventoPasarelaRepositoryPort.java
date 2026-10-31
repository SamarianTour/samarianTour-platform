package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Los eventos de la pasarela en PostgreSQL, únicos por su clave (DN-21). */
public interface EventoPasarelaRepositoryPort {

    /** @return false si ya había uno con la misma clave: un duplicado no se guarda otra vez */
    boolean guardarSiNuevo(EventoPasarela evento);

    /**
     * El pendiente cuyo próximo intento ya toca, el más atrasado, bloqueado hasta el final de la transacción; los que
     * otra transacción tiene bloqueados se saltan, así dos réplicas nunca procesan el mismo. Uno que falló espera su
     * turno, así que nunca queda de primero en cada corrida.
     */
    Optional<EventoPasarela> tomarSinProcesar(Instant ahora);

    void marcarProcesado(EventoPasarela evento);

    /**
     * El evento si sigue pendiente, bloqueado hasta el final de la transacción (espera al que lo tenga): para
     * registrar un fallo sin pisar a la réplica que lo haya procesado mientras tanto.
     */
    Optional<EventoPasarela> pendientePorId(UUID id);

    /** Guarda el fallo: los intentos, el último error y el próximo intento, o que quedó fallido. */
    void registrarFallo(EventoPasarela evento);

    /**
     * Un fallido sin avisar al administrador que no se haya intentado avisar desde {@code inicio}, el de intento más
     * antiguo primero, bloqueado y saltando los de otra transacción.
     */
    Optional<EventoPasarela> tomarFallidoSinAviso(Instant inicio);

    void marcarAvisado(EventoPasarela evento);

    /** Anota que se intentó avisarlo y no se pudo, para que espere su turno detrás de los demás. */
    void marcarAvisoIntentado(UUID id, Instant ahora);
}
