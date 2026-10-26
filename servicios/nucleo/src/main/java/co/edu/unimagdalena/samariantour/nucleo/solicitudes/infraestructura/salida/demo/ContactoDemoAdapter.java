package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ContactoTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ContactoPort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * PROVISIONAL [SUPUESTO] (DN-15): el nombre y el WhatsApp verificado de cada turista, de la tabla de la demo. Se
 * reemplaza por un adaptador que llame a la API del módulo de cuentas cuando exista, sin tocar el dominio.
 */
@Component
public class ContactoDemoAdapter implements ContactoPort {

    private final PropiedadesDemo demo;

    public ContactoDemoAdapter(PropiedadesDemo demo) {
        this.demo = demo;
    }

    @Override
    public Optional<ContactoTurista> turista(UUID turistaId) {
        return demo.turistas().stream()
                .filter(turista -> Objects.equals(turista.id(), turistaId))
                .filter(turista -> turista.whatsapp() != null && !turista.whatsapp().isBlank())
                .findFirst()
                .map(turista -> new ContactoTurista(turista.nombre(), turista.whatsapp()));
    }
}
