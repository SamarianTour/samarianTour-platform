package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.ContactoTurista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.ContrasenaRechazadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentasApiInterna;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ContrasenaIncorrectaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarContactoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SuprimirCuentaPrestadorUseCase;
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
    private final SuprimirCuentaPrestadorUseCase supresion;

    public CuentasApiInternaAdapter(ConsultarContactoUseCase contactos, SuprimirCuentaPrestadorUseCase supresion) {
        this.contactos = contactos;
        this.supresion = supresion;
    }

    @Override
    public Optional<ContactoTurista> contactoTurista(UUID turistaId) {
        return contactos.turistaContactable(turistaId)
                .map(turista -> new ContactoTurista(turista.nombre(), turista.whatsapp()));
    }

    @Override
    public boolean esPrestadorActivo(UUID usuarioId) {
        return contactos.esPrestadorActivo(usuarioId);
    }

    @Override
    public void verificarContrasenaPrestador(UUID prestadorId, String contrasena) {
        try {
            supresion.verificarContrasena(prestadorId, contrasena);
        } catch (CuentaBloqueadaException e) {
            throw new ContrasenaRechazadaException(e.codigo(), e.getMessage(), e.bloqueadaHasta());
        } catch (ContrasenaIncorrectaException | CuentaNoEncontradaException e) {
            throw new ContrasenaRechazadaException(e.codigo(), e.getMessage(), null);
        }
    }

    @Override
    public void suprimirCuentaPrestador(UUID prestadorId) {
        supresion.suprimir(prestadorId);
    }
}
