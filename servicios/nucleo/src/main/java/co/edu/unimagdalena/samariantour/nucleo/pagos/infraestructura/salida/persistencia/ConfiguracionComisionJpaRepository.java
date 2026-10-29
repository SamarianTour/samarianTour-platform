package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ConfiguracionComisionJpaRepository extends JpaRepository<ConfiguracionComisionEntity, UUID> {

    List<ConfiguracionComisionEntity> findAllByOrderByVigenteDesdeDescIdDesc();
}
