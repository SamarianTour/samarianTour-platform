package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.token;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenAcceso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TokenPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Firma el token de acceso con la clave privada RSA del núcleo (RF-04, DN-29), con {@link NimbusJwtEncoder} de Spring
 * Security. Los demás servicios lo validarán con la clave pública. El token lleva:
 * <ul>
 *   <li>{@code iss}: el emisor, {@code nucleo.jwt.emisor};</li>
 *   <li>{@code sub}: el identificador del usuario, y {@code rol}: uno de los cinco (DN-04);</li>
 *   <li>{@code sid}: la sesión, para poder cerrarla (RF-06);</li>
 *   <li>{@code iat} y {@code exp}: 15 minutos de vida;</li>
 *   <li>en la cabecera, {@code kid}: la huella de la clave, para cambiarla sin invalidar los tokens vigentes.</li>
 * </ul>
 */
@Component
public class JwtRs256Adapter implements TokenPort {

    private final JwtEncoder firmador;
    private final String emisor;
    private final RSAPublicKey clavePublica;
    private final String idClave;

    /**
     * @param clavePrivada la clave privada RSA en PKCS#8, en Base64 de una línea o en PEM, como la genera
     *                     {@code infra/generar-claves.sh}
     */
    public JwtRs256Adapter(@Value("${nucleo.jwt.clave-privada:}") String clavePrivada,
                           @Value("${nucleo.jwt.emisor}") String emisor) {
        RSAKey clave = claveRsa(clavePrivada);
        try {
            this.clavePublica = clave.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalArgumentException("La clave del token no es una clave RSA válida", e);
        }
        this.idClave = clave.getKeyID();
        this.firmador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
        this.emisor = emisor;
    }

    @Override
    public TokenAcceso emitirAcceso(Usuario usuario, UUID sesionId, Instant ahora) {
        Instant expira = ahora.plus(Sesion.VIGENCIA_ACCESO);
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(usuario.id().toString())
                .claim("rol", usuario.rol().name())
                .claim("sid", sesionId.toString())
                .issuedAt(ahora)
                .expiresAt(expira)
                .build();
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(idClave).build();
        return new TokenAcceso(firmador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue(), expira);
    }

    /** La clave pública que valida los tokens; la usarán los demás servicios (PR de seguridad-jwt). */
    public RSAPublicKey clavePublica() {
        return clavePublica;
    }

    private static RSAKey claveRsa(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Falta la clave privada del token (NUCLEO_JWT_CLAVE_PRIVADA); se genera "
                    + "con infra/generar-claves.sh");
        }
        String base64 = texto.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        try {
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privada = (RSAPrivateCrtKey) fabrica.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
            RSAPublicKey publica = (RSAPublicKey) fabrica.generatePublic(
                    new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
            return new RSAKey.Builder(publica).privateKey(privada).keyIDFromThumbprint().build();
        } catch (GeneralSecurityException | IllegalArgumentException | ClassCastException | JOSEException e) {
            throw new IllegalArgumentException("La clave del token no es una clave privada RSA en PKCS#8", e);
        }
    }
}
