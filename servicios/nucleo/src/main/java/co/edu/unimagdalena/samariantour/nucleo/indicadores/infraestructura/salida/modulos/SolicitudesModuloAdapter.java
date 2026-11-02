package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.salida.modulos;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoEstado;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.SolicitudesPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.SolicitudesIndicadoresApi;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Los conteos de las solicitudes, por su API, en memoria (diagrama de paquetes, DN-41). */
@Component
public class SolicitudesModuloAdapter implements SolicitudesPort {

    private final SolicitudesIndicadoresApi solicitudes;

    public SolicitudesModuloAdapter(SolicitudesIndicadoresApi solicitudes) {
        this.solicitudes = solicitudes;
    }

    @Override
    public List<ConteoDemanda> porZonaYTipo(Instant desde, Instant hasta) {
        return solicitudes.porZonaYTipo(desde, hasta).stream()
                .map(conteo -> new ConteoDemanda(conteo.zonaId(), conteo.tipoOferta(), conteo.solicitudes()))
                .toList();
    }

    @Override
    public List<ConteoEstado> porEstadoAlcanzado(Instant desde, Instant hasta) {
        return solicitudes.porEstadoAlcanzado(desde, hasta).stream()
                .map(conteo -> new ConteoEstado(conteo.tipoOferta(), conteo.estado(), conteo.solicitudes()))
                .toList();
    }

    @Override
    public List<ConteoCategoria> porCategoria(Instant desde, Instant hasta) {
        return solicitudes.porCategoria(desde, hasta).stream()
                .map(conteo -> new ConteoCategoria(conteo.categoria(), conteo.solicitudes(), conteo.pagadas()))
                .toList();
    }
}
