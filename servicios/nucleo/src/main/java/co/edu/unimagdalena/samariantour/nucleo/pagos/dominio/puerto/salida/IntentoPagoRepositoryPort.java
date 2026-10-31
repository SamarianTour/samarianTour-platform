package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Los intentos de pago en PostgreSQL (DN-25): referencia única y un solo intento en curso por solicitud. Guardar uno
 * que otra transacción cambió lanza {@code ConflictoConcurrenciaException} (ADR-05).
 */
public interface IntentoPagoRepositoryPort {

    /** @throws co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException si la
     *          solicitud ya tiene otro en curso: lo abrió otra petición a la vez */
    void insertar(IntentoPago intento);

    void actualizar(IntentoPago intento);

    Optional<IntentoPago> buscarPorReferencia(String referencia);

    Optional<IntentoPago> enCursoDe(UUID solicitudId);

    /** Los de la solicitud, del primero al último. */
    List<IntentoPago> deLaSolicitud(UUID solicitudId);

    /**
     * Toma el intento en curso cuya hora de abandono ya llegó, el más atrasado, y lo bloquea hasta el final de la
     * transacción; los que otra transacción tiene bloqueados se saltan (DN-20).
     */
    Optional<IntentoPago> tomarPorAbandonar(Instant ahora);
}
