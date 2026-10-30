package co.edu.unimagdalena.samariantour.seguridadjwt;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;
import java.util.UUID;

/**
 * Lo que el token de acceso dice del usuario. El núcleo lo firma con estos nombres y los tres servicios lo leen con
 * ellos. El rol va como texto porque cada servicio tiene su propio enum: el catálogo solo distingue tres.
 *
 * @param usuarioId     {@code sub}
 * @param rol           {@code rol}: TURISTA, PRESTADOR, ADMINISTRADOR, OPERADOR_DISPONIBILIDAD o GESTOR_DESTINO
 * @param tipoPrestador {@code tipoPrestador}: solo del prestador que ya registró su RNT; null en los demás casos
 * @param sesionId      {@code sid}: la sesión que lo emitió
 */
public record ReclamosToken(UUID usuarioId, String rol, String tipoPrestador, String sesionId) {

    public static final String ROL = "rol";
    public static final String TIPO_PRESTADOR = "tipoPrestador";
    public static final String SESION = "sid";
    public static final String ROL_PRESTADOR = "PRESTADOR";

    public ReclamosToken {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(rol, "rol");
    }

    /** Los reclamos de un token que {@link DecodificadorToken} ya validó. */
    public static ReclamosToken de(Jwt token) {
        return new ReclamosToken(UUID.fromString(token.getSubject()), token.getClaimAsString(ROL),
                token.getClaimAsString(TIPO_PRESTADOR), token.getClaimAsString(SESION));
    }
}
