package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AceptacionPoliticaJpaRepository
        extends JpaRepository<AceptacionPoliticaEntity, AceptacionPoliticaEntity.Clave> {

    List<AceptacionPoliticaEntity> findByUsuarioIdOrderByFechaAceptacionAsc(UUID usuarioId);
}
