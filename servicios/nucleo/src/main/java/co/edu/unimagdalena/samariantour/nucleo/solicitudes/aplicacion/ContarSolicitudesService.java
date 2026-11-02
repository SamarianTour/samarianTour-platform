package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoEstado;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ContarSolicitudesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ConteosSolicitudesPort;

import java.time.Instant;
import java.util.List;

/** Los conteos para los indicadores (RF-85 a RF-89); los calcula la base, sin cargar solicitudes. */
public class ContarSolicitudesService implements ContarSolicitudesUseCase {

    private final ConteosSolicitudesPort conteos;

    public ContarSolicitudesService(ConteosSolicitudesPort conteos) {
        this.conteos = conteos;
    }

    @Override
    public List<ConteoDemanda> porZonaYTipo(Instant desde, Instant hasta) {
        return conteos.porZonaYTipo(desde, hasta);
    }

    @Override
    public List<ConteoEstado> porEstadoAlcanzado(Instant desde, Instant hasta) {
        return conteos.porEstadoAlcanzado(desde, hasta);
    }

    @Override
    public List<ConteoCategoria> porCategoria(Instant desde, Instant hasta) {
        return conteos.porCategoria(desde, hasta);
    }
}
