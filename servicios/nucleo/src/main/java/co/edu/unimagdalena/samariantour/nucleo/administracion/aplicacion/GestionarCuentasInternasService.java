package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaInternaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaPropiaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorConPrestadoresException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.CuentaInterna;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.GestionarCuentasInternasUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.CuentasInternasPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.OperadoresPrestadoresPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Las cuentas internas (RF-81, DN-40). Antes de desactivar revisa lo que las cuentas no saben: que no sea la del
 * propio administrador y que un operador no tenga prestadores asignados.
 */
public class GestionarCuentasInternasService implements GestionarCuentasInternasUseCase {

    private final CuentasInternasPort cuentas;
    private final OperadoresPrestadoresPort prestadores;

    public GestionarCuentasInternasService(CuentasInternasPort cuentas, OperadoresPrestadoresPort prestadores) {
        this.cuentas = cuentas;
        this.prestadores = prestadores;
    }

    @Override
    @Transactional
    public CuentaInterna crear(CrearCuentaInternaCommand comando) {
        return cuentas.crear(comando.rol(), comando.nombre(), comando.correo());
    }

    @Override
    public List<CuentaInterna> consultar() {
        return cuentas.internas();
    }

    @Override
    @Transactional
    public CuentaInterna desactivar(UUID usuarioId, UUID administradorId) {
        if (usuarioId.equals(administradorId)) {
            throw new CuentaPropiaException();
        }
        CuentaInterna cuenta = cuentas.porId(usuarioId)
                .orElseThrow(() -> new CuentaInternaNoEncontradaException(usuarioId));
        if (cuenta.esOperador()) {
            long asignados = prestadores.asignadosA(usuarioId);
            if (asignados > 0) {
                throw new OperadorConPrestadoresException(usuarioId, asignados);
            }
        }
        return cuentas.desactivar(usuarioId);
    }

    @Override
    @Transactional
    public CuentaInterna reactivar(UUID usuarioId) {
        return cuentas.reactivar(usuarioId);
    }
}
