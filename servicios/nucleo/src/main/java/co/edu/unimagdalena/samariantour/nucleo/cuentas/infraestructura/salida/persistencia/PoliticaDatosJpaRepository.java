package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PoliticaDatosJpaRepository extends JpaRepository<PoliticaDatosEntity, String> {

    Optional<PoliticaDatosEntity> findFirstByOrderByFechaPublicacionDescVersionDesc();
}
