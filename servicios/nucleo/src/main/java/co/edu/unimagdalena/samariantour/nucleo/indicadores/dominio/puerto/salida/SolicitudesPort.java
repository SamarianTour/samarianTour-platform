package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoEstado;

import java.time.Instant;
import java.util.List;

/** Los conteos de las solicitudes enviadas desde {@code desde} y antes de {@code hasta}, que da su módulo (DN-41). */
public interface SolicitudesPort {

    List<ConteoDemanda> porZonaYTipo(Instant desde, Instant hasta);

    List<ConteoEstado> porEstadoAlcanzado(Instant desde, Instant hasta);

    List<ConteoCategoria> porCategoria(Instant desde, Instant hasta);
}
