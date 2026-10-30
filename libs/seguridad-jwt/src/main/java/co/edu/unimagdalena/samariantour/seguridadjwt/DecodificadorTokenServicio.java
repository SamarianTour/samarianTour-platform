package co.edu.unimagdalena.samariantour.seguridadjwt;

import com.nimbusds.jwt.JWTParser;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Valida el token de servicio en las rutas internas (DN-38). Cada servicio que puede llamar tiene su emisor y su clave
 * pública; el token se acepta solo si:
 * <ul>
 *   <li>lo emitió uno de esos servicios y está firmado con RS256 por su clave;</li>
 *   <li>su {@code aud} es este servicio, su {@code sub} es el nombre del que llama y no ha vencido ({@code exp},
 *       obligatorio);</li>
 *   <li>no trae {@code rol}: el token de un usuario, aun el del administrador, nunca entra a una ruta interna.</li>
 * </ul>
 */
public final class DecodificadorTokenServicio implements JwtDecoder {

    /** Un servicio que puede llamar: su nombre ({@code sub}), su emisor ({@code iss}) y su clave pública. */
    public record Llamador(String servicio, String emisor, RSAPublicKey clavePublica) {
    }

    private final Map<String, JwtDecoder> porEmisor;

    /** @param audiencia el nombre de este servicio, que el token debe traer en {@code aud} */
    public DecodificadorTokenServicio(String audiencia, List<Llamador> llamadores) {
        this.porEmisor = llamadores.stream().collect(Collectors.toUnmodifiableMap(Llamador::emisor,
                llamador -> decodificador(audiencia, llamador), (uno, otro) -> {
                    throw new IllegalArgumentException("Dos servicios con el mismo emisor");
                }));
    }

    @Override
    public Jwt decode(String token) {
        // El emisor se lee sin verificar, solo para elegir la clave; la firma la verifica el decodificador del emisor.
        String emisor;
        try {
            emisor = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            throw new BadJwtException("El token no es un JWT", e);
        }
        JwtDecoder decodificador = emisor == null ? null : porEmisor.get(emisor);
        if (decodificador == null) {
            throw new BadJwtException("El token no lo emitió un servicio admitido");
        }
        return decodificador.decode(token);
    }

    private static JwtDecoder decodificador(String audiencia, Llamador llamador) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withPublicKey(llamador.clavePublica())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(llamador.emisor()),
                token -> validarReclamos(token, audiencia, llamador.servicio())));
        return decodificador;
    }

    private static OAuth2TokenValidatorResult validarReclamos(Jwt token, String audiencia, String servicio) {
        if (token.getExpiresAt() == null) {
            return invalido("El token no tiene vencimiento");
        }
        if (token.getAudience() == null || !token.getAudience().contains(audiencia)) {
            return invalido("El token no es para este servicio");
        }
        if (!servicio.equals(token.getSubject())) {
            return invalido("El token no identifica al servicio que lo emitió");
        }
        if (token.hasClaim(ReclamosToken.ROL)) {
            return invalido("El token de un usuario no sirve en una ruta interna");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult invalido(String descripcion) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, descripcion, null));
    }
}
