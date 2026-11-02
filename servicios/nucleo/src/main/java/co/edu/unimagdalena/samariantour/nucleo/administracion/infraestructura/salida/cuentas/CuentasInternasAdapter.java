package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.cuentas;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CorreoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaInternaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.CuentaInterna;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.CuentasInternasPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentaInternaRechazadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentaInternaRegistrada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentasAdministracionApi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Las cuentas internas, por la API de las cuentas (DN-40). Traduce sus errores a los de la administración. */
@Component
public class CuentasInternasAdapter implements CuentasInternasPort {

    private final CuentasAdministracionApi cuentas;

    public CuentasInternasAdapter(CuentasAdministracionApi cuentas) {
        this.cuentas = cuentas;
    }

    @Override
    public List<CuentaInterna> internas() {
        return cuentas.cuentasInternas().stream().map(CuentasInternasAdapter::aDominio).toList();
    }

    @Override
    public Optional<CuentaInterna> porId(UUID usuarioId) {
        return cuentas.cuentaInterna(usuarioId).map(CuentasInternasAdapter::aDominio);
    }

    @Override
    public CuentaInterna crear(String rol, String nombre, String correo) {
        return traducir(null, () -> cuentas.crearCuentaInterna(rol, nombre, correo));
    }

    @Override
    public CuentaInterna desactivar(UUID usuarioId) {
        return traducir(usuarioId, () -> cuentas.desactivar(usuarioId));
    }

    @Override
    public CuentaInterna reactivar(UUID usuarioId) {
        return traducir(usuarioId, () -> cuentas.reactivar(usuarioId));
    }

    @Override
    public boolean esOperadorActivo(UUID usuarioId) {
        return cuentas.esOperadorActivo(usuarioId);
    }

    private static CuentaInterna traducir(UUID usuarioId, Supplier<CuentaInternaRegistrada> operacion) {
        try {
            return aDominio(operacion.get());
        } catch (CuentaInternaRechazadaException e) {
            throw switch (e.codigo()) {
                case "CORREO_YA_REGISTRADO" -> new CorreoYaRegistradoException(e.getMessage());
                case "CORREO_NO_DISPONIBLE" -> new CorreoNoDisponibleException(e.getMessage());
                case "CUENTA_NO_ENCONTRADA" -> new CuentaInternaNoEncontradaException(usuarioId);
                default -> datoInvalido(e.getMessage());
            };
        }
    }

    /** Las cuentas escriben el campo al principio del mensaje: "correo: no es un correo válido". */
    private static DatoInvalidoException datoInvalido(String mensaje) {
        int separador = mensaje.indexOf(": ");
        return separador < 0 ? new DatoInvalidoException("cuenta", mensaje)
                : new DatoInvalidoException(mensaje.substring(0, separador), mensaje.substring(separador + 2));
    }

    private static CuentaInterna aDominio(CuentaInternaRegistrada cuenta) {
        return new CuentaInterna(cuenta.id(), cuenta.rol(), cuenta.nombre(), cuenta.correo(), cuenta.estado(),
                cuenta.contrasenaEstablecida(), cuenta.registradaEn());
    }
}
