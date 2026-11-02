package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.AsignarOperadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.AuditoriaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/** Asigna el operador del prestador y deja el cambio en la auditoría, con el anterior (RF-81, DN-39). */
public class AsignarOperadorService implements AsignarOperadorUseCase {

    private final PrestadorRepositoryPort prestadores;
    private final AuditoriaPort auditoria;
    private final Clock reloj;

    public AsignarOperadorService(PrestadorRepositoryPort prestadores, AuditoriaPort auditoria, Clock reloj) {
        this.prestadores = prestadores;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Optional<Prestador> asignar(UUID prestadorId, UUID operadorId) {
        return prestadores.porId(prestadorId).map(prestador -> {
            UUID anterior = prestador.operadorId();
            if (prestador.asignarOperador(operadorId)) {
                prestadores.actualizar(prestador);
                auditoria.registrar(prestadorId, "OPERADOR_ASIGNADO", "%s (antes %s)".formatted(operadorId,
                        anterior == null ? "sin operador" : anterior), reloj.instant());
            }
            return prestador;
        });
    }
}
