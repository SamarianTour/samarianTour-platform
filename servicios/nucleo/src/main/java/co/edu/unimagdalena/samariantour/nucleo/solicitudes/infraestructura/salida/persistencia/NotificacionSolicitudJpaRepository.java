package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface NotificacionSolicitudJpaRepository extends JpaRepository<NotificacionSolicitudEntity, UUID> {

    /**
     * Toma las de WhatsApp pendientes cuyo intento ya toca y las bloquea hasta el final de la transacción; las que otra
     * transacción tiene bloqueadas se saltan, como la bandeja (DN-06).
     */
    @Query(value = """
            SELECT * FROM notificaciones_solicitud
            WHERE estado = 'PENDIENTE' AND proximo_intento <= :ahora
            ORDER BY proximo_intento, creada_en
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificacionSolicitudEntity> tomarPendientes(@Param("ahora") Instant ahora, @Param("limite") int limite);

    Page<NotificacionSolicitudEntity> findByCanalAndDestinatarioTipoAndDestinatarioIdOrderByCreadaEnDescIdAsc(
            String canal, String destinatarioTipo, UUID destinatarioId, Pageable pagina);
}
