package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
    @DisplayName("la descripción es obligatoria y no tiene límite de palabras")
    void descripcionObligatoria() {
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, "  ", null, null, null, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("descripcion");
        String larga = "palabra ".repeat(200).strip();
        assertThat(new PerfilPublico(PRESTADOR, null, larga, null, null, null, null).descripcion()).isEqualTo(larga);
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
        assertThat(perfil.revision()).isEqualTo(PerfilPublico.REVISION_NUEVA);
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
    @DisplayName("acepta URLs http y https en el sitio web y las redes sociales")
    void urlsValidas() {
        PerfilPublico perfil = new PerfilPublico(PRESTADOR, null, DESCRIPCION, null, "Lunes a sábado",
                "http://ejemplo.co", List.of("https://red-social.ejemplo.co/guias-minca"));

        assertThat(perfil.sitioWeb()).isEqualTo("http://ejemplo.co");
        assertThat(perfil.redesSociales()).containsExactly("https://red-social.ejemplo.co/guias-minca");
    }

    @ParameterizedTest(name = "\"{0}\" no es una URL http o https")
    @ValueSource(strings = {"ejemplo.co", "ftp://ejemplo.co/foto.jpg", "https://", "javascript:alert(1)", "http://e j.co"})
    @DisplayName("rechaza lo que no es una URL http o https")
    void urlsInvalidas(String valor) {
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, DESCRIPCION, null, null, valor, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("sitioWeb");
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, DESCRIPCION, null, null, null, List.of(valor)))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("redesSociales");
    }

    @Test
    @DisplayName("las listas del perfil no se pueden modificar desde fuera")
    void listasInmutables() {
        List<Foto> fotos = new ArrayList<>(List.of(Foto.nueva()));
        PerfilPublico perfil = new PerfilPublico(PRESTADOR, null, DESCRIPCION, fotos, null, null,
                List.of("https://ejemplo.co/red-social"));

        fotos.add(Foto.nueva());

        assertThat(perfil.fotos()).hasSize(1);
        assertThatThrownBy(() -> perfil.redesSociales().add("https://ejemplo.co/otra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("agrega las fotos al final, hasta 6, y quita la que se le indica (DC-39)")
    void fotos() {
        PerfilPublico perfil = new PerfilPublico(PRESTADOR, null, DESCRIPCION, null, null, null, null);
        List<Foto> subidas = IntStream.range(0, PerfilPublico.MAXIMO_FOTOS).mapToObj(i -> Foto.nueva()).toList();
        for (Foto foto : subidas) {
            perfil = perfil.agregarFoto(foto);
        }

        assertThat(perfil.fotos()).containsExactlyElementsOf(subidas);
        PerfilPublico lleno = perfil;
        assertThatThrownBy(() -> lleno.agregarFoto(Foto.nueva()))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("6 fotos");

        PerfilPublico sinLaSegunda = perfil.quitarFoto(subidas.get(1));
        assertThat(sinLaSegunda.fotos()).hasSize(5).doesNotContain(subidas.get(1));
        assertThat(sinLaSegunda.tieneFoto(subidas.get(0))).isTrue();
        assertThat(sinLaSegunda.quitarFoto(subidas.get(1))).isEqualTo(sinLaSegunda);
    }

    @Test
    @DisplayName("una foto no puede estar dos veces y no se aceptan más de 6 al reconstruir el perfil")
    void fotosInvalidas() {
        Foto foto = Foto.nueva();
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, DESCRIPCION, List.of(foto, foto), null, null,
                null)).isInstanceOf(IllegalArgumentException.class);
        List<Foto> siete = IntStream.range(0, 7).mapToObj(i -> Foto.nueva()).toList();
        assertThatThrownBy(() -> new PerfilPublico(PRESTADOR, null, DESCRIPCION, siete, null, null, null))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    @DisplayName("cambiar los datos conserva las fotos y la revisión con que se leyó")
    void conDatos() {
        Foto foto = Foto.nueva();
        PerfilPublico leido = new PerfilPublico(PRESTADOR, "Antes", DESCRIPCION, List.of(foto), null, null, null, 4);

        PerfilPublico cambiado = leido.conDatos("Después", "Otra descripción", "Todos los días", null, List.of());

        assertThat(cambiado.nombreComercial()).isEqualTo("Después");
        assertThat(cambiado.descripcion()).isEqualTo("Otra descripción");
        assertThat(cambiado.fotos()).containsExactly(foto);
        assertThat(cambiado.revision()).isEqualTo(4);
    }
}
