package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerfilPublicoTest {

    private static final String DESCRIPCION = "Guías locales de Minca";

    @Test
    @DisplayName("el perfil se asocia a un prestador")
    void prestadorObligatorio() {
        assertThatThrownBy(() -> new PerfilPublico(null, null, DESCRIPCION, List.of(), null, null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("la descripción es obligatoria")
    void descripcionObligatoria() {
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, "  ", null, null, null, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("descripcion");
    }

    @Test
    @DisplayName("un perfil solo con descripción es válido: los demás campos son opcionales")
    void camposOpcionales() {
        PerfilPublico perfil = new PerfilPublico(PRESTADOR, null, DESCRIPCION, null, "", " ", null);

        assertThat(perfil.descripcion()).isEqualTo(DESCRIPCION);
        assertThat(perfil.nombreComercial()).isNull();
        assertThat(perfil.horariosAtencion()).isNull();
        assertThat(perfil.sitioWeb()).isNull();
        assertThat(perfil.fotos()).isEmpty();
        assertThat(perfil.redesSociales()).isEmpty();
    }

    @Test
    @DisplayName("el nombre comercial es opcional; en blanco se trata como ausente")
    void nombreComercial() {
        assertThat(new PerfilPublico(PRESTADOR, " Aves de Minca ", DESCRIPCION, null, null, null, null)
                .nombreComercial()).isEqualTo("Aves de Minca");
        assertThat(new PerfilPublico(PRESTADOR, "  ", DESCRIPCION, null, null, null, null).nombreComercial())
                .isNull();
    }

    @Test
    @DisplayName("acepta URLs http y https en el sitio web, las redes sociales y las fotos")
    void urlsValidas() {
        PerfilPublico perfil = new PerfilPublico(PRESTADOR, null, DESCRIPCION,
                List.of("https://ejemplo.co/fotos/1.jpg"), "Lunes a sábado", "http://ejemplo.co",
                List.of("https://red-social.ejemplo.co/guias-minca"));

        assertThat(perfil.sitioWeb()).isEqualTo("http://ejemplo.co");
        assertThat(perfil.fotos()).containsExactly("https://ejemplo.co/fotos/1.jpg");
        assertThat(perfil.redesSociales()).containsExactly("https://red-social.ejemplo.co/guias-minca");
    }

    @ParameterizedTest(name = "\"{0}\" no es una URL http o https")
    @ValueSource(strings = {"ejemplo.co", "ftp://ejemplo.co/foto.jpg", "https://", "javascript:alert(1)", "http://e j.co"})
    @DisplayName("rechaza lo que no es una URL http o https")
    void urlsInvalidas(String valor) {
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, DESCRIPCION, null, null, valor, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("sitioWeb");
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, DESCRIPCION, List.of(valor), null, null, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("fotos");
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, DESCRIPCION, null, null, null, List.of(valor)))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("redesSociales");
    }

    @Test
    @DisplayName("las listas del perfil no se pueden modificar desde fuera")
    void listasInmutables() {
        List<String> fotos = new ArrayList<>(List.of("https://ejemplo.co/foto-1.jpg"));
        PerfilPublico perfil = new PerfilPublico(PRESTADOR, null, DESCRIPCION, fotos, null, null,
                List.of("https://ejemplo.co/red-social"));

        fotos.add("https://ejemplo.co/foto-2.jpg");

        assertThat(perfil.fotos()).containsExactly("https://ejemplo.co/foto-1.jpg");
        assertThatThrownBy(() -> perfil.redesSociales().add("https://ejemplo.co/otra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
