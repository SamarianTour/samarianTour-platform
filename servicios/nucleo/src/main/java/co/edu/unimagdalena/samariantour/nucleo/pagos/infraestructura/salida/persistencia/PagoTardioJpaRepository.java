package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface PagoTardioJpaRepository extends JpaRepository<PagoTardioEntity, UUID> {

    List<PagoTardioEntity> findAllByOrderByRegistradoEnDescIdAsc();

    /** Los que faltan por avisar al administrador, bloqueados; los de otra transacción se saltan. */
    @Query(value = """
            SELECT * FROM pagos_tardios
            WHERE aviso_enviado_en IS NULL
            ORDER BY registrado_en, id
            LIMIT :cantidad
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PagoTardioEntity> tomarSinAviso(@Param("cantidad") int cantidad);
}
