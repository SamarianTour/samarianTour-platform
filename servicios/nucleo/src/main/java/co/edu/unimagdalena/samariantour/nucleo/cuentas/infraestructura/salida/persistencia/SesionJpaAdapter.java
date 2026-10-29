package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Las sesiones en {@code sesiones} (V8, DN-02). */
@Component
public class SesionJpaAdapter implements SesionRepositoryPort {

    private final SesionJpaRepository sesiones;

    SesionJpaAdapter(SesionJpaRepository sesiones) {
        this.sesiones = sesiones;
    }

    @Override
    public void insertar(Sesion sesion) {
        SesionEntity fila = new SesionEntity(sesion.id(), sesion.usuarioId(), sesion.creadaEn());
        fila.copiar(sesion.hashRenovacion(), sesion.expiraEn(), sesion.revocadaEn());
        sesiones.saveAndFlush(fila);
    }

    @Override
    public void actualizar(Sesion sesion) {
        SesionEntity fila = sesiones.findById(sesion.id())
                .orElseThrow(() -> new ConflictoConcurrenciaException(
                        "La sesión %s ya no existe".formatted(sesion.id())));
        if (!Objects.equals(fila.revision(), sesion.revision())) {
            throw new ConflictoConcurrenciaException("La sesión %s cambió desde que se leyó".formatted(sesion.id()));
        }
        fila.copiar(sesion.hashRenovacion(), sesion.expiraEn(), sesion.revocadaEn());
        try {
            sesiones.saveAndFlush(fila);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException("La sesión %s cambió desde que se leyó".formatted(sesion.id()));
        }
    }

    @Override
    public Optional<Sesion> porId(UUID id) {
        return sesiones.findById(id).map(fila -> new Sesion(fila.id(), fila.usuarioId(), fila.hashRenovacion(),
                fila.creadaEn(), fila.expiraEn(), fila.revocadaEn(), fila.revision()));
    }

    @Override
    public void revocarTodas(UUID usuarioId, Instant ahora) {
        sesiones.revocarTodas(usuarioId, ahora);
    }
}
