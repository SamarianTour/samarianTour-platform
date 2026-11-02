package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El periodo de los indicadores, en hora de Colombia (DN-41). */
class PeriodoTest {

    private static final LocalDate HOY = LocalDate.parse("2026-11-02");

    @Test
    @DisplayName("sin fechas son los últimos 30 días hasta hoy; con una sola, se completa la otra")
    void porDefecto() {
        assertThat(Periodo.de(null, null, HOY)).isEqualTo(new Periodo(LocalDate.parse("2026-10-04"), HOY));
        assertThat(Periodo.de(null, LocalDate.parse("2026-10-31"), HOY).desde()).isEqualTo("2026-10-02");
        assertThat(Periodo.de(LocalDate.parse("2026-10-01"), null, HOY).hasta()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("va de la medianoche del primer día a la del día siguiente al último, en Colombia (UTC-5)")
    void instantes() {
        Periodo octubre = new Periodo(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

        assertThat(octubre.inicio()).isEqualTo(Instant.parse("2026-10-01T05:00:00Z"));
        assertThat(octubre.fin()).isEqualTo(Instant.parse("2026-11-01T05:00:00Z"));
    }

    @Test
    @DisplayName("al revés o de más de 366 días es un dato inválido")
    void invalido() {
        assertThatThrownBy(() -> new Periodo(HOY, HOY.minusDays(1)))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("desde: ");
        assertThatThrownBy(() -> new Periodo(HOY.minusDays(366), HOY))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("hasta: ");
        assertThat(new Periodo(HOY.minusDays(365), HOY).desde()).isEqualTo(HOY.minusDays(365));
    }
}
