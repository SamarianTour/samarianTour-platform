package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.CategoriaContada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.DemandaContada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.EstadoContado;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.SolicitudesIndicadoresApi;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ContarSolicitudesUseCase;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** La entrada de los indicadores a las solicitudes: traduce los conteos a los tipos del {@code api} (DN-41). */
@Component
public class SolicitudesIndicadoresApiAdapter implements SolicitudesIndicadoresApi {

    private final ContarSolicitudesUseCase conteos;

    public SolicitudesIndicadoresApiAdapter(ContarSolicitudesUseCase conteos) {
        this.conteos = conteos;
    }

    @Override
    public List<DemandaContada> porZonaYTipo(Instant desde, Instant hasta) {
        return conteos.porZonaYTipo(desde, hasta).stream()
                .map(conteo -> new DemandaContada(conteo.zonaId(), conteo.tipo().name(), conteo.solicitudes()))
                .toList();
    }

    @Override
    public List<EstadoContado> porEstadoAlcanzado(Instant desde, Instant hasta) {
        return conteos.porEstadoAlcanzado(desde, hasta).stream()
                .map(conteo -> new EstadoContado(conteo.tipo().name(), conteo.estado().name(), conteo.solicitudes()))
                .toList();
    }

    @Override
    public List<CategoriaContada> porCategoria(Instant desde, Instant hasta) {
        return conteos.porCategoria(desde, hasta).stream()
                .map(conteo -> new CategoriaContada(conteo.categoria() == null ? null : conteo.categoria().name(),
                        conteo.solicitudes(), conteo.pagadas()))
                .toList();
    }
}
