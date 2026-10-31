package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface IntentoPagoJpaRepository extends JpaRepository<IntentoPagoEntity, UUID> {

    Optional<IntentoPagoEntity> findByReferencia(String referencia);

    Optional<IntentoPagoEntity> findBySolicitudIdAndEstado(UUID solicitudId, String estado);

    List<IntentoPagoEntity> findBySolicitudIdOrderByNumeroAsc(UUID solicitudId);

    /**
     * El en curso cuya hora de abandono ya llegó, el de la hora más antigua, bloqueado hasta el final de la
     * transacción; los que otra transacción tiene bloqueados se saltan (DN-20).
     */
    @Query(value = """
            SELECT * FROM intentos_pago
            WHERE estado = 'EN_CURSO' AND abandonar_en <= :ahora
            ORDER BY abandonar_en, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<IntentoPagoEntity> tomarPorAbandonar(@Param("ahora") Instant ahora);
}
