package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * PROVISIONAL [SUPUESTO] (DN-15): el operador y el WhatsApp de cada prestador, de la tabla de la demo. Se reemplaza por
 * un adaptador que llame a la API del módulo de prestadores cuando exista, sin tocar el dominio.
 */
@Component
public class PrestadoresDemoAdapter implements PrestadoresPort {

    private final PropiedadesDemo demo;

    public PrestadoresDemoAdapter(PropiedadesDemo demo) {
        this.demo = demo;
    }

    @Override
    public Optional<UUID> operadorAsignado(UUID prestadorId) {
        return prestador(prestadorId).map(PropiedadesDemo.Prestador::operadorId);
    }

    @Override
    public Optional<String> whatsapp(UUID prestadorId) {
        return prestador(prestadorId).map(PropiedadesDemo.Prestador::whatsapp);
    }

    private Optional<PropiedadesDemo.Prestador> prestador(UUID prestadorId) {
        return demo.prestadores().stream().filter(prestador -> Objects.equals(prestador.id(), prestadorId)).findFirst();
    }
}
