package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.FuenteTipoPrestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConsultarPrestadorUseCase;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Aporta a las cuentas el tipo del prestador para su token de acceso (DN-37). La dependencia sigue siendo de
 * prestadores hacia {@code cuentas.api}, así que no hay ciclo.
 */
@Component
public class FuenteTipoPrestadorAdapter implements FuenteTipoPrestador {

    private final ConsultarPrestadorUseCase consultas;

    public FuenteTipoPrestadorAdapter(ConsultarPrestadorUseCase consultas) {
        this.consultas = consultas;
    }

    @Override
    public Optional<String> tipoDe(UUID prestadorId) {
        return consultas.consultar(prestadorId).map(prestador -> prestador.tipo().name());
    }
}
