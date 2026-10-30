package co.edu.unimagdalena.samariantour.seguridadjwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecodificadorTokenTest {

    private static final UUID USUARIO = UUID.fromString("70000000-0000-0000-0000-000000000007");

    private final JwtDecoder decodificador = DecodificadorToken.crear(ClavesRsa.publica(TokensDePrueba.CLAVE_PUBLICA),
            TokensDePrueba.EMISOR);

    @Test
    @DisplayName("un token del núcleo da el usuario, el rol, la sesión y, del prestador, su tipo")
    void tokenValido() {
        Jwt turista = decodificar(TokensDePrueba.bearer(USUARIO, "TURISTA"));
        assertThat(ReclamosToken.de(turista))
                .isEqualTo(new ReclamosToken(USUARIO, "TURISTA", null, "00000000-0000-0000-0000-00000000000a"));

        Jwt prestador = decodificar(TokensDePrueba.bearerPrestador(USUARIO, "ALOJAMIENTO"));
        assertThat(ReclamosToken.de(prestador).tipoPrestador()).isEqualTo("ALOJAMIENTO");
        assertThat(ReclamosToken.de(decodificar(TokensDePrueba.bearerPrestador(USUARIO, null))).tipoPrestador())
                .isNull();
    }

    @Test
    @DisplayName("se rechaza el token vencido, el de otra clave y el de otro emisor")
    void firmaVencimientoYEmisor() {
        assertThatThrownBy(() -> decodificar(TokensDePrueba.bearerVencido(USUARIO, "TURISTA")))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decodificar(TokensDePrueba.bearerConOtraClave(USUARIO, "ADMINISTRADOR")))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decodificar(TokensDePrueba.bearerAjustado(USUARIO, "ADMINISTRADOR",
                datos -> datos.issuer("otro-emisor")))).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("se rechaza el token sin vencimiento, sin usuario válido, sin rol o con tipo sin ser prestador")
    void reclamosObligatorios() {
        assertThatThrownBy(() -> decodificar(TokensDePrueba.bearerAjustado(USUARIO, "TURISTA",
                datos -> datos.claims(reclamos -> reclamos.remove("exp"))))).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decodificar(TokensDePrueba.bearerAjustado(USUARIO, "TURISTA",
                datos -> datos.subject("nucleo")))).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decodificar(TokensDePrueba.bearerAjustado(USUARIO, "TURISTA",
                datos -> datos.claims(reclamos -> reclamos.remove(ReclamosToken.ROL)))))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decodificar(TokensDePrueba.bearerAjustado(USUARIO, "TURISTA",
                datos -> datos.claim(ReclamosToken.TIPO_PRESTADOR, "ALOJAMIENTO"))))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("se rechaza el token sin firma (alg none) y el firmado con HS256")
    void algoritmosNoAdmitidos() throws Exception {
        JWTClaimsSet datos = new JWTClaimsSet.Builder()
                .issuer(TokensDePrueba.EMISOR)
                .subject(USUARIO.toString())
                .claim(ReclamosToken.ROL, "ADMINISTRADOR")
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build();
        String sinFirma = new PlainJWT(datos).serialize();
        assertThatThrownBy(() -> decodificador.decode(sinFirma)).isInstanceOf(JwtException.class);

        SignedJWT hmac = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), datos);
        // Con la clave pública como secreto: el ataque clásico de confusión de algoritmos.
        hmac.sign(new MACSigner(TokensDePrueba.CLAVE_PUBLICA.getBytes()));
        assertThatThrownBy(() -> decodificador.decode(hmac.serialize())).isInstanceOf(JwtException.class);
    }

    private Jwt decodificar(String bearer) {
        return decodificador.decode(bearer.substring("Bearer ".length()));
    }
}
