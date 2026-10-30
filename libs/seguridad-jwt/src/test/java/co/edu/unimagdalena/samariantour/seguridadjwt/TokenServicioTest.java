package co.edu.unimagdalena.samariantour.seguridadjwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El token con el que un servicio llama a las rutas internas de otro (DN-38). */
class TokenServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-30T12:00:00Z");

    private final DecodificadorTokenServicio catalogo = new DecodificadorTokenServicio(ServiciosInternos.CATALOGO,
            List.of(new DecodificadorTokenServicio.Llamador(ServiciosInternos.NUCLEO, TokensDePrueba.EMISOR,
                            ClavesRsa.publica(TokensDePrueba.CLAVE_PUBLICA)),
                    new DecodificadorTokenServicio.Llamador(ServiciosInternos.RECOMENDACION,
                            TokensDePrueba.EMISOR_RECOMENDACION,
                            ClavesRsa.publica(TokensDePrueba.CLAVE_PUBLICA_RECOMENDACION))));

    @Test
    @DisplayName("el núcleo firma un token que el catálogo acepta, y lo reutiliza hasta que le falta un minuto")
    void emiteYReutiliza() {
        RelojMovible reloj = new RelojMovible(AHORA);
        EmisorTokenServicio nucleo = new EmisorTokenServicio(ServiciosInternos.NUCLEO, TokensDePrueba.EMISOR,
                ClavesRsa.privada(TokensDePrueba.CLAVE_PRIVADA), reloj);

        String primero = nucleo.token(ServiciosInternos.CATALOGO);
        assertThat(catalogo.decode(primero).getSubject()).isEqualTo(ServiciosInternos.NUCLEO);
        reloj.ahora = AHORA.plus(Duration.ofMinutes(3));
        assertThat(nucleo.token(ServiciosInternos.CATALOGO)).isEqualTo(primero);
        reloj.ahora = AHORA.plus(Duration.ofMinutes(4).plusSeconds(1));
        assertThat(nucleo.token(ServiciosInternos.CATALOGO)).isNotEqualTo(primero);
    }

    @Test
    @DisplayName("acepta el token de cada servicio admitido con su propia clave")
    void serviciosAdmitidos() {
        assertThat(catalogo.decode(token(ServiciosInternos.NUCLEO, ServiciosInternos.CATALOGO)).getSubject())
                .isEqualTo(ServiciosInternos.NUCLEO);
        assertThat(catalogo.decode(token(ServiciosInternos.RECOMENDACION, ServiciosInternos.CATALOGO)).getSubject())
                .isEqualTo(ServiciosInternos.RECOMENDACION);
    }

    @Test
    @DisplayName("rechaza el token para otro servicio, el de otra clave, el de un usuario y el que no es JWT")
    void rechazos() {
        assertThatThrownBy(() -> catalogo.decode(token(ServiciosInternos.NUCLEO, ServiciosInternos.RECOMENDACION)))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> catalogo.decode(TokensDePrueba.bearerServicioConOtraClave(ServiciosInternos.NUCLEO,
                ServiciosInternos.CATALOGO).substring(7))).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> catalogo.decode(TokensDePrueba.bearer(
                "60000000-0000-0000-0000-000000000006", "ADMINISTRADOR").substring(7)))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> catalogo.decode("no-es-un-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("solo las peticiones a /internal/** llevan el token; las rutas públicas salen sin él")
    void interceptor() throws Exception {
        EmisorTokenServicio recomendacion = new EmisorTokenServicio(ServiciosInternos.RECOMENDACION,
                TokensDePrueba.EMISOR_RECOMENDACION, ClavesRsa.privada(TokensDePrueba.CLAVE_PRIVADA_RECOMENDACION),
                Clock.systemUTC());
        ClientHttpRequestInterceptor interceptor = recomendacion.interceptor(ServiciosInternos.CATALOGO);

        MockClientHttpRequest interna = enviar(interceptor,
                "http://catalogo:8080/api/catalogo/internal/ofertas?page=0");
        MockClientHttpRequest publica = enviar(interceptor, "http://catalogo:8080/api/catalogo/zonas");

        String autorizacion = interna.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertThat(autorizacion).startsWith("Bearer ");
        assertThat(catalogo.decode(autorizacion.substring(7)).getSubject()).isEqualTo(ServiciosInternos.RECOMENDACION);
        assertThat(publica.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }

    private static MockClientHttpRequest enviar(ClientHttpRequestInterceptor interceptor, String url)
            throws Exception {
        MockClientHttpRequest peticion = new MockClientHttpRequest(HttpMethod.GET, URI.create(url));
        interceptor.intercept(peticion, new byte[0], (p, cuerpo) -> new MockClientHttpResponse(new byte[0],
                HttpStatus.OK));
        return peticion;
    }

    /** Un reloj que la prueba adelanta. */
    private static final class RelojMovible extends Clock {

        private Instant ahora;

        RelojMovible(Instant ahora) {
            this.ahora = ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private static String token(String servicio, String audiencia) {
        return TokensDePrueba.bearerServicio(servicio, audiencia).substring("Bearer ".length());
    }
}
