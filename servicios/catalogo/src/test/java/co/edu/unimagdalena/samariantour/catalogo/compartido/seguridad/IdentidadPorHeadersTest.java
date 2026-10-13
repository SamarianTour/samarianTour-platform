package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentidadPorHeadersTest {

    private static final String PRESTADOR = "20000000-0000-0000-0000-000000000002";

    private final IdentidadPorHeaders resolutor = new IdentidadPorHeaders();

    @Test
    @DisplayName("toma el prestador y su tipo de los headers que sustituyen al token")
    void identidadDeLosHeaders() {
        IdentidadPrestador identidad = resolutor.prestadorDe(peticion(PRESTADOR, " GUIA_INDEPENDIENTE "));

        assertThat(identidad).isEqualTo(new IdentidadPrestador(UUID.fromString(PRESTADOR), "GUIA_INDEPENDIENTE"));
    }

    @Test
    @DisplayName("sin alguno de los headers, o con un identificador mal formado, no hay identidad")
    void sinIdentidad() {
        assertThatThrownBy(() -> resolutor.prestadorDe(peticion(null, "GUIA_INDEPENDIENTE")))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("X-Prestador-Id");
        assertThatThrownBy(() -> resolutor.prestadorDe(peticion(PRESTADOR, null)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("X-Tipo-Prestador");
        assertThatThrownBy(() -> resolutor.prestadorDe(peticion("no-es-un-uuid", "GUIA_INDEPENDIENTE")))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    @DisplayName("sin el perfil demo ninguna petición tiene identidad, aunque traiga los headers")
    void sinPerfilDemo() {
        assertThatThrownBy(() -> new IdentidadNoDisponible().prestadorDe(peticion(PRESTADOR, "GUIA_INDEPENDIENTE")))
                .isInstanceOf(NoAutenticadoException.class);
    }

    private static MockHttpServletRequest peticion(String prestadorId, String tipoPrestador) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/prestador/ofertas");
        if (prestadorId != null) {
            peticion.addHeader(IdentidadPorHeaders.HEADER_PRESTADOR, prestadorId);
        }
        if (tipoPrestador != null) {
            peticion.addHeader(IdentidadPorHeaders.HEADER_TIPO, tipoPrestador);
        }
        return peticion;
    }
}
