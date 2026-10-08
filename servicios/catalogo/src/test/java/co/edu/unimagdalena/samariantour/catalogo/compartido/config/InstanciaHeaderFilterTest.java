package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InstanciaHeaderFilterTest {

    @Test
    void agregaElHostnameDeLaInstanciaALaRespuesta() throws Exception {
        var filtro = new InstanciaHeaderFilter("catalogo-1");
        var respuesta = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/ofertas"), respuesta, new MockFilterChain());

        assertThat(respuesta.getHeader("X-Instancia")).isEqualTo("catalogo-1");
    }
}
