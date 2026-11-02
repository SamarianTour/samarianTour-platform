package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

import java.time.Instant;
import java.util.List;

/**
 * Lo que los indicadores usan de las solicitudes (RF-85 a RF-89, DN-41): conteos de las enviadas desde {@code desde}
 * y antes de {@code hasta}, nunca solicitudes (RET-03). Java puro, como {@link SolicitudesPagosApi}.
 */
public interface SolicitudesIndicadoresApi {

    List<DemandaContada> porZonaYTipo(Instant desde, Instant hasta);

    /** Cada solicitud cuenta una vez en cada estado por el que pasó, según su historial. */
    List<EstadoContado> porEstadoAlcanzado(Instant desde, Instant hasta);

    List<CategoriaContada> porCategoria(Instant desde, Instant hasta);
}
