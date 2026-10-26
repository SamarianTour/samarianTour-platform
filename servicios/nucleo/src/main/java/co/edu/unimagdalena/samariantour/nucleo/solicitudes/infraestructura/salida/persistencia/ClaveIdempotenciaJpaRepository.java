package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface ClaveIdempotenciaJpaRepository extends JpaRepository<ClaveIdempotenciaEntity, ClaveIdempotenciaEntity.Llave> {

    /**
     * Reserva la clave si no existe. Si otra transacción la acaba de insertar sin confirmar, PostgreSQL espera a que
     * termine: si confirma, no inserta nada (0); si se deshace, inserta (1). No aborta la transacción en ningún caso.
     */
    @Modifying
    @Query(value = """
            INSERT INTO claves_idempotencia (usuario_id, clave, operacion, huella, creada_en, expira_en)
            VALUES (:usuarioId, :clave, :operacion, :huella, :ahora, :expiraEn)
            ON CONFLICT (usuario_id, clave) DO NOTHING
            """, nativeQuery = true)
    int reservar(@Param("usuarioId") UUID usuarioId, @Param("clave") UUID clave, @Param("operacion") String operacion,
                 @Param("huella") String huella, @Param("ahora") Instant ahora, @Param("expiraEn") Instant expiraEn);

    /** La clave vencida deja de contar: se borra antes de reservarla otra vez. */
    @Modifying
    @Query(value = "DELETE FROM claves_idempotencia WHERE usuario_id = :usuarioId AND clave = :clave "
            + "AND expira_en <= :ahora", nativeQuery = true)
    int borrarSiVencio(@Param("usuarioId") UUID usuarioId, @Param("clave") UUID clave, @Param("ahora") Instant ahora);

    @Modifying
    @Query(value = "UPDATE claves_idempotencia SET codigo_respuesta = :codigo, cuerpo_respuesta = :cuerpo "
            + "WHERE usuario_id = :usuarioId AND clave = :clave", nativeQuery = true)
    int completar(@Param("usuarioId") UUID usuarioId, @Param("clave") UUID clave, @Param("codigo") int codigo,
                  @Param("cuerpo") String cuerpo);

    @Query(value = "SELECT * FROM claves_idempotencia WHERE usuario_id = :usuarioId AND clave = :clave",
            nativeQuery = true)
    Optional<ClaveIdempotenciaEntity> leer(@Param("usuarioId") UUID usuarioId, @Param("clave") UUID clave);

    @Modifying
    @Query(value = "DELETE FROM claves_idempotencia WHERE expira_en <= :ahora", nativeQuery = true)
    int purgarVencidas(@Param("ahora") Instant ahora);
}
