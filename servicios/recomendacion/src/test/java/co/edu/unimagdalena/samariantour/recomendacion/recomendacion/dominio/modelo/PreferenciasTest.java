package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.preferencias;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreferenciasTest {

    @Test
    @DisplayName("sin texto ni ninguna preferencia es 400 DATO_INVALIDO")
    void exigeAlgo() {
        assertThatThrownBy(() -> preferencias().texto("   ").crear())
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageStartingWith("preferencias:")
                .extracting(e -> ((DatoInvalidoException) e).codigo()).isEqualTo("DATO_INVALIDO");
    }

    @Test
    @DisplayName("basta el texto, o un solo filtro; pedir accesibilidad cuenta como preferencia")
    void bastaUnaPreferencia() {
        assertThat(preferencias().texto("algo con el mar").crear().tieneTexto()).isTrue();
        assertThat(preferencias().accesible().crear().accesibleSillaRuedas()).isTrue();
        assertThat(preferencias().precioMaximo(50_000).crear().precioMaximo()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("el texto pasa de 500 caracteres: 400; con espacios en los extremos se recortan antes de contar")
    void textoMaximo() {
        assertThat(preferencias().texto("  " + "a".repeat(500) + "  ").crear().texto()).hasSize(500);
        assertThatThrownBy(() -> preferencias().texto("a".repeat(501)).crear())
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("texto:");
    }

    @Test
    @DisplayName("el precio máximo debe ser mayor que cero")
    void precioPositivo() {
        assertThatThrownBy(() -> preferencias().precioMaximo(0).crear())
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("precioMaximo:");
    }

    @Test
    @DisplayName("sin idioma es español; acepta es y en sin importar mayúsculas y rechaza otros")
    void idioma() {
        Preferencias sinIdioma = new Preferencias("playa", null, null, null, " ", false, null, null);
        assertThat(sinIdioma.idioma()).isEqualTo(Idioma.ES);
        assertThat(sinIdioma.tiposOferta()).isEmpty();
        assertThat(sinIdioma.zonaId()).isNull();
        assertThat(Idioma.deCodigo(null)).isEqualTo(Idioma.ES);
        assertThat(Idioma.deCodigo(" EN ")).isEqualTo(Idioma.EN);
        assertThatThrownBy(() -> Idioma.deCodigo("fr")).isInstanceOf(DatoInvalidoException.class)
                .hasMessageStartingWith("idioma:");
    }

    @Test
    @DisplayName("toString no incluye el texto del turista, para que nunca termine en un log")
    void toStringSinTexto() {
        Preferencias conTexto = new Preferencias("viajo con mi hija Ana", Idioma.ES, Set.of(TipoOferta.ACTIVIDAD),
                Set.of(), null, false, null, new BigDecimal("90000"));
        assertThat(conTexto.toString()).doesNotContain("Ana").contains("texto=(omitido)", "ACTIVIDAD");
    }
}
