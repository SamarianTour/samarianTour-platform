package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoIntento;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IntentoPagoRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Los intentos de pago en {@code intentos_pago} (DN-25). Si dos peticiones abren un intento de la misma solicitud a la
 * vez, el índice de un solo intento en curso deja pasar una: la otra es un conflicto de concurrencia, y al reintentarla
 * encuentra el intento ya abierto y lo devuelve (ADR-05).
 */
@Component
public class IntentoPagoJpaAdapter implements IntentoPagoRepositoryPort {

    static final String INDICE_UNO_EN_CURSO = "intentos_pago_uno_en_curso";

    private final IntentoPagoJpaRepository intentos;

    IntentoPagoJpaAdapter(IntentoPagoJpaRepository intentos) {
        this.intentos = intentos;
    }

    @Override
    public void insertar(IntentoPago intento) {
        IntentoPagoEntity fila = new IntentoPagoEntity(intento.id(), intento.solicitudId(), intento.turistaId(),
                intento.numero(), intento.referencia(), intento.montoCentavos(), intento.creadoEn());
        fila.cambiarEstado(intento.estado().name(), intento.transaccionId(), intento.resueltoEn(),
                intento.abandonarEn());
        try {
            intentos.saveAndFlush(fila);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(INDICE_UNO_EN_CURSO)) {
                throw new ConflictoConcurrenciaException(
                        "La solicitud " + intento.solicitudId() + " ya tiene un intento de pago en curso");
            }
            throw e;
        }
    }

    @Override
    public void actualizar(IntentoPago intento) {
        IntentoPagoEntity fila = intentos.findById(intento.id()).orElseThrow(() -> new ConflictoConcurrenciaException(
                "El intento " + intento.id() + " ya no existe"));
        if (!Objects.equals(fila.revision(), intento.revision())) {
            throw new ConflictoConcurrenciaException("El intento " + intento.id() + " cambió desde que se leyó");
        }
        fila.cambiarEstado(intento.estado().name(), intento.transaccionId(), intento.resueltoEn(),
                intento.abandonarEn());
        try {
            intentos.saveAndFlush(fila);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException("El intento " + intento.id() + " cambió desde que se leyó");
        }
    }

    @Override
    public Optional<IntentoPago> buscarPorReferencia(String referencia) {
        return intentos.findByReferencia(referencia).map(IntentoPagoJpaAdapter::aDominio);
    }

    @Override
    public Optional<IntentoPago> enCursoDe(UUID solicitudId) {
        return intentos.findBySolicitudIdAndEstado(solicitudId, EstadoIntento.EN_CURSO.name())
                .map(IntentoPagoJpaAdapter::aDominio);
    }

    @Override
    public List<IntentoPago> deLaSolicitud(UUID solicitudId) {
        return intentos.findBySolicitudIdOrderByNumeroAsc(solicitudId).stream()
                .map(IntentoPagoJpaAdapter::aDominio).toList();
    }

    @Override
    public Optional<IntentoPago> tomarPorAbandonar(Instant ahora) {
        return intentos.tomarPorAbandonar(ahora).map(IntentoPagoJpaAdapter::aDominio);
    }

    private static IntentoPago aDominio(IntentoPagoEntity fila) {
        return IntentoPago.reconstituir(fila.id(), fila.solicitudId(), fila.turistaId(), fila.numero(),
                fila.referencia(), fila.montoCentavos(), fila.creadoEn(), fila.abandonarEn(),
                EstadoIntento.valueOf(fila.estado()), fila.transaccionId(), fila.resueltoEn(), fila.revision());
    }
}
