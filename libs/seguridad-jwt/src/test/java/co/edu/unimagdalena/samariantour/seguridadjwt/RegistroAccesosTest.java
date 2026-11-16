package co.edu.unimagdalena.samariantour.seguridadjwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroAccesosTest {

    private static final UUID TURISTA = UUID.fromString("70000000-0000-0000-0000-000000000007");
    private static final String TOKEN = "eyJ-el-token-que-nunca-debe-quedar-en-el-log";

    private AuditoriaDePrueba auditoria;

    @BeforeEach
    void escuchar() {
        auditoria = AuditoriaDePrueba.escuchar();
    }

    @AfterEach
    void limpiar() {
        auditoria.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("deja una advertencia con el prefijo, el usuario, el rol, el método, la ruta y la IP que agregó Nginx")
    void usuarioAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(AutenticacionToken.de(jwt(TURISTA, "TURISTA")));
        MockHttpServletRequest peticion = peticion("GET", "/api/nucleo/admin/cuentas-internas");
        peticion.addHeader("X-Forwarded-For", "10.9.9.9, 203.0.113.7");
        peticion.addHeader("Authorization", "Bearer " + TOKEN);

        RegistroAccesos.denegado(peticion, "ACCESO_DENEGADO");

        assertThat(auditoria.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel().toString()).isEqualTo("WARN");
            assertThat(evento.getFormattedMessage()).isEqualTo("[AUDITORIA QAS-04] acceso denegado "
                    + "codigo=ACCESO_DENEGADO usuario=" + TURISTA + " rol=TURISTA metodo=GET "
                    + "ruta=/api/nucleo/admin/cuentas-internas ip=203.0.113.7");
        });
    }

    @Test
    @DisplayName("nunca registra el token ni la consulta de la ruta, que puede traer otro token")
    void sinTokenNiConsulta() {
        SecurityContextHolder.getContext().setAuthentication(AutenticacionToken.de(jwt(TURISTA, "TURISTA")));
        MockHttpServletRequest peticion = peticion("GET", "/api/nucleo/turista/solicitudes/123");
        peticion.setQueryString("token=secreto-de-restablecimiento");
        peticion.addHeader("Authorization", "Bearer " + TOKEN);

        RegistroAccesos.denegado(peticion, "SOLICITUD_NO_ENCONTRADA");

        assertThat(auditoria.lineas().getFirst())
                .contains("ruta=/api/nucleo/turista/solicitudes/123 ")
                .doesNotContain(TOKEN, "secreto-de-restablecimiento", "token=");
    }

    @Test
    @DisplayName("sin Nginx usa la IP de la conexión; sin sesión, el usuario es anónimo")
    void anonimoSinNginx() {
        MockHttpServletRequest peticion = peticion("POST", "/api/catalogo/prestador/ofertas");
        peticion.setRemoteAddr("192.0.2.10");

        RegistroAccesos.denegado(peticion, "ACCESO_DENEGADO");

        assertThat(auditoria.lineas().getFirst())
                .contains("usuario=anonimo rol=- metodo=POST", "ip=192.0.2.10");
    }

    @Test
    @DisplayName("un servicio que llama a una ruta interna queda con su nombre y el rol SERVICIO")
    void servicio() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "RS256").subject("recomendacion")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token,
                List.of(new SimpleGrantedAuthority(SeguridadJwt.servicio("recomendacion"))), "recomendacion"));

        RegistroAccesos.denegado(peticion("GET", "/internal/ofertas"), "ACCESO_DENEGADO");

        assertThat(auditoria.lineas().getFirst()).contains("usuario=recomendacion rol=SERVICIO");
    }

    @Test
    @DisplayName("un salto de línea en la ruta no crea una línea falsa, y un X-Forwarded-For que no es una IP no entra")
    void sinInyeccion() {
        MockHttpServletRequest peticion = peticion("GET", "/api/nucleo/x\n[AUDITORIA QAS-04] acceso permitido");
        peticion.addHeader("X-Forwarded-For", "203.0.113.7, <script>");

        RegistroAccesos.denegado(peticion, "ACCESO_DENEGADO");

        String mensaje = auditoria.lineas().getFirst();
        assertThat(mensaje).doesNotContain("\n").contains("ruta=/api/nucleo/x_[AUDITORIA", "ip=desconocida");
    }

    private static MockHttpServletRequest peticion(String metodo, String ruta) {
        return new MockHttpServletRequest(metodo, ruta);
    }

    private static Jwt jwt(UUID usuario, String rol) {
        return Jwt.withTokenValue(TOKEN).header("alg", "RS256").subject(usuario.toString()).claim("rol", rol)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
}
