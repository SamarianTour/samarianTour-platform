package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaAnticipacionTest {

    @Test
    @DisplayName("al menos 2 días calendario después del envío: enviada el sábado, desde el lunes (RF-30)")
    void dosDiasCalendario() {
        Instant sabado = Instant.parse("2026-10-24T15:00:00Z");

        assertThat(PoliticaAnticipacion.primeraFechaPermitida(sabado)).isEqualTo(LocalDate.parse("2026-10-26"));
        assertThat(PoliticaAnticipacion.permite(LocalDate.parse("2026-10-25"), sabado)).isFalse();
        assertThat(PoliticaAnticipacion.permite(LocalDate.parse("2026-10-26"), sabado)).isTrue();
        assertThat(PoliticaAnticipacion.permite(LocalDate.parse("2026-12-24"), sabado)).isTrue();
    }

    @Test
    @DisplayName("cuenta los días en la hora de Colombia: el domingo 03:00 UTC todavía es sábado en Bogotá")
    void horaDeColombia() {
        Instant sabadoEnBogota = Instant.parse("2026-10-25T03:00:00Z");

        assertThat(PoliticaAnticipacion.primeraFechaPermitida(sabadoEnBogota)).isEqualTo(LocalDate.parse("2026-10-26"));
    }

    @Test
    @DisplayName("son días de calendario, no 48 h: enviada a las 23:59, el día permitido empieza 24 h y 1 min después")
    void noSonHoras() {
        Instant tarde = LocalDate.parse("2026-10-24").atTime(23, 59).atZone(PoliticaVentanas.ZONA).toInstant();

        assertThat(PoliticaAnticipacion.permite(LocalDate.parse("2026-10-26"), tarde)).isTrue();
    }
}
