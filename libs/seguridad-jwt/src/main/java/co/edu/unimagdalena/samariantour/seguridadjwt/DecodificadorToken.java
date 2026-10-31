package co.edu.unimagdalena.samariantour.seguridadjwt;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.UUID;

/**
 * Valida el token de acceso localmente, con la clave pública del núcleo (diagrama de paquetes, interacción 5). Un
 * token sirve solo si:
 * <ul>
 *   <li>está firmado con RS256 por esa clave: {@code none}, HS256 u otra clave se rechazan;</li>
 *   <li>lo emitió el núcleo ({@code iss}) y no ha vencido ({@code exp}, obligatorio, con 60 s de tolerancia);</li>
 *   <li>trae un usuario ({@code sub}, un UUID) y un rol, y el tipo solo si el rol es PRESTADOR.</li>
 * </ul>
 */
public final class DecodificadorToken {

    private DecodificadorToken() {
    }

    public static JwtDecoder crear(RSAPublicKey clavePublica, String emisor) {
        return crear(clavePublica, emisor, Clock.systemUTC());
    }

    /** @param reloj con el que se valida el vencimiento; una prueba le pasa uno que no depende de la fecha real */
    public static JwtDecoder crear(RSAPublicKey clavePublica, String emisor, Clock reloj) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withPublicKey(clavePublica)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadoresPorDefecto(emisor, reloj),
                DecodificadorToken::validarReclamos));
        return decodificador;
    }

    /**
     * Los mismos de {@link JwtValidators#createDefaultWithIssuer}: el emisor, la huella del certificado y las fechas
     * ({@code exp} y {@code nbf}, con 60 s de tolerancia), pero las fechas con el reloj que se le pase.
     */
    static OAuth2TokenValidator<Jwt> validadoresPorDefecto(String emisor, Clock reloj) {
        JwtTimestampValidator fechas = new JwtTimestampValidator();
        fechas.setClock(reloj);
        return JwtValidators.createDefaultWithValidators(fechas, new JwtIssuerValidator(emisor));
    }

    private static OAuth2TokenValidatorResult validarReclamos(Jwt token) {
        if (token.getExpiresAt() == null) {
            return invalido("El token no tiene vencimiento");
        }
        if (!esUuid(token.getSubject())) {
            return invalido("El token no identifica al usuario");
        }
        String rol = token.getClaimAsString(ReclamosToken.ROL);
        if (rol == null || rol.isBlank()) {
            return invalido("El token no trae el rol");
        }
        String tipo = token.getClaimAsString(ReclamosToken.TIPO_PRESTADOR);
        if (tipo != null && (tipo.isBlank() || !ReclamosToken.ROL_PRESTADOR.equals(rol))) {
            return invalido("Solo el token de un prestador trae su tipo");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static boolean esUuid(String valor) {
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private static OAuth2TokenValidatorResult invalido(String descripcion) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, descripcion, null));
    }
}
