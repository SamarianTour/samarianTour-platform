package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.modulos;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadorConsultado;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadoresApiInterna;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * El operador asignado y el WhatsApp del prestador, de su módulo (RF-32, RF-57, DN-35). Reemplaza la tabla provisional
 * de la demo (DN-15); el dominio de las solicitudes no cambió.
 */
@Component
public class PrestadoresSolicitudesAdapter implements PrestadoresPort {

    private final PrestadoresApiInterna prestadores;

    public PrestadoresSolicitudesAdapter(PrestadoresApiInterna prestadores) {
        this.prestadores = prestadores;
    }

    @Override
    public Optional<UUID> operadorAsignado(UUID prestadorId) {
        return prestadores.consultar(prestadorId).map(PrestadorConsultado::operadorId);
    }

    @Override
    public Optional<String> whatsapp(UUID prestadorId) {
        return prestadores.consultar(prestadorId).map(PrestadorConsultado::whatsapp);
    }

    @Override
    public boolean estaHabilitado(UUID prestadorId) {
        return prestadores.consultar(prestadorId).map(PrestadorConsultado::habilitado).orElse(false);
    }
}
