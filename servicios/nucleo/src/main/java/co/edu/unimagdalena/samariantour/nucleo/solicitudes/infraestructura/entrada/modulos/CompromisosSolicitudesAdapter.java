package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.CompromisosPrestador;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CompromisosConTuristas;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarCompromisosUseCase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Le dice al módulo de prestadores qué le debe el prestador a los turistas antes de suprimir sus datos (DN-45):
 * solicitudes pagadas cuyo servicio no ha terminado y pagos en curso.
 */
@Component
public class CompromisosSolicitudesAdapter implements CompromisosPrestador {

    private final ConsultarCompromisosUseCase compromisos;

    public CompromisosSolicitudesAdapter(ConsultarCompromisosUseCase compromisos) {
        this.compromisos = compromisos;
    }

    @Override
    public List<String> pendientes(UUID prestadorId) {
        CompromisosConTuristas pendientes = compromisos.delPrestador(prestadorId);
        List<String> frases = new ArrayList<>();
        if (pendientes.pagadasPorPrestar() > 0) {
            frases.add(pendientes.pagadasPorPrestar() + " solicitudes pagadas cuyo servicio no ha terminado");
        }
        if (pendientes.pagosEnCurso() > 0) {
            frases.add(pendientes.pagosEnCurso() + " solicitudes con un pago en curso");
        }
        return frases;
    }
}
