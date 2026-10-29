package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.modulos;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentasApiInterna;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ContactoTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ContactoPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * El contacto del turista, de su cuenta (RF-03, RF-30, RF-44, RF-57, DN-22): solo si está activa y verificó su
 * WhatsApp. Reemplaza la tabla provisional de la demo (DN-15); el dominio de las solicitudes no cambió.
 */
@Component
public class CuentasModuloAdapter implements ContactoPort {

    private final CuentasApiInterna cuentas;

    public CuentasModuloAdapter(CuentasApiInterna cuentas) {
        this.cuentas = cuentas;
    }

    @Override
    public Optional<ContactoTurista> turista(UUID turistaId) {
        return cuentas.contactoTurista(turistaId)
                .map(contacto -> new ContactoTurista(contacto.nombre(), contacto.whatsapp()));
    }
}
