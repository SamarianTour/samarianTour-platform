package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface ClaveIdempotenciaPagoJpaRepository
        extends JpaRepository<ClaveIdempotenciaPagoEntity, ClaveIdempotenciaPagoEntity.Llave> {

    /** Reserva la clave si no existe; si otra transacción la usa, espera a que termine. No aborta (DN-12). */
    @Modifying
    @Query(value = """
            INSERT INTO claves_idempotencia_pagos (usuario_id, clave, operacion, huella, creada_en, expira_en)
            VALUES (:usuarioId, :clave, :operacion, :huella, :ahora, :expiraEn)
            ON CONFLICT (usuario_id, clave) DO NOTHING
            """, nativeQuery = true)
    int reservar(@Param("usuarioId") UUID usuarioId, @Param("clave") UUID clave, @Param("operacion") String operacion,
                 @Param("huella") String huella, @Param("ahora") Instant ahora, @Param("expiraEn") Instant expiraEn);

    @Modifying
    @Query(value = "DELETE FROM claves_idempotencia_pagos WHERE usuario_id = :usuarioId AND clave = :clave "
            + "AND expira_en <= :ahora", nativeQuery = true)
    int borrarSiVencio(@Param("usuarioId") UUID usuarioId, @Param("clave") UUID clave, @Param("ahora") Instant ahora);

    @Modifying
    @Query(value = "UPDATE claves_idempotencia_pagos SET codigo_respuesta = :codigo, cuerpo_respuesta = :cuerpo "
            + "WHERE usuario_id = :usuarioId AND clave = :clave", nativeQuery = true)
    int completar(@Param("usuarioId") UUID usuarioId, @Param("clave") UUID clave, @Param("codigo") int codigo,
                  @Param("cuerpo") String cuerpo);

    @Query(value = "SELECT * FROM claves_idempotencia_pagos WHERE usuario_id = :usuarioId AND clave = :clave",
            nativeQuery = true)
    Optional<ClaveIdempotenciaPagoEntity> leer(@Param("usuarioId") UUID usuarioId, @Param("clave") UUID clave);

    @Modifying
    @Query(value = "DELETE FROM claves_idempotencia_pagos WHERE expira_en <= :ahora", nativeQuery = true)
    int purgarVencidas(@Param("ahora") Instant ahora);
}
