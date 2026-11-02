package co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.DistribucionCategoria;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.EmbudoConversion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.Periodo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.Tablero;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarDistribucionCategoriaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarEmbudoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarTableroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.SolicitudesPort;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Los indicadores del gestor del destino (RF-85 a RF-89), como el {@code IndicadoresService} del diagrama de paquetes.
 * Las solicitudes cuentan; aquí se arma cada indicador y se ocultan los conteos pequeños (RET-03).
 */
public class IndicadoresService implements ConsultarTableroUseCase, ConsultarEmbudoUseCase,
        ConsultarDistribucionCategoriaUseCase {

    private final SolicitudesPort solicitudes;
    private final Clock reloj;

    public IndicadoresService(SolicitudesPort solicitudes, Clock reloj) {
        this.solicitudes = solicitudes;
        this.reloj = reloj;
    }

    @Override
    public Tablero consultarTablero(LocalDate desde, LocalDate hasta) {
        Periodo periodo = periodo(desde, hasta);
        return Tablero.de(periodo, solicitudes.porZonaYTipo(periodo.inicio(), periodo.fin()));
    }

    @Override
    public EmbudoConversion consultarEmbudo(LocalDate desde, LocalDate hasta) {
        Periodo periodo = periodo(desde, hasta);
        return EmbudoConversion.de(periodo, solicitudes.porEstadoAlcanzado(periodo.inicio(), periodo.fin()));
    }

    @Override
    public DistribucionCategoria consultarDistribucion(LocalDate desde, LocalDate hasta) {
        Periodo periodo = periodo(desde, hasta);
        return DistribucionCategoria.de(periodo, solicitudes.porCategoria(periodo.inicio(), periodo.fin()));
    }

    private Periodo periodo(LocalDate desde, LocalDate hasta) {
        return Periodo.de(desde, hasta, LocalDate.ofInstant(reloj.instant(), Periodo.ZONA));
    }
}
