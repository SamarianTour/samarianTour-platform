package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DispersionJpaRepository extends JpaRepository<DispersionEntity, UUID> {

    Optional<DispersionEntity> findBySolicitudId(UUID solicitudId);

    /** La pendiente cuyo intento ya toca, la más atrasada, bloqueada; las de otra transacción se saltan (DN-24). */
    @Query(value = """
            SELECT * FROM dispersiones
            WHERE estado = 'PENDIENTE' AND proximo_intento <= :ahora
            ORDER BY proximo_intento, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<DispersionEntity> tomarPendiente(@Param("ahora") Instant ahora);

    @Query(value = "SELECT * FROM dispersiones WHERE id = :id AND estado = 'PENDIENTE' FOR UPDATE", nativeQuery = true)
    Optional<DispersionEntity> pendientePorId(@Param("id") UUID id);

    /** Una fallida sin avisar, de intento de aviso más antiguo primero, bloqueada; las ajenas se saltan. */
    @Query(value = """
            SELECT * FROM dispersiones
            WHERE estado = 'FALLIDA' AND aviso_enviado_en IS NULL
              AND (aviso_intentado_en IS NULL OR aviso_intentado_en < :inicio)
            ORDER BY aviso_intentado_en NULLS FIRST, creada_en, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<DispersionEntity> tomarFallidaSinAviso(@Param("inicio") Instant inicio);

    @Modifying
    @Query(value = "UPDATE dispersiones SET aviso_intentado_en = :ahora WHERE id = :id", nativeQuery = true)
    int marcarAvisoIntentado(@Param("id") UUID id, @Param("ahora") Instant ahora);

    /** Las fallidas del prestador, bloqueadas hasta el final de la transacción (DN-44). */
    @Query(value = """
            SELECT * FROM dispersiones WHERE prestador_id = :prestadorId AND estado = 'FALLIDA'
            ORDER BY creada_en FOR UPDATE
            """, nativeQuery = true)
    List<DispersionEntity> fallidasDelPrestador(@Param("prestadorId") UUID prestadorId);

    @Query(value = """
            SELECT count(*) FROM dispersiones WHERE prestador_id = :prestadorId AND estado IN ('PENDIENTE', 'FALLIDA')
            """, nativeQuery = true)
    long porRecibir(@Param("prestadorId") UUID prestadorId);
}
