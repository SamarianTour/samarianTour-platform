package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoEstado;

import java.time.Instant;
import java.util.List;

/** Los conteos que los indicadores piden a las solicitudes (RF-85 a RF-89), solo agregados (RET-03). */
public interface ContarSolicitudesUseCase {

    List<ConteoDemanda> porZonaYTipo(Instant desde, Instant hasta);

    List<ConteoEstado> porEstadoAlcanzado(Instant desde, Instant hasta);

    List<ConteoCategoria> porCategoria(Instant desde, Instant hasta);
}
