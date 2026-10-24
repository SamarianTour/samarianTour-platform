package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface DecisionModeracionJpaRepository extends JpaRepository<DecisionModeracionEntity, UUID> {

    Optional<DecisionModeracionEntity> findByTipoAndModeradoIdAndNumeroVersion(String tipo, String moderadoId,
                                                                                Integer numeroVersion);

    Optional<DecisionModeracionEntity> findByTipoAndModeradoIdAndNumeroVersionIsNull(String tipo, String moderadoId);

    /**
     * Guarda la decisión solo si no hay otra sobre lo mismo; si la otra réplica la guardó antes, no aborta la
     * transacción: devuelve 0.
     */
    @Modifying
    @Query(value = """
            INSERT INTO decisiones_moderacion (id, tipo, moderado_id, numero_version, decision, motivo,
                                               administrador_id, decidida_en, id_aviso)
            VALUES (:id, :tipo, :moderadoId, CAST(:numeroVersion AS INTEGER), :decision, CAST(:motivo AS VARCHAR),
                    :administradorId, :decididaEn, :idAviso)
            ON CONFLICT ON CONSTRAINT decisiones_moderacion_unica DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(@Param("id") UUID id, @Param("tipo") String tipo, @Param("moderadoId") String moderadoId,
                           @Param("numeroVersion") Integer numeroVersion, @Param("decision") String decision,
                           @Param("motivo") String motivo, @Param("administradorId") UUID administradorId,
                           @Param("decididaEn") Instant decididaEn, @Param("idAviso") String idAviso);
}
