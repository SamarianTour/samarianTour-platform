package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SolicitudJpaRepository extends JpaRepository<SolicitudEntity, UUID> {

    List<SolicitudEntity> findByTuristaIdAndOfertaIdAndFechaInicioAndEstadoIn(UUID turistaId, String ofertaId,
                                                                              LocalDate fechaInicio,
                                                                              Collection<String> estados);

    Page<SolicitudEntity> findByTuristaIdOrderByCreadaEnDescIdAsc(UUID turistaId, Pageable pagina);

    Page<SolicitudEntity> findByOperadorIdAndEstadoOrderByCreadaEnAscIdAsc(UUID operadorId, String estado,
                                                                          Pageable pagina);

    Page<SolicitudEntity> findByOperadorIdAndEstadoOrderByCreadaEnDescIdAsc(UUID operadorId, String estado,
                                                                           Pageable pagina);

    /**
     * Toma las abiertas cuyo vencimiento ya toca y las bloquea hasta el final de la transacción; las que otra
     * transacción tiene bloqueadas se saltan, así dos réplicas nunca vencen la misma (DN-14).
     */
    @Query(value = """
            SELECT * FROM solicitudes
            WHERE estado IN ('PENDIENTE', 'ATENDIDA', 'CONFIRMADA', 'PAGO_FALLIDO')
              AND limite_vencimiento <= :ahora
            ORDER BY limite_vencimiento, id
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SolicitudEntity> tomarPorVencer(@Param("ahora") Instant ahora, @Param("limite") int limite);
}
