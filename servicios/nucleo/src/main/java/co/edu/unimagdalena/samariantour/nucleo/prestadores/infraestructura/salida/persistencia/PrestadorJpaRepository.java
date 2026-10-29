package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface PrestadorJpaRepository extends JpaRepository<PrestadorEntity, UUID> {

    /** Los que la sincronización de {@code inicio} no ha tomado; los que tiene otra réplica se saltan (DN-36). */
    @Query(value = """
            SELECT id FROM prestadores
            WHERE sincronizacion_tomada_en IS NULL OR sincronizacion_tomada_en < :inicio
            ORDER BY id
            LIMIT :limite
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<UUID> porSincronizar(Instant inicio, int limite);

    /** No toca la revisión: tomarlo no es un cambio del prestador. */
    @Modifying
    @Query(value = "UPDATE prestadores SET sincronizacion_tomada_en = :inicio WHERE id IN (:ids)", nativeQuery = true)
    void marcarTomados(Collection<UUID> ids, Instant inicio);
}
