package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AparicionesPrestador;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.MedicionExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertasElegibles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El índice de exposición de cada prestador (RET-02, DN-43). */
class IndiceExposicionTest {

    private static final UUID MAR_Y_SELVA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID KAYAK = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID GUIA = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    @DisplayName("el 30 % de las apariciones con el 10 % de las ofertas da 3,0 y supera el umbral; el justo da 1,0")
    void medir() {
        List<MedicionExposicion> mediciones = IndiceExposicion.medir(
                List.of(new AparicionesPrestador(MAR_Y_SELVA, 30), new AparicionesPrestador(KAYAK, 70)),
                List.of(new OfertasElegibles(MAR_Y_SELVA, 10), new OfertasElegibles(KAYAK, 70),
                        new OfertasElegibles(GUIA, 20)));

        assertThat(mediciones).containsExactly(
                new MedicionExposicion(MAR_Y_SELVA, 30, new BigDecimal("30.00"), new BigDecimal("10.00"),
                        new BigDecimal("3.00")),
                new MedicionExposicion(KAYAK, 70, new BigDecimal("70.00"), new BigDecimal("70.00"),
                        new BigDecimal("1.00")),
                new MedicionExposicion(GUIA, 0, new BigDecimal("0.00"), new BigDecimal("20.00"),
                        new BigDecimal("0.00")));
        assertThat(mediciones).filteredOn(IndiceExposicion::superaUmbral).extracting(MedicionExposicion::prestadorId)
                .containsExactly(MAR_Y_SELVA);
    }

    @Test
    @DisplayName("1,5 justo no supera el umbral; un prestador que ya no tiene ofertas visibles no se mide")
    void bordes() {
        List<MedicionExposicion> mediciones = IndiceExposicion.medir(
                List.of(new AparicionesPrestador(MAR_Y_SELVA, 15), new AparicionesPrestador(KAYAK, 85),
                        new AparicionesPrestador(GUIA, 5)),
                List.of(new OfertasElegibles(MAR_Y_SELVA, 10), new OfertasElegibles(KAYAK, 90)));

        assertThat(mediciones).extracting(MedicionExposicion::prestadorId).containsExactly(MAR_Y_SELVA, KAYAK);
        assertThat(IndiceExposicion.medir(List.of(new AparicionesPrestador(MAR_Y_SELVA, 15)),
                List.of(new OfertasElegibles(MAR_Y_SELVA, 10), new OfertasElegibles(KAYAK, 90))).getFirst().indice())
                .isEqualByComparingTo("10.00");
        assertThat(IndiceExposicion.superaUmbral(new MedicionExposicion(KAYAK, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1.50")))).isFalse();
    }

    @Test
    @DisplayName("se mide desde 200 recomendaciones; sin apariciones u ofertas no hay mediciones")
    void datosSuficientes() {
        assertThat(IndiceExposicion.hayDatosSuficientes(199)).isFalse();
        assertThat(IndiceExposicion.hayDatosSuficientes(200)).isTrue();
        assertThat(IndiceExposicion.medir(List.of(), List.of(new OfertasElegibles(KAYAK, 5)))).isEmpty();
        assertThat(IndiceExposicion.medir(List.of(new AparicionesPrestador(KAYAK, 5)), List.of())).isEmpty();
    }
}
