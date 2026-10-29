package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica.ReglaCategoria;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica.ReglaCoherenciaTipoRnt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El tipo y sus campos condicionales (RF-68, RF-69), la categoría y la coherencia con el RNT (DN-33). */
class DatosTipoTest {

    @Test
    @DisplayName("el guía declara modalidades e idiomas y no empleados; los demás, empleados y nada más")
    void camposCondicionales() {
        assertThatThrownBy(() -> new DatosTipo(TipoPrestador.GUIA_INDEPENDIENTE, 3, List.of(ModalidadGuianza.KAYAK),
                List.of("es"))).hasMessageStartingWith("numeroEmpleados:");
        assertThatThrownBy(() -> new DatosTipo(TipoPrestador.GUIA_INDEPENDIENTE, null, List.of(), List.of("es")))
                .hasMessageStartingWith("modalidades:");
        assertThatThrownBy(() -> new DatosTipo(TipoPrestador.GUIA_INDEPENDIENTE, null,
                List.of(ModalidadGuianza.KAYAK), List.of("ES"))).hasMessageStartingWith("idiomas:");
        assertThatThrownBy(() -> new DatosTipo(TipoPrestador.RESTAURANTE, null, List.of(), List.of()))
                .hasMessageStartingWith("numeroEmpleados:");
        assertThatThrownBy(() -> new DatosTipo(TipoPrestador.ALOJAMIENTO, 5, List.of(ModalidadGuianza.OTRA),
                List.of())).isInstanceOf(DatoInvalidoException.class);
        assertThat(new DatosTipo(TipoPrestador.GUIA_INDEPENDIENTE, null,
                List.of(ModalidadGuianza.KAYAK, ModalidadGuianza.KAYAK), List.of("es", "es")).modalidades())
                .containsExactly(ModalidadGuianza.KAYAK);
    }

    @Test
    @DisplayName("micro y pequeño hasta 50 empleados, mediano hasta 200, grande desde 201")
    void categoria() {
        assertThat(ReglaCategoria.de(TipoPrestador.RESTAURANTE, 50)).isEqualTo(CategoriaPrestador.MICRO_PEQUENO);
        assertThat(ReglaCategoria.de(TipoPrestador.RESTAURANTE, 51)).isEqualTo(CategoriaPrestador.MEDIANO);
        assertThat(ReglaCategoria.de(TipoPrestador.ALOJAMIENTO, 200)).isEqualTo(CategoriaPrestador.MEDIANO);
        assertThat(ReglaCategoria.de(TipoPrestador.ALOJAMIENTO, 201)).isEqualTo(CategoriaPrestador.GRANDE);
    }

    @Test
    @DisplayName("las categorías del RNT se comparan sin tildes ni mayúsculas")
    void coherencia() {
        assertThat(ReglaCoherenciaTipoRnt.coincide(TipoPrestador.ALOJAMIENTO,
                "Establecimientos de alojamiento turístico", false)).isTrue();
        assertThat(ReglaCoherenciaTipoRnt.coincide(TipoPrestador.RESTAURANTE, "ESTABLECIMIENTOS DE GASTRONOMÍA",
                false)).isTrue();
        assertThat(ReglaCoherenciaTipoRnt.coincide(TipoPrestador.RESTAURANTE, "BARES", false)).isFalse();
        assertThat(ReglaCoherenciaTipoRnt.coincide(TipoPrestador.GUIA_INDEPENDIENTE, "GUIAS DE TURISMO", false))
                .isFalse();
    }
}
