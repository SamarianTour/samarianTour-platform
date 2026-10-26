package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface TransicionSolicitudJpaRepository extends JpaRepository<TransicionSolicitudEntity, UUID> {

    List<TransicionSolicitudEntity> findBySolicitudIdInOrderBySolicitudIdAscOrdenAsc(Collection<UUID> solicitudIds);

    long countBySolicitudId(UUID solicitudId);
}
