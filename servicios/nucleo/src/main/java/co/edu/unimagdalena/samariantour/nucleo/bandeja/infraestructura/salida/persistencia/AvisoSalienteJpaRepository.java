package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface AvisoSalienteJpaRepository extends JpaRepository<AvisoSalienteEntity, String> {

    /** Inserta el aviso solo si no hay otro con el mismo identificador; sin abortar la transacción de quien encola. */
    @Modifying
    @Query(value = """
            INSERT INTO bandeja_salida (id_aviso, destino, cuerpo, estado, creado_en, proximo_intento, intentos)
            VALUES (:idAviso, :destino, CAST(:cuerpo AS jsonb), 'PENDIENTE', :ahora, :ahora, 0)
            ON CONFLICT (id_aviso) DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(@Param("idAviso") String idAviso, @Param("destino") String destino,
                           @Param("cuerpo") String cuerpo, @Param("ahora") Instant ahora);

    /**
     * Toma los pendientes que ya tocan y los bloquea hasta el final de la transacción; los que otra transacción tiene
     * bloqueados se saltan, así dos réplicas nunca envían el mismo aviso a la vez.
     */
    @Query(value = """
            SELECT * FROM bandeja_salida
            WHERE estado = 'PENDIENTE' AND proximo_intento <= :ahora
            ORDER BY proximo_intento, creado_en
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AvisoSalienteEntity> tomarPendientes(@Param("ahora") Instant ahora, @Param("limite") int limite);

    /** Los rechazados, del más reciente al más antiguo (DN-44). */
    @Query(value = "SELECT * FROM bandeja_salida WHERE estado = 'RECHAZADO' ORDER BY creado_en DESC",
            nativeQuery = true)
    List<AvisoSalienteEntity> rechazados();

    /** El aviso bloqueado hasta el final de la transacción. */
    @Query(value = "SELECT * FROM bandeja_salida WHERE id_aviso = :idAviso FOR UPDATE", nativeQuery = true)
    Optional<AvisoSalienteEntity> buscarParaCambiar(@Param("idAviso") String idAviso);

    @Modifying
    @Query(value = "DELETE FROM bandeja_salida WHERE estado = 'ENVIADO' AND enviado_en < :limite",
            nativeQuery = true)
    int borrarEnviadosAntesDe(@Param("limite") Instant limite);
}
