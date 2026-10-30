package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.token;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenAcceso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.seguridadjwt.DecodificadorToken;
import co.edu.unimagdalena.samariantour.seguridadjwt.ReclamosToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.cuenta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El token de acceso JWT RS256 (RF-04, DN-29), validado como lo hacen los servicios: con la clave pública (DN-37). */
class JwtRs256AdapterTest {

    private static final String CLAVE = claveNueva();
    private static final String EMISOR = "samarian-tour-nucleo";

    @Test
    @DisplayName("firma el usuario, su rol y la sesión, con el emisor, 15 minutos de vida y el kid de la clave")
    void emite() {
        JwtRs256Adapter adaptador = new JwtRs256Adapter(CLAVE, EMISOR);
        Usuario usuario = cuenta(Rol.OPERADOR_DISPONIBILIDAD, "op@correo.co");
        UUID sesion = UUID.randomUUID();
        // El JWT guarda las fechas en segundos.
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        TokenAcceso token = adaptador.emitirAcceso(usuario, null, sesion, ahora);

        Jwt jwt = NimbusJwtDecoder.withPublicKey(adaptador.clavePublica()).build().decode(token.valor());
        assertThat(jwt.getSubject()).isEqualTo(usuario.id().toString());
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("OPERADOR_DISPONIBILIDAD");
        assertThat(jwt.getClaimAsString("sid")).isEqualTo(sesion.toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(EMISOR);
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiraEn()).isEqualTo(ahora.plus(Duration.ofMinutes(15)));
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsKey("kid");
        assertThat(jwt.hasClaim("tipoPrestador")).isFalse();
    }

    @Test
    @DisplayName("el token del prestador lleva su tipo, y los demás servicios lo validan con libs/seguridad-jwt")
    void prestadorValidadoComoEnLosServicios() {
        JwtRs256Adapter adaptador = new JwtRs256Adapter(CLAVE, EMISOR);
        Usuario guia = cuenta(Rol.PRESTADOR, "guia@correo.co");
        UUID sesion = UUID.randomUUID();

        TokenAcceso token = adaptador.emitirAcceso(guia, "GUIA_INDEPENDIENTE", sesion, Instant.now());

        Jwt jwt = DecodificadorToken.crear(adaptador.clavePublica(), EMISOR).decode(token.valor());
        assertThat(ReclamosToken.de(jwt)).isEqualTo(
                new ReclamosToken(guia.id(), "PRESTADOR", "GUIA_INDEPENDIENTE", sesion.toString()));
    }

    @Test
    @DisplayName("un token firmado con otra clave no valida; sin clave, el núcleo no arranca")
    void otraClaveOSinClave() {
        String token = new JwtRs256Adapter(CLAVE, EMISOR)
                .emitirAcceso(cuenta(Rol.TURISTA, "t@correo.co"), null, UUID.randomUUID(), Instant.now()).valor();
        JwtRs256Adapter otro = new JwtRs256Adapter(claveNueva(), EMISOR);

        assertThatThrownBy(() -> NimbusJwtDecoder.withPublicKey(otro.clavePublica()).build().decode(token))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> new JwtRs256Adapter("", EMISOR))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NUCLEO_JWT_CLAVE_PRIVADA");
        assertThatThrownBy(() -> new JwtRs256Adapter(Base64.getEncoder().encodeToString(new byte[64]), EMISOR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("acepta la clave en PEM, con sus encabezados y saltos de línea")
    void pem() {
        String pem = "-----BEGIN PRIVATE KEY-----\n" + CLAVE.replaceAll("(.{64})", "$1\n")
                + "\n-----END PRIVATE KEY-----\n";

        assertThat(new JwtRs256Adapter(pem, EMISOR).clavePublica())
                .isEqualTo(new JwtRs256Adapter(CLAVE, EMISOR).clavePublica());
    }

    private static String claveNueva() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return Base64.getEncoder().encodeToString(generador.generateKeyPair().getPrivate().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
