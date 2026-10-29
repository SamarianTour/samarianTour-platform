package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CodigoUnUsoJpaRepository extends JpaRepository<CodigoUnUsoEntity, UUID> {

    Optional<CodigoUnUsoEntity> findFirstByUsuarioIdAndPropositoOrderByEmitidoEnDesc(UUID usuarioId,
                                                                                     String proposito);
}
