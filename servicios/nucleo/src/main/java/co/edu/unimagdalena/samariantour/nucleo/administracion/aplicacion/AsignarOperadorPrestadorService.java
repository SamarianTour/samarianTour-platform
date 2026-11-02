package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorNoValidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.PrestadorRegistrado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.AsignarOperadorPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.CuentasInternasPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.OperadoresPrestadoresPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Asigna el operador de un prestador (RF-32, RF-81, DN-39): las cuentas dicen si el operador es válido y los
 * prestadores guardan la asignación.
 */
public class AsignarOperadorPrestadorService implements AsignarOperadorPrestadorUseCase {

    private final CuentasInternasPort cuentas;
    private final OperadoresPrestadoresPort prestadores;

    public AsignarOperadorPrestadorService(CuentasInternasPort cuentas, OperadoresPrestadoresPort prestadores) {
        this.cuentas = cuentas;
        this.prestadores = prestadores;
    }

    @Override
    @Transactional
    public PrestadorRegistrado asignar(UUID prestadorId, UUID operadorId) {
        if (!cuentas.esOperadorActivo(operadorId)) {
            throw new OperadorNoValidoException(operadorId);
        }
        return prestadores.asignarOperador(prestadorId, operadorId)
                .orElseThrow(() -> new PrestadorNoRegistradoException(prestadorId));
    }

    @Override
    public List<PrestadorRegistrado> prestadores(boolean soloSinOperador) {
        return prestadores.prestadores(soloSinOperador);
    }
}
