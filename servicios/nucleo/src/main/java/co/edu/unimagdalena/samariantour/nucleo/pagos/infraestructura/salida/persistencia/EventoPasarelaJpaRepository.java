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
                                          recibido_en, siguiente_intento_en)
            VALUES (:id, :claveUnica, :transaccionId, :referencia, :estado, :montoCentavos, :cuerpo, :recibidoEn,
                    :recibidoEn)
            ON CONFLICT (clave_unica) DO NOTHING
            """, nativeQuery = true)
    int guardarSiNuevo(@Param("id") UUID id, @Param("claveUnica") String claveUnica,
                       @Param("transaccionId") String transaccionId, @Param("referencia") String referencia,
                       @Param("estado") String estado, @Param("montoCentavos") long montoCentavos,
                       @Param("cuerpo") String cuerpo, @Param("recibidoEn") Instant recibidoEn);

    /** El pendiente que ya toca, el más atrasado, bloqueado; los de otra transacción se saltan (DN-21). */
    @Query(value = """
            SELECT * FROM eventos_pasarela
            WHERE procesamiento = 'PENDIENTE' AND siguiente_intento_en <= :ahora
            ORDER BY siguiente_intento_en, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<EventoPasarelaEntity> tomarSinProcesar(@Param("ahora") Instant ahora);

    @Modifying
    @Query(value = """
            UPDATE eventos_pasarela SET procesamiento = 'PROCESADO', procesado_en = :procesadoEn, resultado = :resultado
            WHERE id = :id
            """, nativeQuery = true)
    int marcarProcesado(@Param("id") UUID id, @Param("procesadoEn") Instant procesadoEn,
                        @Param("resultado") String resultado);

    @Query(value = "SELECT * FROM eventos_pasarela WHERE id = :id AND procesamiento = 'PENDIENTE' FOR UPDATE",
            nativeQuery = true)
    Optional<EventoPasarelaEntity> pendientePorId(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            UPDATE eventos_pasarela
            SET procesamiento = :procesamiento, intentos_procesamiento = :intentos, ultimo_error = :error,
                siguiente_intento_en = :siguienteIntentoEn
            WHERE id = :id
            """, nativeQuery = true)
    int registrarFallo(@Param("id") UUID id, @Param("procesamiento") String procesamiento,
                       @Param("intentos") int intentos, @Param("error") String error,
                       @Param("siguienteIntentoEn") Instant siguienteIntentoEn);

    /** Los fallidos sin avisar, del intento de aviso más antiguo al más reciente, bloqueados y saltando los ajenos. */
    @Query(value = """
            SELECT * FROM eventos_pasarela
            WHERE procesamiento = 'FALLIDO' AND aviso_enviado_en IS NULL
              AND (aviso_intentado_en IS NULL OR aviso_intentado_en < :inicio)
            ORDER BY aviso_intentado_en NULLS FIRST, siguiente_intento_en, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<EventoPasarelaEntity> tomarFallidoSinAviso(@Param("inicio") Instant inicio);

    @Modifying
    @Query(value = "UPDATE eventos_pasarela SET aviso_enviado_en = :avisadoEn WHERE id = :id", nativeQuery = true)
    int marcarAvisado(@Param("id") UUID id, @Param("avisadoEn") Instant avisadoEn);

    @Modifying
    @Query(value = "UPDATE eventos_pasarela SET aviso_intentado_en = :ahora WHERE id = :id", nativeQuery = true)
    int marcarAvisoIntentado(@Param("id") UUID id, @Param("ahora") Instant ahora);
}
