package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface EventoPasarelaJpaRepository extends JpaRepository<EventoPasarelaEntity, UUID> {

    /**
     * Guarda el evento si su clave no existe. Si otra transacción acaba de insertar la misma sin confirmar, PostgreSQL
     * espera a que termine; en ningún caso aborta la transacción, así un duplicado responde 200 igual (DN-21).
     */
    @Modifying
    @Query(value = """
            INSERT INTO eventos_pasarela (id, clave_unica, transaccion_id, referencia, estado, monto_centavos, cuerpo,
                                          recibido_en)
            VALUES (:id, :claveUnica, :transaccionId, :referencia, :estado, :montoCentavos, :cuerpo, :recibidoEn)
            ON CONFLICT (clave_unica) DO NOTHING
            """, nativeQuery = true)
    int guardarSiNuevo(@Param("id") UUID id, @Param("claveUnica") String claveUnica,
                       @Param("transaccionId") String transaccionId, @Param("referencia") String referencia,
                       @Param("estado") String estado, @Param("montoCentavos") long montoCentavos,
                       @Param("cuerpo") String cuerpo, @Param("recibidoEn") Instant recibidoEn);

    /** El más antiguo sin procesar, bloqueado; los de otra transacción se saltan (DN-21). */
    @Query(value = """
            SELECT * FROM eventos_pasarela
            WHERE procesado_en IS NULL
            ORDER BY recibido_en, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<EventoPasarelaEntity> tomarSinProcesar();

    @Modifying
    @Query(value = "UPDATE eventos_pasarela SET procesado_en = :procesadoEn, resultado = :resultado WHERE id = :id",
            nativeQuery = true)
    int marcarProcesado(@Param("id") UUID id, @Param("procesadoEn") Instant procesadoEn,
                        @Param("resultado") String resultado);
}
