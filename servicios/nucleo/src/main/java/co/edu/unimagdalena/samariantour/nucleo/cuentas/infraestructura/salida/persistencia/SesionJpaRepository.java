package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

interface SesionJpaRepository extends JpaRepository<SesionEntity, UUID> {

    /**
     * Sube la revisión de cada una: una renovación que la leyó antes no puede guardarla después y deshacer el
     * cierre (ADR-05).
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE SesionEntity s SET s.revocadaEn = :ahora, s.revision = s.revision + 1
            WHERE s.usuarioId = :usuarioId AND s.revocadaEn IS NULL
            """)
    int revocarTodas(@Param("usuarioId") UUID usuarioId, @Param("ahora") Instant ahora);
}
