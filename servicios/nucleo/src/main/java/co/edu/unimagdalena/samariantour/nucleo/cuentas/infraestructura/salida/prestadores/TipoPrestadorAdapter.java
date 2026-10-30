package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.FuenteTipoPrestador;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TipoPrestadorPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * El tipo del prestador desde {@link FuenteTipoPrestador}, que implementa el módulo de prestadores (DN-37). Las
 * cuentas no importan nada de prestadores: Spring les entrega la implementación.
 */
@Component
public class TipoPrestadorAdapter implements TipoPrestadorPort {

    private final FuenteTipoPrestador fuente;

    public TipoPrestadorAdapter(FuenteTipoPrestador fuente) {
        this.fuente = fuente;
    }

    @Override
    public Optional<String> tipoDe(UUID prestadorId) {
        return fuente.tipoDe(prestadorId);
    }
}
