package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.modulos;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentasApiInterna;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.ContrasenaRechazadaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentasPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Las cuentas, por su API (DN-32). */
@Component
public class CuentasPrestadoresAdapter implements CuentasPort {

    private final CuentasApiInterna cuentas;

    public CuentasPrestadoresAdapter(CuentasApiInterna cuentas) {
        this.cuentas = cuentas;
    }

    @Override
    public boolean esPrestadorActivo(UUID usuarioId) {
        return cuentas.esPrestadorActivo(usuarioId);
    }

    @Override
    public void verificarContrasena(UUID prestadorId, String contrasena) {
        try {
            cuentas.verificarContrasenaPrestador(prestadorId, contrasena);
        } catch (co.edu.unimagdalena.samariantour.nucleo.cuentas.api.ContrasenaRechazadaException e) {
            throw new ContrasenaRechazadaException(e.codigo(), e.getMessage(), e.bloqueadaHasta());
        }
    }

    @Override
    public void suprimirCuenta(UUID prestadorId) {
        cuentas.suprimirCuentaPrestador(prestadorId);
    }
}
