package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El tablero, el embudo y la distribución por categoría, con la regla de anonimización (RF-85 a RF-89, RET-03). */
class IndicadoresTest {

    private static final Periodo OCTUBRE = new Periodo(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

    @Nested
    @DisplayName("Tablero (RF-85, RF-86)")
    class DeTablero {

        @Test
        @DisplayName("primero las de más solicitudes; las de 1 o 2 van al final, por zona, sin su conteo")
        void orden() {
            Tablero tablero = Tablero.de(OCTUBRE, List.of(
                    new ConteoDemanda("minca", "ALOJAMIENTO", 2),
                    new ConteoDemanda("tayrona", "ACTIVIDAD", 18),
                    new ConteoDemanda("centro", "RESTAURANTE", 1),
                    new ConteoDemanda("taganga", "ACTIVIDAD", 5)));

            assertThat(tablero.filas()).containsExactly(
                    new DemandaZona("tayrona", "ACTIVIDAD", 18L),
                    new DemandaZona("taganga", "ACTIVIDAD", 5L),
                    new DemandaZona("centro", "RESTAURANTE", null),
                    new DemandaZona("minca", "ALOJAMIENTO", null));
            assertThat(tablero.total()).isEqualTo(26);
        }

        @Test
        @DisplayName("con 1 o 2 solicitudes en todo el periodo, tampoco se publica el total")
        void totalPequeno() {
            assertThat(Tablero.de(OCTUBRE, List.of(new ConteoDemanda("minca", "ALOJAMIENTO", 2))).total()).isNull();
            assertThat(Tablero.de(OCTUBRE, List.of()).total()).isZero();
        }
    }

    @Nested
    @DisplayName("Embudo (RF-87, RF-88)")
    class DeEmbudo {

        @Test
        @DisplayName("las cuatro etapas sobre las enviadas, en total y por cada uno de los cuatro tipos")
        void etapas() {
            EmbudoConversion embudo = EmbudoConversion.de(OCTUBRE, List.of(
                    new ConteoEstado("ACTIVIDAD", "PENDIENTE", 38),
                    new ConteoEstado("ACTIVIDAD", "ATENDIDA", 27),
                    new ConteoEstado("ACTIVIDAD", "CONFIRMADA", 20),
                    new ConteoEstado("ACTIVIDAD", "PAGADA", 17),
                    new ConteoEstado("ACTIVIDAD", "VENCIDA", 3),
                    new ConteoEstado("RESTAURANTE", "PENDIENTE", 4),
                    new ConteoEstado("RESTAURANTE", "ATENDIDA", 3),
                    new ConteoEstado("RESTAURANTE", "CONFIRMADA", 1)));

            assertThat(embudo.etapas()).containsExactly(
                    new EtapaEmbudo(Etapa.ENVIADAS, 42L, new BigDecimal("100.0")),
                    new EtapaEmbudo(Etapa.ATENDIDAS, 30L, new BigDecimal("71.4")),
                    new EtapaEmbudo(Etapa.CONFIRMADAS, 21L, new BigDecimal("50.0")),
                    new EtapaEmbudo(Etapa.PAGADAS, 17L, new BigDecimal("40.5")));
            assertThat(embudo.porTipo()).extracting(EmbudoTipo::tipoOferta)
                    .containsExactly("ACTIVIDAD", "PAQUETE", "ALOJAMIENTO", "RESTAURANTE");
            assertThat(embudo.porTipo().get(3).etapas()).containsExactly(
                    new EtapaEmbudo(Etapa.ENVIADAS, 4L, new BigDecimal("100.0")),
                    new EtapaEmbudo(Etapa.ATENDIDAS, 3L, new BigDecimal("75.0")),
                    new EtapaEmbudo(Etapa.CONFIRMADAS, null, null),
                    new EtapaEmbudo(Etapa.PAGADAS, 0L, new BigDecimal("0.0")));
            assertThat(embudo.porTipo().get(1).etapas()).allSatisfy(etapa -> {
                assertThat(etapa.solicitudes()).isZero();
                assertThat(etapa.porcentaje()).isNull();
            });
        }
    }

    @Nested
    @DisplayName("Distribución por categoría (RF-89)")
    class DeCategoria {

        @Test
        @DisplayName("siempre las cuatro categorías, en orden; SIN_CATEGORIA solo si hay solicitudes sin ella")
        void categorias() {
            DistribucionCategoria distribucion = DistribucionCategoria.de(OCTUBRE, List.of(
                    new ConteoCategoria("MICRO_PEQUENO", 25, 10),
                    new ConteoCategoria("GUIA_INDEPENDIENTE", 6, 2),
                    new ConteoCategoria(null, 11, 6)));

            assertThat(distribucion.categorias()).containsExactly(
                    new ParticipacionCategoria("GUIA_INDEPENDIENTE", 6L, new BigDecimal("14.3"), null),
                    new ParticipacionCategoria("MICRO_PEQUENO", 25L, new BigDecimal("59.5"), 10L),
                    new ParticipacionCategoria("MEDIANO", 0L, new BigDecimal("0.0"), 0L),
                    new ParticipacionCategoria("GRANDE", 0L, new BigDecimal("0.0"), 0L),
                    new ParticipacionCategoria("SIN_CATEGORIA", 11L, new BigDecimal("26.2"), 6L));
            assertThat(distribucion.total()).isEqualTo(42);
            assertThat(DistribucionCategoria.de(OCTUBRE, List.of()).categorias()).hasSize(4);
        }
    }
}
