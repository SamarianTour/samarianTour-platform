package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CuentaCobroJpaRepository extends JpaRepository<CuentaCobroEntity, UUID> {

    Optional<CuentaCobroEntity> findByPrestadorIdAndVigenteHastaIsNull(UUID prestadorId);
}
