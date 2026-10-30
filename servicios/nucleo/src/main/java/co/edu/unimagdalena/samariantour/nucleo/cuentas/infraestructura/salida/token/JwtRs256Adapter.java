package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.token;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenAcceso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TokenPort;
import co.edu.unimagdalena.samariantour.seguridadjwt.ClavesRsa;
import co.edu.unimagdalena.samariantour.seguridadjwt.ReclamosToken;
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

import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

/**
 * Firma el token de acceso con la clave privada RSA del núcleo (RF-04, DN-29), con {@link NimbusJwtEncoder} de Spring
 * Security. Los tres servicios lo validan con la clave pública ({@code libs/seguridad-jwt}, DN-37). El token lleva,
 * con los nombres de {@link ReclamosToken}:
 * <ul>
 *   <li>{@code iss}: el emisor, {@code nucleo.jwt.emisor};</li>
 *   <li>{@code sub}: el identificador del usuario, y {@code rol}: uno de los cinco (DN-04);</li>
 *   <li>{@code tipoPrestador}: solo el del prestador que ya registró su RNT (DN-37);</li>
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
        RSAPrivateCrtKey privada = ClavesRsa.privada(clavePrivada);
        this.clavePublica = ClavesRsa.publicaDe(privada);
        RSAKey clave;
        try {
            clave = new RSAKey.Builder(clavePublica).privateKey(privada).keyIDFromThumbprint().build();
        } catch (JOSEException e) {
            throw new IllegalArgumentException("La clave del token no es una clave RSA válida", e);
        }
        this.idClave = clave.getKeyID();
        this.firmador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
        this.emisor = emisor;
    }

    @Override
    public TokenAcceso emitirAcceso(Usuario usuario, String tipoPrestador, UUID sesionId, Instant ahora) {
        Instant expira = ahora.plus(Sesion.VIGENCIA_ACCESO);
        JwtClaimsSet.Builder datos = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(usuario.id().toString())
                .claim(ReclamosToken.ROL, usuario.rol().name())
                .claim(ReclamosToken.SESION, sesionId.toString())
                .issuedAt(ahora)
                .expiresAt(expira);
        if (tipoPrestador != null) {
            datos.claim(ReclamosToken.TIPO_PRESTADOR, tipoPrestador);
        }
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(idClave).build();
        return new TokenAcceso(firmador.encode(JwtEncoderParameters.from(cabecera, datos.build())).getTokenValue(),
                expira);
    }

    /** La clave pública que valida los tokens. */
    public RSAPublicKey clavePublica() {
        return clavePublica;
    }
}
