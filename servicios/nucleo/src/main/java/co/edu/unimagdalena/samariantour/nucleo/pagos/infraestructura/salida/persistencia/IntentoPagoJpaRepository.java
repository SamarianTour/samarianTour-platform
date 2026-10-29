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
     * Toma los en curso abiertos antes del límite y los bloquea hasta el final de la transacción; los que otra
     * transacción tiene bloqueados se saltan (DN-20).
     */
    @Query(value = """
            SELECT * FROM intentos_pago
            WHERE estado = 'EN_CURSO' AND creado_en <= :limite
            ORDER BY creado_en, id
            LIMIT :cantidad
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<IntentoPagoEntity> tomarEnCursoAbiertosAntesDe(@Param("limite") Instant limite,
                                                        @Param("cantidad") int cantidad);
}
