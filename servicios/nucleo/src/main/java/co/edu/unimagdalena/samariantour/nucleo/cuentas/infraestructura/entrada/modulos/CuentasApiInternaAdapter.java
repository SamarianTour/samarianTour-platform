package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.ContactoTurista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentasApiInterna;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarContactoUseCase;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * La entrada de los demás módulos a las cuentas (DN-32), como un controlador: traduce a los casos de uso y entrega los
 * tipos del {@code api}, nunca los del dominio (regla 11 de {@code ArquitecturaTest}).
 */
@Component
public class CuentasApiInternaAdapter implements CuentasApiInterna {

    private final ConsultarContactoUseCase contactos;

    public CuentasApiInternaAdapter(ConsultarContactoUseCase contactos) {
        this.contactos = contactos;
    }

    @Override
    public Optional<ContactoTurista> contactoTurista(UUID turistaId) {
        return contactos.turistaContactable(turistaId)
                .map(turista -> new ContactoTurista(turista.nombre(), turista.whatsapp()));
    }
}
