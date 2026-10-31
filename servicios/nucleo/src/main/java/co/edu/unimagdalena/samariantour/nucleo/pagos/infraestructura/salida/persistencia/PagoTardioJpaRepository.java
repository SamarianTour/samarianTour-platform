package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PagoTardioJpaRepository extends JpaRepository<PagoTardioEntity, UUID> {

    List<PagoTardioEntity> findAllByOrderByRegistradoEnDescIdAsc();

    /**
     * Uno que falta por avisar al administrador y no se intentó avisar desde el inicio de la corrida, el de intento
     * más antiguo primero, bloqueado; los de otra transacción se saltan.
     */
    @Query(value = """
            SELECT * FROM pagos_tardios
            WHERE aviso_enviado_en IS NULL AND (aviso_intentado_en IS NULL OR aviso_intentado_en < :inicio)
            ORDER BY aviso_intentado_en NULLS FIRST, registrado_en, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<PagoTardioEntity> tomarSinAviso(@Param("inicio") Instant inicio);

    @Modifying
    @Query(value = "UPDATE pagos_tardios SET aviso_intentado_en = :ahora WHERE id = :id", nativeQuery = true)
    int marcarAvisoIntentado(@Param("id") UUID id, @Param("ahora") Instant ahora);
}
