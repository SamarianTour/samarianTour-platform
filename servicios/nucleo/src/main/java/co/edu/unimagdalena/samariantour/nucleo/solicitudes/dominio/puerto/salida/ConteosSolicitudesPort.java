package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoEstado;

import java.time.Instant;
import java.util.List;

/**
 * Los conteos de las solicitudes enviadas desde {@code desde} y antes de {@code hasta}, para los indicadores
 * (RF-85 a RF-89). Nunca salen solicitudes ni datos del turista (RET-03).
 */
public interface ConteosSolicitudesPort {

    List<ConteoDemanda> porZonaYTipo(Instant desde, Instant hasta);

    List<ConteoEstado> porEstadoAlcanzado(Instant desde, Instant hasta);

    List<ConteoCategoria> porCategoria(Instant desde, Instant hasta);
}
