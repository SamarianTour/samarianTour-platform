package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ConsultarDispersionesPorRecibirUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.CompromisosPrestador;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Le dice al módulo de prestadores qué dinero le falta por recibir al prestador antes de suprimir sus datos (DN-45):
 * sin su cuenta de cobro ya no se le podría enviar.
 */
@Component
public class CompromisosPagosAdapter implements CompromisosPrestador {

    private final ConsultarDispersionesPorRecibirUseCase dispersiones;

    public CompromisosPagosAdapter(ConsultarDispersionesPorRecibirUseCase dispersiones) {
        this.dispersiones = dispersiones;
    }

    @Override
    public List<String> pendientes(UUID prestadorId) {
        long porRecibir = dispersiones.porRecibir(prestadorId);
        return porRecibir > 0 ? List.of(porRecibir + " dispersiones por recibir") : List.of();
    }
}
