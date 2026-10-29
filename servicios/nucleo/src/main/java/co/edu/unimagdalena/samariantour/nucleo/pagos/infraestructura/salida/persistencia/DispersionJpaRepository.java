package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DispersionJpaRepository extends JpaRepository<DispersionEntity, UUID> {

    Optional<DispersionEntity> findBySolicitudId(UUID solicitudId);

    /** Las pendientes cuyo intento ya toca, bloqueadas; las de otra transacción se saltan (DN-24). */
    @Query(value = """
            SELECT * FROM dispersiones
            WHERE estado = 'PENDIENTE' AND proximo_intento <= :ahora
            ORDER BY proximo_intento, id
            LIMIT :cantidad
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<DispersionEntity> tomarPendientes(@Param("ahora") Instant ahora, @Param("cantidad") int cantidad);

    @Query(value = """
            SELECT * FROM dispersiones
            WHERE estado = 'FALLIDA' AND aviso_enviado_en IS NULL
            ORDER BY creada_en, id
            LIMIT :cantidad
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<DispersionEntity> tomarFallidasSinAviso(@Param("cantidad") int cantidad);
}
