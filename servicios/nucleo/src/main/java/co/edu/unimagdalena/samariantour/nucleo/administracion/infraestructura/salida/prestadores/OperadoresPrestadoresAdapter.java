package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.PrestadorRegistrado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.OperadoresPrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadorAdministrado;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadoresApiInterna;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** El operador de cada prestador, por la API de los prestadores (DN-35, DN-39). */
@Component
public class OperadoresPrestadoresAdapter implements OperadoresPrestadoresPort {

    private final PrestadoresApiInterna prestadores;

    public OperadoresPrestadoresAdapter(PrestadoresApiInterna prestadores) {
        this.prestadores = prestadores;
    }

    @Override
    public List<PrestadorRegistrado> prestadores(boolean soloSinOperador) {
        return prestadores.prestadores(soloSinOperador).stream().map(OperadoresPrestadoresAdapter::aDominio).toList();
    }

    @Override
    public Optional<PrestadorRegistrado> asignarOperador(UUID prestadorId, UUID operadorId) {
        return prestadores.asignarOperador(prestadorId, operadorId).map(OperadoresPrestadoresAdapter::aDominio);
    }

    @Override
    public long asignadosA(UUID operadorId) {
        return prestadores.prestadoresAsignados(operadorId);
    }

    private static PrestadorRegistrado aDominio(PrestadorAdministrado prestador) {
        return new PrestadorRegistrado(prestador.id(), prestador.numeroRnt(), prestador.razonSocial(),
                prestador.tipo(), prestador.estadoRnt(), prestador.operadorId(), prestador.registradoEn());
    }
}
