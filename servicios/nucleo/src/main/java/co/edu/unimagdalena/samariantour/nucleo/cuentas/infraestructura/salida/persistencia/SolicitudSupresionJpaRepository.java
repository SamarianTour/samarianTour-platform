package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SolicitudSupresionJpaRepository extends JpaRepository<SolicitudSupresionEntity, UUID> {

    Optional<SolicitudSupresionEntity> findByUsuarioId(UUID usuarioId);
}
