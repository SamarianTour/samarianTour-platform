package co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoEstado;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.Tablero;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.SolicitudesPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los indicadores con un doble de las solicitudes: el periodo que se les pide (RF-85 a RF-89, DN-41). */
class IndicadoresServiceTest {

    /** Lunes 2 de noviembre de 2026 a las 23:30 en Colombia; en UTC ya es martes. */
    private static final Instant AHORA = Instant.parse("2026-11-03T04:30:00Z");

    /** Los periodos pedidos: inicio y fin. */
    private final List<String> pedidos = new ArrayList<>();

    private final SolicitudesPort solicitudes = new SolicitudesPort() {
        @Override
        public List<ConteoDemanda> porZonaYTipo(Instant desde, Instant hasta) {
            pedidos.add(desde + " " + hasta);
            return List.of(new ConteoDemanda("tayrona", "ACTIVIDAD", 4));
        }

        @Override
        public List<ConteoEstado> porEstadoAlcanzado(Instant desde, Instant hasta) {
            pedidos.add(desde + " " + hasta);
            return List.of();
        }

        @Override
        public List<ConteoCategoria> porCategoria(Instant desde, Instant hasta) {
            pedidos.add(desde + " " + hasta);
            return List.of();
        }
    };

    private final IndicadoresService indicadores = new IndicadoresService(solicitudes,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    @DisplayName("sin fechas pide los últimos 30 días hasta hoy en Colombia, no en UTC")
    void periodoPorDefecto() {
        Tablero tablero = indicadores.consultarTablero(null, null);

        assertThat(tablero.periodo().hasta()).isEqualTo(LocalDate.parse("2026-11-02"));
        assertThat(tablero.periodo().desde()).isEqualTo(LocalDate.parse("2026-10-04"));
        assertThat(pedidos).containsExactly("2026-10-04T05:00:00Z 2026-11-03T05:00:00Z");
        assertThat(tablero.total()).isEqualTo(4);
    }

    @Test
    @DisplayName("el embudo y las categorías piden el mismo periodo; uno al revés no consulta nada")
    void mismoPeriodo() {
        LocalDate desde = LocalDate.parse("2026-10-01");
        LocalDate hasta = LocalDate.parse("2026-10-31");
        indicadores.consultarEmbudo(desde, hasta);
        indicadores.consultarDistribucion(desde, hasta);

        assertThat(pedidos).containsOnly("2026-10-01T05:00:00Z 2026-11-01T05:00:00Z").hasSize(2);
        assertThatThrownBy(() -> indicadores.consultarEmbudo(hasta, desde)).isInstanceOf(DatoInvalidoException.class);
        assertThat(pedidos).hasSize(2);
    }
}
