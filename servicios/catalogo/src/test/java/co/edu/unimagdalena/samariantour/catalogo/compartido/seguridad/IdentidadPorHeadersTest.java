package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentidadPorHeadersTest {

    private static final String USUARIO = "20000000-0000-0000-0000-000000000002";

    private final IdentidadPorHeaders resolutor = new IdentidadPorHeaders();

    @Test
    @DisplayName("toma el usuario, el rol y el tipo de prestador de los headers que sustituyen al token")
    void prestador() {
        Identidad identidad = resolutor.identidadDe(peticion(USUARIO, "PRESTADOR", " GUIA_INDEPENDIENTE "));

        assertThat(identidad).isEqualTo(new Identidad(UUID.fromString(USUARIO), Rol.PRESTADOR, "GUIA_INDEPENDIENTE"));
    }

    @Test
    @DisplayName("el turista y el administrador no llevan tipo de prestador; si lo envían, se ignora")
    void otrosRoles() {
        assertThat(resolutor.identidadDe(peticion(USUARIO, "TURISTA", null)))
                .isEqualTo(new Identidad(UUID.fromString(USUARIO), Rol.TURISTA, null));
        assertThat(resolutor.identidadDe(peticion(USUARIO, "ADMINISTRADOR", "AGENCIA_OPERADORA")))
                .isEqualTo(new Identidad(UUID.fromString(USUARIO), Rol.ADMINISTRADOR, null));
    }

    @Test
    @DisplayName("sin el usuario o el rol, con un rol desconocido o con un id mal formado, no hay identidad")
    void sinIdentidad() {
        assertThatThrownBy(() -> resolutor.identidadDe(peticion(null, "TURISTA", null)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("X-Usuario-Id");
        assertThatThrownBy(() -> resolutor.identidadDe(peticion(USUARIO, null, null)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("X-Rol");
        assertThatThrownBy(() -> resolutor.identidadDe(peticion(USUARIO, "OPERADOR", null)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("X-Rol");
        assertThatThrownBy(() -> resolutor.identidadDe(peticion("no-es-un-uuid", "TURISTA", null)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("X-Usuario-Id");
    }

    @Test
    @DisplayName("el rol PRESTADOR exige el tipo de prestador")
    void prestadorSinTipo() {
        assertThatThrownBy(() -> resolutor.identidadDe(peticion(USUARIO, "PRESTADOR", null)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("X-Tipo-Prestador");
    }

    @Test
    @DisplayName("sin el perfil demo ninguna petición tiene identidad, aunque traiga los headers")
    void sinPerfilDemo() {
        assertThatThrownBy(() -> new IdentidadNoDisponible().identidadDe(peticion(USUARIO, "TURISTA", null)))
                .isInstanceOf(NoAutenticadoException.class);
    }

    private static MockHttpServletRequest peticion(String usuarioId, String rol, String tipoPrestador) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/prestador/ofertas");
        if (usuarioId != null) {
            peticion.addHeader(IdentidadPorHeaders.HEADER_USUARIO, usuarioId);
        }
        if (rol != null) {
            peticion.addHeader(IdentidadPorHeaders.HEADER_ROL, rol);
        }
        if (tipoPrestador != null) {
            peticion.addHeader(IdentidadPorHeaders.HEADER_TIPO, tipoPrestador);
        }
        return peticion;
    }
}
