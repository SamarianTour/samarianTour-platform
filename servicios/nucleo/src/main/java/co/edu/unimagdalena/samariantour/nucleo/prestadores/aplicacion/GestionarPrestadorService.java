package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.DatosTipo;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ActualizarTipoYEmpleadosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConsultarEstadoRntUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** El tipo y sus campos condicionales (RF-68, RF-69) y la consulta del estado RNT (RF-73). */
public class GestionarPrestadorService implements ActualizarTipoYEmpleadosUseCase, ConsultarEstadoRntUseCase {

    private final PrestadorRepositoryPort prestadores;

    public GestionarPrestadorService(PrestadorRepositoryPort prestadores) {
        this.prestadores = prestadores;
    }

    @Override
    @Transactional
    public Prestador actualizar(UUID prestadorId, DatosTipo datosTipo) {
        Prestador prestador = consultar(prestadorId);
        prestador.cambiarTipo(datosTipo);
        prestadores.actualizar(prestador);
        return prestador;
    }

    @Override
    public Prestador consultar(UUID prestadorId) {
        return prestadores.porId(prestadorId).orElseThrow(() -> new PrestadorNoRegistradoException(prestadorId));
    }
}
