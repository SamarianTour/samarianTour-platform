package co.edu.unimagdalena.samariantour.seguridadjwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * El usuario autenticado con un token válido: sus reclamos y la autoridad {@code ROLE_<rol>}, que las reglas por
 * prefijo de ruta comparan con {@code hasRole}.
 */
public final class AutenticacionToken extends AbstractAuthenticationToken {

    private final transient Jwt token;
    private final ReclamosToken reclamos;

    private AutenticacionToken(Jwt token, ReclamosToken reclamos) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + reclamos.rol())));
        this.token = token;
        this.reclamos = reclamos;
        setAuthenticated(true);
    }

    /** Convierte el token que {@link DecodificadorToken} ya validó. */
    public static AutenticacionToken de(Jwt token) {
        return new AutenticacionToken(token, ReclamosToken.de(token));
    }

    @Override
    public ReclamosToken getPrincipal() {
        return reclamos;
    }

    @Override
    public Jwt getCredentials() {
        return token;
    }

    @Override
    public String getName() {
        return reclamos.usuarioId().toString();
    }
}
