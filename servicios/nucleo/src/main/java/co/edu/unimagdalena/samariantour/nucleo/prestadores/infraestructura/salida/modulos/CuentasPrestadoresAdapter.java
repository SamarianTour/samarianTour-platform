package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.modulos;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentasApiInterna;
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
}
