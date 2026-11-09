package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.modulos;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.CompromisosPrestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CompromisosPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Reúne lo que cada {@link CompromisosPrestador} que Spring encuentre tiene pendiente con el prestador (DN-45): hoy,
 * solicitudes y pagos. Prestadores no importa nada de ellos.
 */
@Component
public class CompromisosAdapter implements CompromisosPort {

    private final List<CompromisosPrestador> modulos;

    public CompromisosAdapter(List<CompromisosPrestador> modulos) {
        this.modulos = modulos;
    }

    @Override
    public List<String> pendientes(UUID prestadorId) {
        return modulos.stream().flatMap(modulo -> modulo.pendientes(prestadorId).stream()).toList();
    }
}
